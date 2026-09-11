package com.tinah.transactify.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2 : ajoute l'index unique anti-doublon sur `transactions`
 * (operator, timestamp, amount, transaction_type, phone_number) — voir
 * [com.tinah.transactify.data.db.entity.Transaction]. Aucune autre colonne ne
 * change : les données existantes sont conservées.
 *
 * Le nom de l'index suit la convention de nommage de Room
 * (`index_<table>_<col1>_<col2>...`, déjà observable sur `index_clients_phone_number`)
 * — il doit correspondre exactement à ce que Room attend, sous peine
 * d'`IllegalStateException` au démarrage (schéma jugé invalide malgré une
 * structure équivalente).
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_transactions_operator_timestamp_amount_transaction_type_phone_number`
            ON `transactions` (`operator`, `timestamp`, `amount`, `transaction_type`, `phone_number`)
            """.trimIndent(),
        )
    }
}

/** Toutes les migrations enregistrées, dans l'ordre. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
