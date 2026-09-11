package com.tinah.transactify.ui.transactions

import android.os.Bundle
import android.text.InputType
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
import com.tinah.transactify.databinding.FragmentTransactionDetailBinding
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.domain.model.TransactionItem
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.utils.DateUtils
import com.tinah.transactify.utils.MoneyFormatter
import kotlinx.coroutines.launch

/** Détail d'une transaction : consultation, correction manuelle du bénéfice, suppression. */
class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!

    private val transactionId: Int by lazy { requireArguments().getInt(ARG_TRANSACTION_ID) }

    private val viewModel: TransactionDetailViewModel by viewModels {
        TransactionDetailViewModel.Factory(transactionId, requireContext().appContainer)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editProfitButton.setOnClickListener { showEditProfitDialog() }
        binding.deleteButton.setOnClickListener { confirmDelete() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transaction.collect { item ->
                    if (item != null) bind(item)
                }
            }
        }
    }

    private fun bind(item: TransactionItem) {
        binding.operatorText.text = item.operator.storageValue
        binding.amountText.text = MoneyFormatter.format(item.amount)
        binding.typeText.text = item.type.displayName
        binding.typeText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (item.type == TransactionType.RECU) R.color.profit_green else R.color.loss_red,
            ),
        )
        binding.phoneText.text = item.phoneNumber
        binding.dateText.text = DateUtils.formatDateTime(item.timestamp)

        binding.referenceRow.visibility = if (item.reference != null) View.VISIBLE else View.GONE
        binding.referenceText.text = item.reference.orEmpty()

        binding.bonusRow.visibility = if (item.bonusLinked) View.VISIBLE else View.GONE
        binding.bonusAmountText.text = MoneyFormatter.format(item.bonusAmount)

        binding.profitText.text = MoneyFormatter.format(item.profit)
    }

    private fun showEditProfitDialog() {
        val current = viewModel.transaction.value ?: return
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(current.profit.toInt().toString())
            setSelection(text.length)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.transaction_detail_edit_profit)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val newProfit = input.text.toString().toDoubleOrNull()
                if (newProfit != null && newProfit >= 0.0) {
                    viewModel.updateProfit(newProfit)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.transaction_detail_delete)
            .setMessage(R.string.transaction_detail_delete_confirm)
            .setPositiveButton(R.string.transaction_detail_delete) { _, _ ->
                viewModel.delete { findNavController().popBackStack() }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TRANSACTION_ID = "transactionId"

        fun argsFor(transactionId: Int): Bundle = bundleOf(ARG_TRANSACTION_ID to transactionId)
    }
}
