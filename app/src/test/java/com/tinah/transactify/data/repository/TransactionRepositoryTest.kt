package com.tinah.transactify.data.repository

import com.tinah.transactify.data.db.dao.ClientDao
import com.tinah.transactify.data.db.dao.TransactionDao
import com.tinah.transactify.data.db.entity.Client
import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.domain.model.ClientClassification
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.domain.usecase.ClassifyClientUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TransactionRepositoryTest {

    private val transactionDao = mock<TransactionDao>()
    private val clientDao = mock<ClientDao>()
    private val repository = TransactionRepository(transactionDao, clientDao, ClassifyClientUseCase())

    private val phone = "+26132000000"

    private fun transaction(type: String = TransactionType.RECU.storageValue) = Transaction(
        operator = "Orange Money",
        amount = 10_000.0,
        transactionType = type,
        phoneNumber = phone,
        reference = "R1",
        timestamp = 1_000L,
    )

    @Test
    fun `insertion cree un nouveau client avec les agregats SQL`() = runTest {
        whenever(transactionDao.insertTransaction(any())).thenReturn(1L)
        whenever(transactionDao.sumAmountForClientByType(phone, TransactionType.RECU.storageValue)).thenReturn(10_000.0)
        whenever(transactionDao.sumAmountForClientByType(phone, TransactionType.ENVOYE.storageValue)).thenReturn(0.0)
        whenever(transactionDao.countForClient(phone)).thenReturn(1)
        whenever(clientDao.getClientByPhone(phone)).thenReturn(null)

        val id = repository.insertTransaction(transaction())

        assertEquals(1L, id)
        val captor = argumentCaptor<Client>()
        verify(clientDao).insertClient(captor.capture())
        assertEquals(10_000.0, captor.firstValue.totalReceived, 0.0)
        assertEquals(0.0, captor.firstValue.totalSent, 0.0)
        // 1 seule transaction -> ONE_TIME, pas REGULAR ni VIP
        assertEquals(ClientClassification.ONE_TIME.name, captor.firstValue.classification)
    }

    @Test
    fun `insertion met a jour un client existant sans doublonner le bonus`() = runTest {
        whenever(transactionDao.insertTransaction(any())).thenReturn(2L)
        whenever(transactionDao.sumAmountForClientByType(phone, TransactionType.RECU.storageValue)).thenReturn(30_000.0)
        whenever(transactionDao.sumAmountForClientByType(phone, TransactionType.ENVOYE.storageValue)).thenReturn(20_000.0)
        whenever(transactionDao.countForClient(phone)).thenReturn(25)
        val existing = Client(id = 7, phoneNumber = phone, name = "Rakoto", transactionCount = 24)
        whenever(clientDao.getClientByPhone(phone)).thenReturn(existing)

        repository.insertTransaction(transaction())

        val captor = argumentCaptor<Client>()
        verify(clientDao).updateClient(captor.capture())
        assertEquals("Rakoto", captor.firstValue.name)
        assertEquals(30_000.0, captor.firstValue.totalReceived, 0.0)
        assertEquals(20_000.0, captor.firstValue.totalSent, 0.0)
        assertEquals(25, captor.firstValue.transactionCount)
        // 25 transactions >= seuil VIP
        assertEquals(ClientClassification.VIP.name, captor.firstValue.classification)
    }

    @Test
    fun `un doublon ignore par l'index unique ne touche pas les stats client`() = runTest {
        whenever(transactionDao.insertTransaction(any())).thenReturn(-1L)

        val id = repository.insertTransaction(transaction())

        assertEquals(-1L, id)
        verify(clientDao, never()).getClientByPhone(any())
        verify(clientDao, never()).insertClient(any())
        verify(clientDao, never()).updateClient(any())
    }

    @Test
    fun `isDuplicate delegue au DAO en incluant le numero de client`() = runTest {
        whenever(
            transactionDao.countMatching(eq("Orange Money"), eq(1_000L), eq(10_000.0), eq("REÇU"), eq(phone)),
        ).thenReturn(1)

        assertEquals(true, repository.isDuplicate("Orange Money", 1_000L, 10_000.0, "REÇU", phone))
    }

    @Test
    fun `refreshClientStats est appelable directement (utilise par BonusMatchingService)`() = runTest {
        whenever(transactionDao.sumAmountForClientByType(phone, TransactionType.RECU.storageValue)).thenReturn(5_000.0)
        whenever(transactionDao.sumAmountForClientByType(phone, TransactionType.ENVOYE.storageValue)).thenReturn(0.0)
        whenever(transactionDao.countForClient(phone)).thenReturn(1)
        whenever(clientDao.getClientByPhone(phone)).thenReturn(null)

        repository.refreshClientStats(phone)

        val captor = argumentCaptor<Client>()
        verify(clientDao).insertClient(captor.capture())
        assertEquals(5_000.0, captor.firstValue.totalReceived, 0.0)
    }
}
