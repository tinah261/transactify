package com.tinah.transactify.domain.usecase

import com.tinah.transactify.domain.model.CommissionRates
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateProfitUseCaseTest {

    private val useCase = CalculateProfitUseCase { CommissionRates.DEFAULT }

    @Test
    fun `applique le taux Orange Money envoye`() = runTest {
        val profit = useCase(100_000.0, OperatorType.ORANGE_MONEY, TransactionType.ENVOYE)
        assertEquals(3_000.0, profit, 0.0)
    }

    @Test
    fun `applique le taux Airtel Money recu`() = runTest {
        val profit = useCase(40_000.0, OperatorType.AIRTEL_MONEY, TransactionType.RECU)
        assertEquals(1_000.0, profit, 0.0)
    }

    @Test
    fun `montant nul ou negatif renvoie zero`() = runTest {
        assertEquals(0.0, useCase(0.0, OperatorType.MVOLA, TransactionType.ENVOYE), 0.0)
        assertEquals(0.0, useCase(-5_000.0, OperatorType.MVOLA, TransactionType.ENVOYE), 0.0)
    }

    @Test
    fun `utilise les taux surcharges fournis par le provider`() = runTest {
        val custom = CalculateProfitUseCase {
            CommissionRates.DEFAULT.withRate(OperatorType.MVOLA, TransactionType.ENVOYE, 0.10)
        }
        assertEquals(1_000.0, custom(10_000.0, OperatorType.MVOLA, TransactionType.ENVOYE), 0.0)
    }

    @Test
    fun `forStoredValues tolere des valeurs inconnues`() = runTest {
        assertEquals(0.0, useCase.forStoredValues(10_000.0, "Inconnu", "REÇU"), 0.0)
        // M-Vola recu = 2,2 %
        assertEquals(
            440.0,
            useCase.forStoredValues(20_000.0, "M-Vola", "REÇU"),
            0.0001,
        )
    }
}
