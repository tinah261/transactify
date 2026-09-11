package com.tinah.transactify.data.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tinah.transactify.utils.Constants
import java.util.concurrent.TimeUnit

/**
 * Point d'entrée unique pour planifier [SMSProcessingWorker] — évite de dupliquer
 * la configuration WorkManager dans le receiver et au démarrage de l'app.
 */
object SmsWorkScheduler {

    /**
     * Traitement immédiat déclenché par [SMSBroadcastReceiver]. `APPEND` (et non
     * `APPEND_OR_REPLACE`) pour ne jamais annuler un traitement déjà en file en
     * cas de rafale de SMS.
     */
    fun scheduleImmediateProcessing(context: Context) {
        val request = OneTimeWorkRequestBuilder<SMSProcessingWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            Constants.SMS_WORK_NAME,
            ExistingWorkPolicy.APPEND,
            request,
        )
    }

    /**
     * Filet de rattrapage périodique (au cas où un SMS aurait été manqué : app
     * tuée, permission accordée entre deux SMS, etc.). Pas de contrainte réseau —
     * le traitement est entièrement local.
     */
    fun scheduleCatchUp(context: Context) {
        val request = PeriodicWorkRequestBuilder<SMSProcessingWorker>(
            Constants.SMS_CATCHUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.SMS_CATCHUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
