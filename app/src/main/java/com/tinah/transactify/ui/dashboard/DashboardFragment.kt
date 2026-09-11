package com.tinah.transactify.ui.dashboard

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
import com.tinah.transactify.databinding.FragmentDashboardBinding
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.utils.MoneyFormatter
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(requireContext().appContainer)
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
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.summary.collect { summary ->
                    binding.totalReceivedText.text = MoneyFormatter.format(summary.totalReceived)
                    binding.totalSentText.text = MoneyFormatter.format(summary.totalSent)
                    binding.totalProfitText.text = MoneyFormatter.format(summary.totalProfit)
                }
            }
        }
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
        super.onDestroyView()
        _binding = null
    }
}
