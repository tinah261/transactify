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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Query("SELECT COUNT(*) FROM transactions WHERE DATE(timestamp / 1000, 'unixepoch') = DATE('now')")
    fun getTransactionCountToday(): Flow<Int>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestTransactions(limit: Int = 50): Flow<List<Transaction>>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE operator = :operator
          AND timestamp = :timestamp
          AND amount = :amount
          AND transaction_type = :type
        """
    )
    suspend fun countMatching(
        operator: String,
        timestamp: Long,
        amount: Double,
        type: String,
    ): Int

    @Query("DELETE FROM transactions WHERE timestamp < :beforeTime")
    suspend fun deleteOldTransactions(beforeTime: Long): Int
}
