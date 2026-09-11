package com.tinah.transactify

import android.app.Application
import com.tinah.transactify.di.AppContainer
import timber.log.Timber

class TransactifyApplication : Application() {

    /** Conteneur de dépendances de l'application (créé au premier accès). */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
