package com.tinah.transactify.data.service

import android.content.Context
import android.provider.Telephony
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.domain.usecase.RawSms
import com.tinah.transactify.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Lit les SMS récents du fournisseur système et les fait traiter par
 * [com.tinah.transactify.domain.usecase.ProcessSmsUseCase].
 */
class SMSProcessingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val processSms = applicationContext.appContainer.processSmsUseCase
        try {
            val since = System.currentTimeMillis() - Constants.SMS_LOOKBACK_MS
            val cursor = applicationContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY, Telephony.Sms.ADDRESS, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC",
            )

            var processed = 0
            cursor?.use {
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val body = it.getString(bodyIdx) ?: continue
                    processSms(
                        RawSms(
                            body = body,
                            sender = it.getString(addressIdx),
                            timestamp = it.getLong(dateIdx),
                        ),
                    )
                    processed++
                }
            }

            Timber.d("Traitement SMS terminé (%d message(s) balayé(s))", processed)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Échec du traitement des SMS")
            Result.retry()
        }
    }
}
