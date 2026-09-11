package com.tinah.transactify.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.tinah.transactify.data.service.CashPointForegroundService
import com.tinah.transactify.data.service.SmsWorkScheduler
import com.tinah.transactify.databinding.ActivityMainBinding
import com.tinah.transactify.di.appContainer
import com.tinah.transactify.utils.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val smsGranted = PermissionHelper.SMS_PERMISSIONS.all { results[it] == true }
        if (smsGranted || PermissionHelper.hasSmsPermissions(this)) {
            onSmsPermissionsGranted()
        } else {
            Timber.w("Permissions SMS refusées — la capture des transactions est désactivée")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        ensurePermissions()
    }

    private fun ensurePermissions() {
        val required = PermissionHelper.requiredPermissions()
        if (PermissionHelper.hasAll(this, required)) {
            onSmsPermissionsGranted()
        } else {
            permissionLauncher.launch(required)
        }
    }

    /** Permissions SMS acquises : démarre le service (si activé) et le rattrapage périodique. */
    private fun onSmsPermissionsGranted() {
        SmsWorkScheduler.scheduleCatchUp(this)

        lifecycleScope.launch {
            if (appContainer.preferencesManager.foregroundServiceEnabled.first()) {
                startCashPointService()
            }
        }
    }

    private fun startCashPointService() {
        val intent = Intent(this, CashPointForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun setupNavigation() {
        val navController = binding.navHostFragment.findNavController()
        binding.bottomNavigation.setupWithNavController(navController)
    }
}
