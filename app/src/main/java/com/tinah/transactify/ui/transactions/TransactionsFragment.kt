package com.tinah.transactify.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.tinah.transactify.R
import com.tinah.transactify.databinding.FragmentTransactionsBinding
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.domain.model.OperatorType
import kotlinx.coroutines.launch

/** Liste des transactions, filtrable par opérateur via les chips en tête d'écran. */
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModel.Factory(requireContext().appContainer)
    }

    private val adapter = TransactionAdapter { item ->
        findNavController().navigate(
            R.id.action_transactionsFragment_to_transactionDetailFragment,
            TransactionDetailFragment.argsFor(item.id),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transactionsList.adapter = adapter
        binding.transactionsList.itemAnimator = null

        setupFilterChips()
        observeTransactions()
    }

    private fun setupFilterChips() {
        binding.operatorFilterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val operator = when (checkedIds.firstOrNull()) {
                R.id.chip_orange -> OperatorType.ORANGE_MONEY
                R.id.chip_airtel -> OperatorType.AIRTEL_MONEY
                R.id.chip_mvola -> OperatorType.MVOLA
                else -> null
            }
            viewModel.setOperatorFilter(operator)
        }
    }

    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transactions.collect { transactions ->
                    adapter.submitList(transactions)
                    binding.emptyStateText.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.transactionsList.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
