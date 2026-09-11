package com.tinah.transactify.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tinah.transactify.data.db.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /**
     * `IGNORE` plutôt que `REPLACE` : en cas de conflit sur l'index unique
     * (operator, timestamp, amount, transaction_type, phone_number), on ne
     * touche pas à la ligne existante (qui peut déjà être liée à un bonus) et on
     * renvoie `-1`.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE phone_number = :phoneNumber ORDER BY timestamp DESC")
    fun getTransactionsByClient(phoneNumber: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE operator = :operator ORDER BY timestamp DESC")
    fun getTransactionsByOperator(operator: String): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE transaction_type = 'REÇU'")
    fun getTotalReceived(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE transaction_type = 'ENVOYÉ'")
    fun getTotalSent(): Flow<Double?>

    @Query("SELECT SUM(profit_calculated) FROM transactions")
    fun getTotalProfit(): Flow<Double?>

    /**
     * Compte les transactions dans `[startInclusive, endExclusive[`. Les bornes
     * doivent être calculées côté Kotlin (voir [com.tinah.transactify.utils.DateUtils])
     * dans le fuseau horaire de l'appareil — `DATE('now')` de SQLite raisonne en
     * UTC et décale la journée pour Madagascar (UTC+3).
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :startInclusive AND timestamp < :endExclusive")
    fun getTransactionCountInRange(startInclusive: Long, endExclusive: Long): Flow<Int>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestTransactions(limit: Int = 50): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    /**
     * Liste filtrée pour l'écran Transactions : [operator] = `null` -> tous les
     * opérateurs ; bornes de date toujours requises (l'appelant passe
     * `0` / `Long.MAX_VALUE` pour « toutes dates »).
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:operator IS NULL OR operator = :operator)
          AND timestamp >= :startInclusive
          AND timestamp < :endExclusive
        ORDER BY timestamp DESC
        """
    )
    fun getFilteredTransactions(
        operator: String?,
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<Transaction>>

    /** Somme des montants d'un client pour un sens de transaction donné. */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE phone_number = :phoneNumber AND transaction_type = :type
        """
    )
    suspend fun sumAmountForClientByType(phoneNumber: String, type: String): Double

    /** Nombre total de transactions d'un client, tous sens confondus. */
    @Query("SELECT COUNT(*) FROM transactions WHERE phone_number = :phoneNumber")
    suspend fun countForClient(phoneNumber: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE operator = :operator
          AND timestamp = :timestamp
          AND amount = :amount
          AND transaction_type = :type
          AND phone_number = :phoneNumber
        """
    )
    suspend fun countMatching(
        operator: String,
        timestamp: Long,
        amount: Double,
        type: String,
        phoneNumber: String,
    ): Int

    @Query("DELETE FROM transactions WHERE timestamp < :beforeTime")
    suspend fun deleteOldTransactions(beforeTime: Long): Int
}
