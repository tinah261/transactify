package com.tinah.transactify.domain.model

import com.tinah.transactify.data.db.entity.Transaction

/**
 * Modèle UI d'une transaction : mêmes données que l'entité Room, mais avec
 * l'opérateur et le sens déjà typés (enum) plutôt qu'en chaînes brutes.
 */
data class TransactionItem(
    val id: Int,
    val operator: OperatorType,
    val type: TransactionType,
    val amount: Double,
    val phoneNumber: String,
    val reference: String?,
    val timestamp: Long,
    val bonusAmount: Double,
    val bonusLinked: Boolean,
    val profit: Double,
)

/**
 * Convertit l'entité Room en modèle UI, ou `null` si l'opérateur/le sens
 * stockés en base ne sont pas reconnus. Ne devrait jamais arriver pour des
 * données créées par l'app — filet de sécurité contre une donnée corrompue.
 */
fun Transaction.toItemOrNull(): TransactionItem? {
    val operatorType = OperatorType.fromStorage(operator) ?: return null
    val type = TransactionType.fromStorage(transactionType) ?: return null
    return TransactionItem(
        id = id,
        operator = operatorType,
        type = type,
        amount = amount,
        phoneNumber = phoneNumber,
        reference = reference,
        timestamp = timestamp,
        bonusAmount = bonusAmount,
        bonusLinked = bonusLinked,
        profit = profitCalculated,
    )
}
