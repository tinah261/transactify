package com.tinah.transactify.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tinah.transactify.di.AppContainer
import com.tinah.transactify.domain.model.TransactionSummary
import com.tinah.transactify.domain.usecase.GetDashboardSummaryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** ViewModel du tableau de bord : expose les agrégats sous forme de [StateFlow]. */
class DashboardViewModel(
    getDashboardSummary: GetDashboardSummaryUseCase,
) : ViewModel() {

    val summary: StateFlow<TransactionSummary> = getDashboardSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionSummary(),
        )

    /** Fabrique liée au [AppContainer] de l'application. */
    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            return DashboardViewModel(container.getDashboardSummaryUseCase) as T
        }
    }
}
