package com.tinah.transactify.utils

/** Constantes partagées de l'application. */
object Constants {

    /** Nom du fichier de base de données Room. */
    const val DATABASE_NAME = "transactify.db"

    /** Nom du fichier DataStore Preferences. */
    const val PREFERENCES_NAME = "transactify_prefs"

    // --- WorkManager ---------------------------------------------------------

    /** Nom unique du travail de traitement des SMS entrants. */
    const val SMS_WORK_NAME = "sms_processing"

    /** Nom unique du travail périodique de rattrapage. */
    const val SMS_CATCHUP_WORK_NAME = "sms_catchup"

    /**
     * Fenêtre de lecture rétroactive des SMS (ms). Utilisée comme filet de
     * sécurité quand aucun curseur `lastProcessedSmsTimestamp` n'est encore
     * enregistré.
     */
    const val SMS_LOOKBACK_MS = 120_000L

    /** Fenêtre de rapprochement d'un SMS bonus avec sa transaction mère (ms). */
    const val BONUS_MATCH_WINDOW_MS = 120_000L

    // --- Notifications / service ------------------------------------------------

    const val SERVICE_NOTIFICATION_ID = 42
    const val SERVICE_CHANNEL_ID = "cashpoint_service"

    // --- Classification client -----------------------------------------------

    /** Seuil de transactions pour passer VIP. */
    const val VIP_MIN_TRANSACTIONS = 20

    /** Seuil de volume cumulé (Ar) pour passer VIP. */
    const val VIP_MIN_VOLUME = 1_000_000.0

    /** Au-delà de ce nombre de transactions, un client n'est plus « ponctuel ». */
    const val ONE_TIME_MAX_TRANSACTIONS = 1

    /** Taux de commission par défaut (fraction décimale). */
    object DefaultRates {
        const val ORANGE_RECU = 0.02
        const val ORANGE_ENVOYE = 0.03
        const val AIRTEL_RECU = 0.025
        const val AIRTEL_ENVOYE = 0.035
        const val MVOLA_RECU = 0.022
        const val MVOLA_ENVOYE = 0.032
    }
}
