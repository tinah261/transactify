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

    /** Insère une transaction, met à jour les stats du client et renvoie l'id généré. */
    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(transaction)
        updateClientStats(transaction.phoneNumber)
        return id
    }

    /**
     * Vrai si une transaction identique (même opérateur, horodatage, montant et
     * sens) existe déjà — protection contre le retraitement d'un même SMS.
     */
    suspend fun isDuplicate(
        operator: String,
        timestamp: Long,
        amount: Double,
        type: String,
    ): Boolean = transactionDao.countMatching(operator, timestamp, amount, type) > 0

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
