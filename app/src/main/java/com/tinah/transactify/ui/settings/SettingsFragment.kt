package com.tinah.transactify.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.tinah.transactify.BuildConfig
import com.tinah.transactify.R
import com.tinah.transactify.data.service.CashPointForegroundService
import com.tinah.transactify.data.service.SmsWorkScheduler
import com.tinah.transactify.databinding.FragmentSettingsBinding
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.utils.Constants
import kotlinx.coroutines.launch

/** Taux de commission, activation du service, maintenance (recalcul, re-scan SMS). */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(requireContext().appContainer)
    }

    /** Évite que la mise à jour programmatique du switch (depuis le StateFlow) ne redéclenche son propre listener. */
    private var isBindingServiceSwitch = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.aboutText.text = getString(R.string.settings_about, BuildConfig.VERSION_NAME)

        binding.saveRatesButton.setOnClickListener { saveRates() }
        binding.serviceSwitch.setOnCheckedChangeListener { _, checked ->
            if (!isBindingServiceSwitch) onServiceToggled(checked)
        }
        binding.recalculateButton.setOnClickListener { confirmRecalculate() }
        binding.rescanButton.setOnClickListener { confirmRescan() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rates.collect { bindRates(it) } }
                launch {
                    viewModel.foregroundServiceEnabled.collect { enabled ->
                        isBindingServiceSwitch = true
                        binding.serviceSwitch.isChecked = enabled
                        isBindingServiceSwitch = false
                    }
                }
            }
        }
    }

    private fun bindRates(rates: CommissionRates) {
        fun set(field: TextInputEditText, operator: OperatorType, type: TransactionType) {
            // Ne pas écraser une saisie en cours si le champ a déjà le focus.
            if (field.hasFocus()) return
            field.setText(formatPercent(rates.rateFor(operator, type)))
        }
        set(binding.rateOrangeRecu, OperatorType.ORANGE_MONEY, TransactionType.RECU)
        set(binding.rateOrangeEnvoye, OperatorType.ORANGE_MONEY, TransactionType.ENVOYE)
        set(binding.rateAirtelRecu, OperatorType.AIRTEL_MONEY, TransactionType.RECU)
        set(binding.rateAirtelEnvoye, OperatorType.AIRTEL_MONEY, TransactionType.ENVOYE)
        set(binding.rateMvolaRecu, OperatorType.MVOLA, TransactionType.RECU)
        set(binding.rateMvolaEnvoye, OperatorType.MVOLA, TransactionType.ENVOYE)
    }

    private fun saveRates() {
        val fields = listOf(
            Triple(binding.rateOrangeRecu, OperatorType.ORANGE_MONEY, TransactionType.RECU),
            Triple(binding.rateOrangeEnvoye, OperatorType.ORANGE_MONEY, TransactionType.ENVOYE),
            Triple(binding.rateAirtelRecu, OperatorType.AIRTEL_MONEY, TransactionType.RECU),
            Triple(binding.rateAirtelEnvoye, OperatorType.AIRTEL_MONEY, TransactionType.ENVOYE),
            Triple(binding.rateMvolaRecu, OperatorType.MVOLA, TransactionType.RECU),
            Triple(binding.rateMvolaEnvoye, OperatorType.MVOLA, TransactionType.ENVOYE),
        )

        var hasInvalid = false
        fields.forEach { (field, operator, type) ->
            val percent = field.text?.toString()?.toDoubleOrNull()
            if (percent == null || percent !in 0.0..100.0) {
                hasInvalid = true
            } else {
                viewModel.setRate(operator, type, percent / 100.0)
            }
        }

        val messageRes = if (hasInvalid) R.string.settings_rates_saved_with_errors else R.string.settings_rates_saved
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun onServiceToggled(enabled: Boolean) {
        viewModel.setForegroundServiceEnabled(enabled)
        val intent = Intent(requireContext(), CashPointForegroundService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
        } else {
            requireContext().stopService(intent)
        }
    }

    private fun confirmRecalculate() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_recalculate_profits)
            .setMessage(R.string.settings_recalculate_confirm)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.recalculateAllProfits { count ->
                    Toast.makeText(
                        requireContext(),
                        resources.getQuantityString(R.plurals.settings_recalculate_result, count, count),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmRescan() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_rescan_sms)
            .setMessage(R.string.settings_rescan_confirm)
            .setPositiveButton(R.string.action_save) { _, _ -> rescanLast24Hours() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun rescanLast24Hours() {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            context.appContainer.preferencesManager.resetSmsCursor(
                System.currentTimeMillis() - Constants.SMS_RESCAN_WINDOW_MS,
            )
            SmsWorkScheduler.scheduleImmediateProcessing(context)
            Toast.makeText(context, R.string.settings_rescan_started, Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatPercent(rate: Double): String {
        val percent = rate * 100.0
        return if (percent == percent.toLong().toDouble()) percent.toLong().toString() else percent.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
