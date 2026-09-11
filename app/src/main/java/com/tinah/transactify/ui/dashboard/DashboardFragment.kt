package com.tinah.transactify.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.tinah.transactify.R
import com.tinah.transactify.databinding.FragmentDashboardBinding
import com.tinah.transactify.databinding.ItemOperatorBreakdownBinding
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.domain.model.OperatorBreakdown
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionSummary
import com.tinah.transactify.ui.transactions.TransactionAdapter
import com.tinah.transactify.ui.transactions.TransactionDetailFragment
import com.tinah.transactify.utils.MoneyFormatter
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(requireContext().appContainer)
    }

    private val latestTransactionsAdapter = TransactionAdapter { item ->
        findNavController().navigate(
            R.id.action_dashboardFragment_to_transactionDetailFragment,
            TransactionDetailFragment.argsFor(item.id),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.latestTransactionsList.adapter = latestTransactionsAdapter
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.summary.collect { bindSummary(it) } }
                launch { viewModel.operatorBreakdown.collect { bindOperatorBreakdown(it) } }
            }
        }
    }

    private fun bindSummary(summary: TransactionSummary) {
        binding.totalReceivedText.text = MoneyFormatter.format(summary.totalReceived)
        binding.totalSentText.text = MoneyFormatter.format(summary.totalSent)
        binding.totalProfitText.text = MoneyFormatter.format(summary.totalProfit)
        binding.todayCountText.text = resources.getQuantityString(
            R.plurals.dashboard_today_count,
            summary.transactionCountToday,
            summary.transactionCountToday,
        )

        latestTransactionsAdapter.submitList(summary.latestTransactions)
        binding.latestTransactionsEmptyText.visibility =
            if (summary.latestTransactions.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun bindOperatorBreakdown(breakdown: List<OperatorBreakdown>) {
        binding.operatorBreakdownCard.visibility = if (breakdown.isEmpty()) View.GONE else View.VISIBLE
        binding.operatorBreakdownContainer.removeAllViews()

        breakdown.forEach { item ->
            val rowBinding = ItemOperatorBreakdownBinding.inflate(
                layoutInflater,
                binding.operatorBreakdownContainer,
                false,
            )
            rowBinding.dot.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), operatorColorRes(item.operator))
            rowBinding.operatorText.text =
                getString(R.string.dashboard_operator_line, item.operator.storageValue, item.transactionCount)
            rowBinding.amountText.text = MoneyFormatter.format(item.totalProfit)
            binding.operatorBreakdownContainer.addView(rowBinding.root)
        }
    }

    private fun operatorColorRes(operator: OperatorType): Int = when (operator) {
        OperatorType.ORANGE_MONEY -> R.color.orange_money
        OperatorType.AIRTEL_MONEY -> R.color.airtel_red
        OperatorType.MVOLA -> R.color.mvola_blue
    }

    private fun setupClickListeners() {
        binding.viewTransactionsBtn.setOnClickListener {
            findNavController().navigate(R.id.transactionsFragment)
        }
        binding.viewClientsBtn.setOnClickListener {
            findNavController().navigate(R.id.clientsFragment)
        }
    }

    override fun onDestroyView() {
        binding.latestTransactionsList.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
