package com.tinah.transactify.di

import android.content.Context
import com.tinah.transactify.TransactifyApplication
import com.tinah.transactify.data.datastore.PreferencesManager
import com.tinah.transactify.data.datastore.transactifyDataStore
import com.tinah.transactify.data.db.AppDatabase
import com.tinah.transactify.data.repository.ClientRepository
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.usecase.CalculateProfitUseCase
import com.tinah.transactify.domain.usecase.ClassifyClientUseCase
import com.tinah.transactify.domain.usecase.ExportReportToExcelUseCase
import com.tinah.transactify.domain.usecase.ExportReportToPdfUseCase
import com.tinah.transactify.domain.usecase.GetClientsUseCase
import com.tinah.transactify.domain.usecase.GetDashboardSummaryUseCase
import com.tinah.transactify.domain.usecase.GetReportDataUseCase
import com.tinah.transactify.domain.usecase.GetTransactionsUseCase
import com.tinah.transactify.domain.usecase.ProcessSmsUseCase
import com.tinah.transactify.domain.usecase.RecalculateProfitsUseCase
import com.tinah.transactify.utils.BonusMatchingService

/**
 * Conteneur d'injection de dépendances (ServiceLocator manuel).
 *
 * Instancié une seule fois par [TransactifyApplication] et récupéré via
 * [Context.appContainer]. Chaque dépendance est un singleton paresseux. Migrable
 * vers Hilt ultérieurement sans changer les sites d'appel.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(appContext.transactifyDataStore)
    }

    val classifyClientUseCase: ClassifyClientUseCase by lazy { ClassifyClientUseCase() }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao(), database.clientDao(), classifyClientUseCase)
    }

    val clientRepository: ClientRepository by lazy {
        ClientRepository(database.clientDao())
    }

    val calculateProfitUseCase: CalculateProfitUseCase by lazy {
        CalculateProfitUseCase(preferencesManager::currentRates)
    }

    val bonusMatchingService: BonusMatchingService by lazy {
        BonusMatchingService(transactionRepository, calculateProfitUseCase)
    }

    val processSmsUseCase: ProcessSmsUseCase by lazy {
        ProcessSmsUseCase(transactionRepository, calculateProfitUseCase, bonusMatchingService)
    }

    val getDashboardSummaryUseCase: GetDashboardSummaryUseCase by lazy {
        GetDashboardSummaryUseCase(transactionRepository)
    }

    val getTransactionsUseCase: GetTransactionsUseCase by lazy {
        GetTransactionsUseCase(transactionRepository)
    }

    val getClientsUseCase: GetClientsUseCase by lazy {
        GetClientsUseCase(clientRepository)
    }

    val getReportDataUseCase: GetReportDataUseCase by lazy {
        GetReportDataUseCase(transactionRepository)
    }

    val exportReportToPdfUseCase: ExportReportToPdfUseCase by lazy { ExportReportToPdfUseCase() }

    val exportReportToExcelUseCase: ExportReportToExcelUseCase by lazy { ExportReportToExcelUseCase() }

    val recalculateProfitsUseCase: RecalculateProfitsUseCase by lazy {
        RecalculateProfitsUseCase(transactionRepository, calculateProfitUseCase)
    }
}

/** Raccourci d'accès au conteneur depuis n'importe quel [Context]. */
val Context.appContainer: AppContainer
    get() = (applicationContext as TransactifyApplication).container
