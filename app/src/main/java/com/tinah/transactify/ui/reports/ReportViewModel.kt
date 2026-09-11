package com.tinah.transactify.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tinah.transactify.di.AppContainer
import com.tinah.transactify.domain.model.ReportData
import com.tinah.transactify.domain.model.ReportPeriod
import com.tinah.transactify.domain.usecase.GetReportDataUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** ViewModel de l'écran Rapports : synthèse + graphiques pour une période choisie. */
class ReportViewModel(
    getReportData: GetReportDataUseCase,
) : ViewModel() {

    private val period = MutableStateFlow(ReportPeriod.LAST_30_DAYS)

    val selectedPeriod: StateFlow<ReportPeriod> = period
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), period.value)

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportData: StateFlow<ReportData?> = period
        .flatMapLatest<ReportPeriod, ReportData?> { getReportData(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPeriod(newPeriod: ReportPeriod) {
        period.value = newPeriod
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReportViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            return ReportViewModel(container.getReportDataUseCase) as T
        }
    }
}
