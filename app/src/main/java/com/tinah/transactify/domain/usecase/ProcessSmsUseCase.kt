package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.utils.BonusMatchingService
import com.tinah.transactify.utils.SMSParser
import timber.log.Timber

/** SMS brut à analyser. */
data class RawSms(
    val body: String,
    val sender: String?,
    val timestamp: Long,
)

/** Issue du traitement d'un SMS. */
sealed interface SmsProcessingOutcome {
    /** SMS non reconnu comme une notification de transaction. */
    data object Ignored : SmsProcessingOutcome

    /** Transaction déjà enregistrée (même opérateur / horodatage / montant / sens). */
    data object Duplicate : SmsProcessingOutcome

    /** Transaction créée. */
    data class Processed(val transactionId: Long, val isBonus: Boolean) : SmsProcessingOutcome
}

/**
 * Transforme un SMS d'opérateur en transaction persistée : analyse, contrôle de
 * doublon, calcul du bénéfice, insertion, puis rapprochement du bonus le cas
 * échéant.
 */
class ProcessSmsUseCase(
    private val transactionRepository: TransactionRepository,
    private val calculateProfit: CalculateProfitUseCase,
    private val bonusMatchingService: BonusMatchingService,
) {

    suspend operator fun invoke(raw: RawSms): SmsProcessingOutcome {
        val parsed = SMSParser.parse(raw.body, raw.sender)
        if (parsed == null) {
            Timber.v("SMS ignoré (non reconnu)")
            return SmsProcessingOutcome.Ignored
        }

        val isDuplicate = transactionRepository.isDuplicate(
            operator = parsed.operator.storageValue,
            timestamp = raw.timestamp,
            amount = parsed.amount,
            type = parsed.type.storageValue,
        )
        if (isDuplicate) {
            Timber.d("SMS déjà traité : %s %.0f Ar", parsed.operator.storageValue, parsed.amount)
            return SmsProcessingOutcome.Duplicate
        }

        val profit = if (parsed.isBonus) {
            0.0
        } else {
            calculateProfit(parsed.amount, parsed.operator, parsed.type)
        }

        val transaction = Transaction(
            operator = parsed.operator.storageValue,
            amount = parsed.amount,
            transactionType = parsed.type.storageValue,
            phoneNumber = parsed.phoneNumber,
            reference = parsed.reference,
            timestamp = raw.timestamp,
            bonusAmount = if (parsed.isBonus) parsed.amount else 0.0,
            bonusLinked = false,
            profitCalculated = profit,
        )

        val id = transactionRepository.insertTransaction(transaction)
        Timber.d("Transaction #%d enregistrée (bénéfice %.0f Ar)", id, profit)

        if (parsed.isBonus) {
            bonusMatchingService.matchBonusToTransaction(transaction.copy(id = id.toInt()))
        }

        return SmsProcessingOutcome.Processed(transactionId = id, isBonus = parsed.isBonus)
    }
}
