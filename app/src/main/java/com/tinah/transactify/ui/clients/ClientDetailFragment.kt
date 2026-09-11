package com.tinah.transactify.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tinah.transactify.R
import com.tinah.transactify.databinding.FragmentClientDetailBinding
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.domain.model.ClientClassification
import com.tinah.transactify.domain.model.ClientItem
import com.tinah.transactify.ui.transactions.TransactionAdapter
import com.tinah.transactify.ui.transactions.TransactionDetailFragment
import com.tinah.transactify.utils.MoneyFormatter
import kotlinx.coroutines.launch

/** Détail d'un client : stats, historique de transactions, renommage. */
class ClientDetailFragment : Fragment() {

    private var _binding: FragmentClientDetailBinding? = null
    private val binding get() = _binding!!

    private val phoneNumber: String by lazy { requireArguments().getString(ARG_PHONE_NUMBER).orEmpty() }

    private val viewModel: ClientDetailViewModel by viewModels {
        ClientDetailViewModel.Factory(phoneNumber, requireContext().appContainer)
    }

    private val transactionAdapter = TransactionAdapter { item ->
        findNavController().navigate(
            R.id.action_clientDetailFragment_to_transactionDetailFragment,
            TransactionDetailFragment.argsFor(item.id),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentClientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transactionsList.adapter = transactionAdapter
        binding.editNameButton.setOnClickListener { showEditNameDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.client.collect { client -> if (client != null) bind(client) }
                }
                launch {
                    viewModel.transactions.collect { transactions ->
                        transactionAdapter.submitList(transactions)
                        binding.emptyTransactionsText.visibility =
                            if (transactions.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun bind(client: ClientItem) {
        binding.nameText.text = client.displayName
        binding.phoneText.text = client.phoneNumber
        binding.totalReceivedText.text = MoneyFormatter.format(client.totalReceived)
        binding.totalSentText.text = MoneyFormatter.format(client.totalSent)
        binding.transactionCountText.text = resources.getQuantityString(
            R.plurals.client_transaction_count,
            client.transactionCount,
            client.transactionCount,
        )

        binding.classificationBadge.text = when (client.classification) {
            ClientClassification.VIP -> getString(R.string.client_classification_vip)
            ClientClassification.REGULAR -> getString(R.string.client_classification_regular)
            ClientClassification.ONE_TIME -> getString(R.string.client_classification_one_time)
        }
        binding.classificationBadge.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                when (client.classification) {
                    ClientClassification.VIP -> R.color.orange_money
                    ClientClassification.REGULAR -> R.color.mvola_blue
                    ClientClassification.ONE_TIME -> R.color.text_secondary
                },
            ),
        )
    }

    private fun showEditNameDialog() {
        val current = viewModel.client.value ?: return
        val input = EditText(requireContext()).apply {
            setText(current.name.orEmpty())
            setSelection(text.length)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.client_detail_edit_name)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.updateName(input.text.toString())
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        binding.transactionsList.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PHONE_NUMBER = "phoneNumber"

        fun argsFor(phoneNumber: String): Bundle = bundleOf(ARG_PHONE_NUMBER to phoneNumber)
    }
}
