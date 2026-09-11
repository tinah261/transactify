package com.tinah.transactify.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.utils.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED || context == null) return
        if (!PermissionHelper.hasSmsPermissions(context)) return

        Timber.d("Boot completed")
        SmsWorkScheduler.scheduleCatchUp(context)

        // Respecte le choix utilisateur (Paramètres) : ne relance pas le service
        // de premier plan s'il a été désactivé. Lecture DataStore -> goAsync().
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                if (appContext.appContainer.preferencesManager.foregroundServiceEnabled.first()) {
                    startForegroundServiceCompat(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun startForegroundServiceCompat(context: Context) {
        val serviceIntent = Intent(context, CashPointForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
