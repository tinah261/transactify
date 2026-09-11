package com.tinah.transactify.data.repository

import com.tinah.transactify.data.db.dao.ClientDao
import com.tinah.transactify.data.db.dao.TransactionDao
import com.tinah.transactify.data.db.entity.Client
import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.usecase.ClassifyClientUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val clientDao: ClientDao,
    private val classifyClient: ClassifyClientUseCase,
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

    /** Nombre de transactions dans `[startInclusive, endExclusive[` (bornes déjà en fuseau local). */
    fun getTransactionCountInRange(startInclusive: Long, endExclusive: Long): Flow<Int> =
        transactionDao.getTransactionCountInRange(startInclusive, endExclusive)

    /**
     * Insère une transaction, met à jour les stats du client et renvoie l'id
     * généré, ou `-1` si la transaction existait déjà (index unique) — dans ce
     * cas les stats du client ne sont pas retouchées.
     */
    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(transaction)
        if (id == -1L) {
            Timber.d("Insertion ignorée (doublon déjà en base) : %s", transaction)
            return -1L
        }
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

    /**
     * Recalcule les agrégats du client en base (SQL, pas en mémoire) : le
     * bénéfice/bonus n'entre pas dans `totalReceived` — c'est une commission du
     * cash point, pas un montant reçu du client.
     */
    private suspend fun updateClientStats(phoneNumber: String) {
        val totalReceived = transactionDao.sumAmountForClientByType(phoneNumber, TransactionType.RECU.storageValue)
        val totalSent = transactionDao.sumAmountForClientByType(phoneNumber, TransactionType.ENVOYE.storageValue)
        val transactionCount = transactionDao.countForClient(phoneNumber)
        val classification = classifyClient(transactionCount, totalReceived + totalSent)
        val now = System.currentTimeMillis()

        when (val existing = clientDao.getClientByPhone(phoneNumber)) {
            null -> clientDao.insertClient(
                Client(
                    phoneNumber = phoneNumber,
                    totalReceived = totalReceived,
                    totalSent = totalSent,
                    transactionCount = transactionCount,
                    lastInteraction = now,
                    classification = classification.name,
                ),
            )

            else -> clientDao.updateClient(
                existing.copy(
                    totalReceived = totalReceived,
                    totalSent = totalSent,
                    transactionCount = transactionCount,
                    lastInteraction = now,
                    classification = classification.name,
                    updatedAt = now,
                ),
            )
        }
    }
}
