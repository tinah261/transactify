package com.tinah.transactify.domain.model

import java.time.LocalDate

/** Période prédéfinie pour l'écran Rapports (pas de sélecteur de date libre). */
enum class ReportPeriod {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    ALL_TIME;

    /** `null` = toutes les dates (pas de borne). */
    fun toDateRange(today: LocalDate = LocalDate.now()): ClosedRange<LocalDate>? = when (this) {
        TODAY -> today..today
        LAST_7_DAYS -> today.minusDays(6)..today
        LAST_30_DAYS -> today.minusDays(29)..today
        ALL_TIME -> null
    }

    /** Libellé français, y compris pour les exports (PDF/Excel n'ont pas accès aux ressources Android). */
    val displayLabel: String get() = when (this) {
        TODAY -> "Aujourd'hui"
        LAST_7_DAYS -> "7 derniers jours"
        LAST_30_DAYS -> "30 derniers jours"
        ALL_TIME -> "Depuis le début"
    }
}
