package com.tinah.transactify.data.service

import android.content.Context
import android.provider.Telephony
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tinah.transactify.data.db.AppDatabase
import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.utils.BonusMatchingService
import com.tinah.transactify.utils.SMSParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SMSProcessingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(context)
    private val repository = TransactionRepository(
        db.transactionDao(),
        db.clientDao()
    )
    private val bonusMatchingService = BonusMatchingService(db.transactionDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cursor = applicationContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.BODY,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.DATE
                ),
                "${Telephony.Sms.DATE} > ?",
                arrayOf((System.currentTimeMillis() - 120_000).toString()), // Dernières 2 min
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val body = it.getString(bodyIdx)
                    val date = it.getLong(dateIdx)

                    val parsed = SMSParser.parseSMS(body) ?: continue

                    val transaction = Transaction(
                        operator = parsed.operator,
                        amount = parsed.amount,
                        phoneNumber = parsed.phoneNumber,
                        transactionType = parsed.type,
                        reference = parsed.reference,
                        timestamp = date,
                        bonusAmount = if (parsed.isBonus) parsed.amount else 0.0,
                        bonusLinked = false
                    )

                    repository.insertTransaction(transaction)

                    if (parsed.isBonus) {
                        bonusMatchingService.matchBonusToTransaction(transaction)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SMS processing failed")
            Result.retry()
        }
    }
}
