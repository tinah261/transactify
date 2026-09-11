package com.tinah.transactify.domain.usecase

import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.data.repository.TransactionRepository
import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.utils.BonusMatchingService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProcessSmsUseCaseTest {

    private val repository = mock<TransactionRepository>()
    private val bonusMatchingService = mock<BonusMatchingService>()
    private val useCase = ProcessSmsUseCase(
        transactionRepository = repository,
        calculateProfit = CalculateProfitUseCase { CommissionRates.DEFAULT },
        bonusMatchingService = bonusMatchingService,
    )

    private val orangeSend =
        "Orange Money: Vous avez envoye 100 000 Ar a +261 32 12 34 56. Ref: R1."

    @Test
    fun `ignore un SMS non reconnu`() = runTest {
        val outcome = useCase(RawSms("Promo: -50% ce week-end !", null, 1_000L))

        assertEquals(SmsProcessingOutcome.Ignored, outcome)
        verify(repository, never()).insertTransaction(any())
    }

    @Test
    fun `enregistre une transaction avec le benefice calcule`() = runTest {
        whenever(repository.isDuplicate(any(), any(), any(), any(), any())).thenReturn(false)
        whenever(repository.insertTransaction(any())).thenReturn(7L)

        val outcome = useCase(RawSms(orangeSend, "OrangeMoney", 5_000L))

        val captor = argumentCaptor<Transaction>()
        verify(repository).insertTransaction(captor.capture())
        assertEquals(100_000.0, captor.firstValue.amount, 0.0)
        assertEquals(3_000.0, captor.firstValue.profitCalculated, 0.0)
        assertEquals(SmsProcessingOutcome.Processed(transactionId = 7L, isBonus = false), outcome)
    }

    @Test
    fun `saute les doublons`() = runTest {
        whenever(repository.isDuplicate(any(), any(), any(), any(), any())).thenReturn(true)

        val outcome = useCase(RawSms(orangeSend, null, 5_000L))

        assertEquals(SmsProcessingOutcome.Duplicate, outcome)
        verify(repository, never()).insertTransaction(any())
    }

    @Test
    fun `un id -1 (index unique) est traite comme un doublon`() = runTest {
        whenever(repository.isDuplicate(any(), any(), any(), any(), any())).thenReturn(false)
        whenever(repository.insertTransaction(any())).thenReturn(-1L)

        val outcome = useCase(RawSms(orangeSend, null, 5_000L))

        assertEquals(SmsProcessingOutcome.Duplicate, outcome)
        verify(bonusMatchingService, never()).matchBonusToTransaction(any())
    }

    @Test
    fun `rapproche le bonus apres insertion`() = runTest {
        whenever(repository.isDuplicate(any(), any(), any(), any(), any())).thenReturn(false)
        whenever(repository.insertTransaction(any())).thenReturn(9L)
        val bonusSms =
            "Orange Money: BONUS Vous avez recu 1 500 Ar de +261 32 12 34 56. Ref: R1."

        val outcome = useCase(RawSms(bonusSms, null, 6_000L))

        assertEquals(SmsProcessingOutcome.Processed(transactionId = 9L, isBonus = true), outcome)
        verify(bonusMatchingService).matchBonusToTransaction(any())
    }
}
