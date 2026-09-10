package com.tinah.transactify.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class SMSBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION && context != null) {

            val smsRequest = OneTimeWorkRequestBuilder<SMSProcessingWorker>()
                .setInputData(workDataOf("sms_received" to true))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sms_processing",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                smsRequest
            )
        }
    }
}
