package com.tinah.transactify.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.tinah.transactify.R
import com.tinah.transactify.utils.Constants
import timber.log.Timber

/**
 * Service de premier plan qui garde l'app active pour capter les SMS en continu.
 * Type `dataSync` : le traitement lui-même (lecture + parsing des SMS) est fait
 * par [SMSProcessingWorker] via WorkManager, indépendamment de ce service — ce
 * dernier n'est qu'un indicateur persistant pour l'utilisateur et le système.
 */
class CashPointForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                Constants.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(Constants.SERVICE_NOTIFICATION_ID, notification)
        }

        Timber.d("ForegroundService started")
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, Constants.SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.SERVICE_CHANNEL_ID,
                "Transactify Service",
                NotificationManager.IMPORTANCE_HIGH,
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // START_STICKY laisse le système relancer le service quand les ressources
        // le permettent ; BootReceiver le relance au redémarrage. On ne tente plus
        // de le relancer nous-mêmes ici : sur Android 12+, démarrer un foreground
        // service depuis onDestroy() (contexte "background") lève une
        // ForegroundServiceStartNotAllowedException et provoque une boucle de crash.
        Timber.d("ForegroundService destroyed")
    }
}
