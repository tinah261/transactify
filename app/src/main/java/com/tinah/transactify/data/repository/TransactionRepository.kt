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

    fun getTransactionById(id: Int): Flow<Transaction?> =
        transactionDao.getTransactionById(id)

    /** [operator] = `null` pour ne filtrer sur aucun opérateur. */
    fun getFilteredTransactions(
        operator: String?,
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<Transaction>> =
        transactionDao.getFilteredTransactions(operator, startInclusive, endExclusive)

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
        refreshClientStats(transaction.phoneNumber)
        return id
    }

    /**
     * Vrai si une transaction identique (même opérateur, horodatage, montant,
     * sens et numéro de client) existe déjà — protection contre le retraitement
     * d'un même SMS.
     */
    suspend fun isDuplicate(
        operator: String,
        timestamp: Long,
        amount: Double,
        type: String,
        phoneNumber: String,
    ): Boolean = transactionDao.countMatching(operator, timestamp, amount, type, phoneNumber) > 0

    /**
     * Met à jour une transaction existante. **N'appelle pas [refreshClientStats]
     * automatiquement** : si la modification touche un montant/sens qui entre
     * dans les agrégats, l'appelant doit rafraîchir explicitement (voir
     * [com.tinah.transactify.utils.BonusMatchingService], qui recalcule après
     * fusion d'un bonus).
     */
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    /**
     * Recalcule les agrégats du client en base (SQL, pas en mémoire) : le
     * bénéfice/bonus n'entre pas dans `totalReceived` — c'est une commission du
     * cash point, pas un montant reçu du client. Public : [insertTransaction]
     * l'appelle automatiquement, mais [com.tinah.transactify.utils.BonusMatchingService]
     * doit aussi l'appeler après avoir fusionné un bonus dans sa transaction
     * mère (sinon les stats du client restent périmées).
     */
    suspend fun refreshClientStats(phoneNumber: String) {
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
