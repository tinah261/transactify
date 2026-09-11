package com.tinah.transactify.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tinah.transactify.di.AppContainer
import com.tinah.transactify.domain.model.OperatorBreakdown
import com.tinah.transactify.domain.model.ReportPeriod
import com.tinah.transactify.domain.model.TransactionSummary
import com.tinah.transactify.domain.usecase.GetDashboardSummaryUseCase
import com.tinah.transactify.domain.usecase.GetReportDataUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** ViewModel du tableau de bord : agrégats globaux, dernières transactions, répartition par opérateur. */
class DashboardViewModel(
    getDashboardSummary: GetDashboardSummaryUseCase,
    getReportData: GetReportDataUseCase,
) : ViewModel() {

    val summary: StateFlow<TransactionSummary> = getDashboardSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionSummary(),
        )

    /**
     * Répartition par opérateur sur l'ensemble de l'historique — réutilise
     * [GetReportDataUseCase] (écran Rapports) plutôt que de dupliquer
     * l'agrégation ; un cash point reste sur un volume de transactions modeste,
     * l'agrégation en mémoire sur "tout l'historique" n'est pas un problème ici.
     */
    val operatorBreakdown: StateFlow<List<OperatorBreakdown>> = getReportData(ReportPeriod.ALL_TIME)
        .map { it.byOperator }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Fabrique liée au [AppContainer] de l'application. */
    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            return DashboardViewModel(container.getDashboardSummaryUseCase, container.getReportDataUseCase) as T
        }
    }
}
