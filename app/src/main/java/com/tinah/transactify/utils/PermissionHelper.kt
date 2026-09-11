package com.tinah.transactify.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Regroupe la logique de permissions runtime de l'application. */
object PermissionHelper {

    /** Permissions SMS indispensables au fonctionnement du cash point. */
    val SMS_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
    )

    /**
     * Permissions à demander au lancement : SMS + notifications (Android 13+).
     */
    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SMS_PERMISSIONS + Manifest.permission.POST_NOTIFICATIONS
        } else {
            SMS_PERMISSIONS
        }

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasAll(context: Context, permissions: Array<String>): Boolean =
        permissions.all { hasPermission(context, it) }

    /** Vrai si les permissions SMS (le minimum vital) sont accordées. */
    fun hasSmsPermissions(context: Context): Boolean = hasAll(context, SMS_PERMISSIONS)
}
