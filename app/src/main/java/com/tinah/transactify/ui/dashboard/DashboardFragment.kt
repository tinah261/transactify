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
import com.tinah.transactify.data.db.AppDatabase
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        DashboardViewModel.Factory(TransactionRepository(db.transactionDao(), db.clientDao()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
                launch {
                    viewModel.totalReceived.collect { amount ->
                        binding.totalReceivedText.text = formatMoney(amount ?: 0.0)
                    }
                }
                launch {
                    viewModel.totalSent.collect { amount ->
                        binding.totalSentText.text = formatMoney(amount ?: 0.0)
                    }
                }
                launch {
                    viewModel.totalProfit.collect { amount ->
                        binding.totalProfitText.text = formatMoney(amount ?: 0.0)
                    }
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

    private fun formatMoney(amount: Double): String {
        val format = NumberFormat.getInstance(Locale.FRENCH)
        return "${format.format(amount)} Ar"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
