package com.tinah.transactify.data.repository

import com.tinah.transactify.data.db.dao.ClientDao
import com.tinah.transactify.data.db.dao.TransactionDao
import com.tinah.transactify.data.db.entity.Client
import com.tinah.transactify.data.db.entity.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val clientDao: ClientDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startTime, endTime)

    fun getTransactionsByClient(phoneNumber: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByClient(phoneNumber)

    fun getTotalReceived(): Flow<Double?> =
        transactionDao.getTotalReceived()

    fun getTotalSent(): Flow<Double?> =
        transactionDao.getTotalSent()

    fun getTotalProfit(): Flow<Double?> =
        transactionDao.getTotalProfit()

    fun getLatestTransactions(limit: Int = 50): Flow<List<Transaction>> =
        transactionDao.getLatestTransactions(limit)

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
        updateClientStats(transaction.phoneNumber)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    private suspend fun updateClientStats(phoneNumber: String) {
        val client = clientDao.getClientByPhone(phoneNumber) ?: run {
            clientDao.insertClient(Client(phoneNumber = phoneNumber))
            null
        }

        val transactionsForClient = transactionDao
            .getTransactionsByClient(phoneNumber)
            .first()

        val totalReceived = transactionsForClient
            .filter { it.transactionType == "REÇU" }
            .sumOf { it.amount + it.bonusAmount }

        val totalSent = transactionsForClient
            .filter { it.transactionType == "ENVOYÉ" }
            .sumOf { it.amount }

        val current = client ?: clientDao.getClientByPhone(phoneNumber) ?: return

        clientDao.updateClient(
            current.copy(
                totalReceived = totalReceived,
                totalSent = totalSent,
                transactionCount = transactionsForClient.size,
                lastInteraction = System.currentTimeMillis()
            )
        )
    }
}
