package com.tinah.transactify.data.service

import android.content.Context
import android.provider.Telephony
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.domain.usecase.RawSms
import com.tinah.transactify.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Lit les SMS reçus depuis le dernier passage et les fait traiter par
 * [com.tinah.transactify.domain.usecase.ProcessSmsUseCase].
 *
 * Le curseur ([com.tinah.transactify.data.datastore.PreferencesManager.lastProcessedSmsTimestamp])
 * évite de rebalayer les mêmes SMS à chaque exécution : seule la toute première
 * exécution (curseur à `0`) retombe sur une fenêtre fixe ([Constants.SMS_LOOKBACK_MS])
 * pour ne pas relire tout l'historique SMS de l'appareil.
 */
class SMSProcessingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = applicationContext.appContainer
        try {
            val cursor = container.preferencesManager.lastProcessedSmsTimestamp.first()
            val since = if (cursor > 0) cursor else System.currentTimeMillis() - Constants.SMS_LOOKBACK_MS

            var processed = 0
            var latestTimestamp = cursor

            // >= et non > : un SMS distinct partageant exactement l'horodatage
            // (ms) du curseur ne doit pas être exclu à jamais. Le SMS qui a fixé
            // le curseur est donc relu une fois de plus, mais isDuplicate() /
            // l'index unique l'ignorent proprement (voir ProcessSmsUseCase).
            val smsCursor = applicationContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY, Telephony.Sms.ADDRESS, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC",
            )

            smsCursor?.use {
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val timestamp = it.getLong(dateIdx)
                    val body = it.getString(bodyIdx) ?: continue

                    container.processSmsUseCase(
                        RawSms(body = body, sender = it.getString(addressIdx), timestamp = timestamp),
                    )
                    processed++
                    if (timestamp > latestTimestamp) latestTimestamp = timestamp
                }
            }

            if (latestTimestamp > cursor) {
                container.preferencesManager.setLastProcessedSmsTimestamp(latestTimestamp)
            }

            Timber.d("Traitement SMS terminé (%d message(s) balayé(s) depuis %d)", processed, since)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Échec du traitement des SMS")
            Result.retry()
        }
    }
}
