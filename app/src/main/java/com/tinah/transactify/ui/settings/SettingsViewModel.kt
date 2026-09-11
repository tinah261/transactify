package com.tinah.transactify.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tinah.transactify.data.datastore.PreferencesManager
import com.tinah.transactify.di.AppContainer
import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.usecase.RecalculateProfitsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel de l'écran Paramètres : taux de commission, service, maintenance. */
class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val recalculateProfits: RecalculateProfitsUseCase,
) : ViewModel() {

    val rates: StateFlow<CommissionRates> = preferencesManager.commissionRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommissionRates.DEFAULT)

    val foregroundServiceEnabled: StateFlow<Boolean> = preferencesManager.foregroundServiceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** [rate] en fraction décimale (0.03 = 3 %) ; ignoré silencieusement si hors [0, 1]. */
    fun setRate(operator: OperatorType, type: TransactionType, rate: Double) {
        if (rate !in 0.0..1.0) return
        viewModelScope.launch { preferencesManager.setRate(operator, type, rate) }
    }

    fun setForegroundServiceEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setForegroundServiceEnabled(enabled) }
    }

    /** [onDone] reçoit le nombre de transactions dont le bénéfice a changé. */
    fun recalculateAllProfits(onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(recalculateProfits()) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                "ViewModel inconnu : ${modelClass.name}"
            }
            return SettingsViewModel(container.preferencesManager, container.recalculateProfitsUseCase) as T
        }
    }
}
