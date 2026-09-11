package com.tinah.transactify.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tinah.transactify.data.db.dao.ClientDao
import com.tinah.transactify.data.db.dao.TransactionDao
import com.tinah.transactify.data.db.entity.Client
import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.db.migration.ALL_MIGRATIONS
import com.tinah.transactify.utils.Constants

@Database(
    entities = [Transaction::class, Client::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun clientDao(): ClientDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // Filet de sécurité pour tout saut de version non couvert par une
                    // migration explicite (ne devrait pas arriver en pratique).
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
