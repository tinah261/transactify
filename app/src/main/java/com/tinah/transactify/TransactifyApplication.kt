package com.tinah.transactify

import android.app.Application
import timber.log.Timber

class TransactifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
