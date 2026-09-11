package com.tinah.transactify.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore de l'application, exposé via [PreferencesManager]. */
val Context.transactifyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFERENCES_NAME,
)

/**
 * Accès typé aux préférences persistées : taux de commission configurables,
 * curseur de traitement des SMS, activation du service de premier plan.
 */
class PreferencesManager(
    private val dataStore: DataStore<Preferences>,
) {

    /** Taux de commission courants, complétés par [CommissionRates.DEFAULT]. */
    val commissionRates: Flow<CommissionRates> = dataStore.data.map { prefs ->
        val overrides = buildMap<CommissionRates.Key, Double> {
            OperatorType.entries.forEach { operator ->
                TransactionType.entries.forEach { type ->
                    prefs[rateKey(operator, type)]?.let { rate ->
                        put(CommissionRates.Key(operator, type), rate)
                    }
                }
            }
        }
        CommissionRates.of(overrides)
    }

    /** Lecture ponctuelle des taux (pour les use cases). */
    suspend fun currentRates(): CommissionRates = commissionRates.first()

    suspend fun setRate(operator: OperatorType, type: TransactionType, rate: Double) {
        require(rate in 0.0..1.0) { "Un taux doit être compris entre 0.0 et 1.0 (reçu : $rate)" }
        dataStore.edit { it[rateKey(operator, type)] = rate }
    }

    suspend fun resetRates() {
        dataStore.edit { prefs ->
            OperatorType.entries.forEach { operator ->
                TransactionType.entries.forEach { type ->
                    prefs.remove(rateKey(operator, type))
                }
            }
        }
    }

    /** Horodatage (ms) du SMS le plus récent déjà traité. `0` si aucun. */
    val lastProcessedSmsTimestamp: Flow<Long> = dataStore.data.map { it[LAST_SMS_TS] ?: 0L }

    suspend fun setLastProcessedSmsTimestamp(timestampMillis: Long) {
        dataStore.edit { prefs ->
            val current = prefs[LAST_SMS_TS] ?: 0L
            if (timestampMillis > current) prefs[LAST_SMS_TS] = timestampMillis
        }
    }

    /**
     * Recule volontairement le curseur (contrairement à [setLastProcessedSmsTimestamp],
     * qui ne peut qu'avancer) — utilisé par l'action « re-scanner l'historique
     * SMS » des Paramètres, pour forcer un nouveau passage sur une fenêtre
     * passée.
     */
    suspend fun resetSmsCursor(toTimestampMillis: Long) {
        dataStore.edit { it[LAST_SMS_TS] = toTimestampMillis }
    }

    /** Le service de premier plan « à l'écoute » est-il activé (défaut : oui). */
    val foregroundServiceEnabled: Flow<Boolean> =
        dataStore.data.map { it[FOREGROUND_SERVICE_ENABLED] ?: true }

    suspend fun setForegroundServiceEnabled(enabled: Boolean) {
        dataStore.edit { it[FOREGROUND_SERVICE_ENABLED] = enabled }
    }

    private fun rateKey(operator: OperatorType, type: TransactionType): Preferences.Key<Double> =
        doublePreferencesKey("rate_${operator.name}_${type.name}")

    private companion object {
        val LAST_SMS_TS = longPreferencesKey("last_processed_sms_timestamp")
        val FOREGROUND_SERVICE_ENABLED = booleanPreferencesKey("foreground_service_enabled")
    }
}
