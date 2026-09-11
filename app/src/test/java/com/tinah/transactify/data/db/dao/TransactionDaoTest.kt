package com.tinah.transactify.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tinah.transactify.data.db.AppDatabase
import com.tinah.transactify.data.db.entity.Transaction
import com.tinah.transactify.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests DAO sur une base en mémoire (vrai moteur SQLite via Robolectric) : les
 * agrégats SQL et l'index unique ne peuvent pas être vérifiés par un simple test
 * unitaire mocké.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    private val clientPhone = "+26132000000"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tx(
        operator: String = "Orange Money",
        amount: Double = 10_000.0,
        type: String = TransactionType.RECU.storageValue,
        phone: String = clientPhone,
        timestamp: Long = 1_000L,
    ) = Transaction(
        operator = operator,
        amount = amount,
        transactionType = type,
        phoneNumber = phone,
        reference = "R1",
        timestamp = timestamp,
    )

    @Test
    fun `insert ignore un doublon exact grace a l'index unique`() = runTest {
        val first = dao.insertTransaction(tx())
        val second = dao.insertTransaction(tx())

        assertTrue(first > 0)
        assertEquals(-1L, second)
        assertEquals(1, dao.getAllTransactions().first().size)
    }

    @Test
    fun `un horodatage different n'est pas un doublon`() = runTest {
        dao.insertTransaction(tx(timestamp = 1_000L))
        dao.insertTransaction(tx(timestamp = 2_000L))

        assertEquals(2, dao.getAllTransactions().first().size)
    }

    @Test
    fun `deux clients differents avec le meme operateur-montant-horodatage ne sont pas des doublons`() = runTest {
        val first = dao.insertTransaction(tx(phone = "+26132000001", timestamp = 1_000L))
        val second = dao.insertTransaction(tx(phone = "+26132000002", timestamp = 1_000L))

        assertTrue(first > 0)
        assertTrue(second > 0)
        assertEquals(2, dao.getAllTransactions().first().size)
    }

    @Test
    fun `countMatching inclut le numero de telephone dans la comparaison`() = runTest {
        dao.insertTransaction(tx(phone = "+26132000001", timestamp = 1_000L))

        assertEquals(
            1,
            dao.countMatching("Orange Money", 1_000L, 10_000.0, TransactionType.RECU.storageValue, "+26132000001"),
        )
        assertEquals(
            0,
            dao.countMatching("Orange Money", 1_000L, 10_000.0, TransactionType.RECU.storageValue, "+26132000002"),
        )
    }

    @Test
    fun `sumAmountForClientByType n'agrege que le sens demande`() = runTest {
        dao.insertTransaction(tx(type = TransactionType.RECU.storageValue, amount = 10_000.0, timestamp = 1_000L))
        dao.insertTransaction(tx(type = TransactionType.RECU.storageValue, amount = 5_000.0, timestamp = 2_000L))
        dao.insertTransaction(tx(type = TransactionType.ENVOYE.storageValue, amount = 7_000.0, timestamp = 3_000L))

        assertEquals(15_000.0, dao.sumAmountForClientByType(clientPhone, TransactionType.RECU.storageValue), 0.0)
        assertEquals(7_000.0, dao.sumAmountForClientByType(clientPhone, TransactionType.ENVOYE.storageValue), 0.0)
    }

    @Test
    fun `sumAmountForClientByType renvoie zero plutot que null sans resultat`() = runTest {
        assertEquals(0.0, dao.sumAmountForClientByType("+26139999999", TransactionType.RECU.storageValue), 0.0)
    }

    @Test
    fun `countForClient compte les deux sens confondus`() = runTest {
        dao.insertTransaction(tx(type = TransactionType.RECU.storageValue, timestamp = 1_000L))
        dao.insertTransaction(tx(type = TransactionType.ENVOYE.storageValue, timestamp = 2_000L))

        assertEquals(2, dao.countForClient(clientPhone))
    }

    @Test
    fun `getTransactionCountInRange respecte les bornes fournies`() = runTest {
        dao.insertTransaction(tx(timestamp = 1_000L))
        dao.insertTransaction(tx(timestamp = 5_000L))
        dao.insertTransaction(tx(timestamp = 9_000L))

        assertEquals(2, dao.getTransactionCountInRange(1_000L, 9_000L).first())
        assertEquals(3, dao.getTransactionCountInRange(0L, 10_000L).first())
    }
}
