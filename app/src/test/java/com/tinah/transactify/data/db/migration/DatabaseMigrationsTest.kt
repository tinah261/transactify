package com.tinah.transactify.data.db.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Exécute [MIGRATION_1_2] contre une vraie base SQLite (schéma v1 recréé à la
 * main à partir de `schemas/.../1.json`) et vérifie qu'elle conserve les
 * données et crée l'index unique exactement attendu. On ne passe pas par
 * `MigrationTestHelper` (qui exige les schémas exportés dans les *assets*
 * Android, indisponibles pour un test JVM/Robolectric sans configuration
 * supplémentaire) : on pilote directement la migration sur une
 * [SupportSQLiteDatabase], ce qui teste exactement le même code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationsTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val v1CreateTable = """
        CREATE TABLE IF NOT EXISTS `transactions` (
          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
          `operator` TEXT NOT NULL,
          `amount` REAL NOT NULL,
          `transaction_type` TEXT NOT NULL,
          `phone_number` TEXT NOT NULL,
          `reference` TEXT,
          `timestamp` INTEGER NOT NULL,
          `bonus_amount` REAL NOT NULL,
          `bonus_linked` INTEGER NOT NULL,
          `profit_calculated` REAL NOT NULL,
          `created_at` INTEGER NOT NULL,
          `updated_at` INTEGER NOT NULL
        )
    """.trimIndent()

    private fun openV1Database(dbFile: File): SupportSQLiteDatabase {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.path)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(v1CreateTable)
                        db.execSQL(
                            """
                            INSERT INTO transactions
                            (operator, amount, transaction_type, phone_number, reference,
                             timestamp, bonus_amount, bonus_linked, profit_calculated, created_at, updated_at)
                            VALUES
                            ('Orange Money', 10000.0, 'REÇU', '+26132000000', 'R1', 1000, 0.0, 0, 0.0, 0, 0)
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            )
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
    }

    @Test
    fun `MIGRATION_1_2 conserve les donnees et cree l'index unique attendu`() {
        val dbFile = File(context.cacheDir, "migration-test-${System.nanoTime()}.db")
        dbFile.delete()
        val db = openV1Database(dbFile)

        MIGRATION_1_2.migrate(db)

        db.query("SELECT COUNT(*) FROM transactions").use { cursor ->
            cursor.moveToFirst()
            assertEquals("la ligne existante avant migration doit être conservée", 1, cursor.getInt(0))
        }

        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'transactions'").use { cursor ->
            val indexNames = generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
            assertTrue(
                "index unique attendu introuvable parmi $indexNames",
                indexNames.contains(
                    "index_transactions_operator_timestamp_amount_transaction_type_phone_number",
                ),
            )
        }

        // La migration est idempotente (IF NOT EXISTS) : la rejouer ne doit pas planter.
        MIGRATION_1_2.migrate(db)

        db.close()
    }
}
