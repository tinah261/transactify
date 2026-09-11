package com.tinah.transactify.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.tinah.transactify.R
import com.tinah.transactify.databinding.FragmentReportsBinding
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.domain.model.OperatorBreakdown
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.ReportData
import com.tinah.transactify.domain.model.ReportPeriod
import com.tinah.transactify.utils.MoneyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/** Synthèse + graphiques + export PDF/Excel des transactions sur une période. */
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportViewModel by viewModels {
        ReportViewModel.Factory(requireContext().appContainer)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        setupPeriodChips()
        setupExportButtons()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportData.collect { data -> if (data != null) bind(data) }
            }
        }
    }

    private fun setupCharts() {
        binding.profitBarChart.description.isEnabled = false
        binding.profitBarChart.legend.isEnabled = false

        binding.volumePieChart.description.isEnabled = false
        binding.volumePieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        binding.volumePieChart.setUsePercentValues(true)
        binding.volumePieChart.setEntryLabelTextSize(11f)
    }

    private fun setupPeriodChips() {
        binding.periodGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val period = when (checkedIds.firstOrNull()) {
                R.id.chip_today -> ReportPeriod.TODAY
                R.id.chip_7_days -> ReportPeriod.LAST_7_DAYS
                R.id.chip_all_time -> ReportPeriod.ALL_TIME
                else -> ReportPeriod.LAST_30_DAYS
            }
            viewModel.setPeriod(period)
        }
    }

    private fun setupExportButtons() {
        binding.exportPdfButton.setOnClickListener { export(isPdf = true) }
        binding.exportExcelButton.setOnClickListener { export(isPdf = false) }
    }

    private fun bind(data: ReportData) {
        binding.totalReceivedText.text = MoneyFormatter.format(data.totalReceived)
        binding.totalSentText.text = MoneyFormatter.format(data.totalSent)
        binding.totalProfitText.text = MoneyFormatter.format(data.totalProfit)

        val hasData = data.byOperator.isNotEmpty()
        binding.chartsContainer.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.noDataText.visibility = if (hasData) View.GONE else View.VISIBLE
        binding.exportPdfButton.isEnabled = data.transactions.isNotEmpty()
        binding.exportExcelButton.isEnabled = data.transactions.isNotEmpty()

        if (hasData) {
            bindBarChart(data.byOperator)
            bindPieChart(data.byOperator)
        }
    }

    private fun bindBarChart(byOperator: List<OperatorBreakdown>) {
        val entries = byOperator.mapIndexed { index, breakdown -> BarEntry(index.toFloat(), breakdown.totalProfit.toFloat()) }
        val dataSet = BarDataSet(entries, getString(R.string.report_chart_profit_by_operator)).apply {
            colors = byOperator.map { ContextCompat.getColor(requireContext(), operatorColorRes(it.operator)) }
        }
        binding.profitBarChart.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(byOperator.map { it.operator.storageValue })
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun bindPieChart(byOperator: List<OperatorBreakdown>) {
        val entries = byOperator.map { PieEntry(it.totalVolume.toFloat(), it.operator.storageValue) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = byOperator.map { ContextCompat.getColor(requireContext(), operatorColorRes(it.operator)) }
            valueTextSize = 12f
        }
        binding.volumePieChart.apply {
            data = PieData(dataSet)
            invalidate()
        }
    }

    private fun operatorColorRes(operator: OperatorType): Int = when (operator) {
        OperatorType.ORANGE_MONEY -> R.color.orange_money
        OperatorType.AIRTEL_MONEY -> R.color.airtel_red
        OperatorType.MVOLA -> R.color.mvola_blue
    }

    private fun export(isPdf: Boolean) {
        val data = viewModel.reportData.value ?: return
        val context = requireContext().applicationContext
        val container = requireContext().appContainer

        viewLifecycleOwner.lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
                    val extension = if (isPdf) "pdf" else "xlsx"
                    val file = File(reportsDir, "transactify_${System.currentTimeMillis()}.$extension")
                    file.outputStream().use { out ->
                        if (isPdf) container.exportReportToPdfUseCase(data, out) else container.exportReportToExcelUseCase(data, out)
                    }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    Timber.e(e, "Échec de l'export %s", if (isPdf) "PDF" else "Excel")
                    null
                }
            }

            if (uri == null) {
                Toast.makeText(requireContext(), R.string.report_export_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val mimeType = if (isPdf) "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.report_share_title)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
