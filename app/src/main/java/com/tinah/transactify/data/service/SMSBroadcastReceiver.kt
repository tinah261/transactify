package com.tinah.transactify.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Déclenche [SMSProcessingWorker] à chaque SMS reçu par le système. L'analyse du
 * contenu (est-ce un SMS d'opérateur ?) se fait dans le worker, pas ici : le
 * receiver reste minimal, WorkManager gère la fiabilité (retry, contrainte).
 */
class SMSBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION && context != null) {
            SmsWorkScheduler.scheduleImmediateProcessing(context)
        }
    }
}
