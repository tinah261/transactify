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

    @Test
    fun `getTotalReceived et getTotalSent ne sommet que le sens correspondant`() = runTest {
        dao.insertTransaction(tx(type = TransactionType.RECU.storageValue, amount = 10_000.0, timestamp = 1_000L))
        dao.insertTransaction(tx(type = TransactionType.RECU.storageValue, amount = 5_000.0, timestamp = 2_000L))
        dao.insertTransaction(tx(type = TransactionType.ENVOYE.storageValue, amount = 7_000.0, timestamp = 3_000L))

        assertEquals(15_000.0, dao.getTotalReceived().first())
        assertEquals(7_000.0, dao.getTotalSent().first())
    }

    @Test
    fun `getTotalProfit somme le benefice de toutes les transactions`() = runTest {
        dao.insertTransaction(tx(timestamp = 1_000L).copy(profitCalculated = 300.0))
        dao.insertTransaction(tx(timestamp = 2_000L).copy(profitCalculated = 450.0))

        assertEquals(750.0, dao.getTotalProfit().first())
    }

    @Test
    fun `les totaux sont null sans transaction (pas zero)`() = runTest {
        assertEquals(null, dao.getTotalReceived().first())
        assertEquals(null, dao.getTotalSent().first())
        assertEquals(null, dao.getTotalProfit().first())
    }

    @Test
    fun `getFilteredTransactions filtre par operateur et periode`() = runTest {
        dao.insertTransaction(tx(operator = "Orange Money", timestamp = 1_000L))
        dao.insertTransaction(tx(operator = "M-Vola", timestamp = 2_000L))
        dao.insertTransaction(tx(operator = "Orange Money", timestamp = 9_000L))

        val orangeOnly = dao.getFilteredTransactions("Orange Money", 0L, Long.MAX_VALUE).first()
        assertEquals(2, orangeOnly.size)

        val everyone = dao.getFilteredTransactions(null, 0L, Long.MAX_VALUE).first()
        assertEquals(3, everyone.size)

        val orangeInWindow = dao.getFilteredTransactions("Orange Money", 0L, 5_000L).first()
        assertEquals(1, orangeInWindow.size)
    }

    @Test
    fun `getLatestTransactions limite et trie par date decroissante`() = runTest {
        dao.insertTransaction(tx(timestamp = 1_000L))
        dao.insertTransaction(tx(timestamp = 3_000L))
        dao.insertTransaction(tx(timestamp = 2_000L))

        val latest = dao.getLatestTransactions(2).first()

        assertEquals(listOf(3_000L, 2_000L), latest.map { it.timestamp })
    }

    @Test
    fun `getTransactionById renvoie null si absente`() = runTest {
        assertEquals(null, dao.getTransactionById(999).first())
    }

    @Test
    fun `getTransactionById renvoie la transaction demandee`() = runTest {
        val id = dao.insertTransaction(tx())
        assertEquals(id.toInt(), dao.getTransactionById(id.toInt()).first()?.id)
    }

    @Test
    fun `getTransactionsByClient ne renvoie que les transactions de ce numero`() = runTest {
        dao.insertTransaction(tx(phone = "+26132000001", timestamp = 1_000L))
        dao.insertTransaction(tx(phone = "+26132000002", timestamp = 2_000L))

        assertEquals(1, dao.getTransactionsByClient("+26132000001").first().size)
    }

    @Test
    fun `updateTransaction et deleteTransaction modifient bien la ligne ciblee`() = runTest {
        val id = dao.insertTransaction(tx())
        val saved = dao.getTransactionById(id.toInt()).first()!!

        dao.updateTransaction(saved.copy(profitCalculated = 999.0))
        assertEquals(999.0, dao.getTransactionById(id.toInt()).first()?.profitCalculated)

        dao.deleteTransaction(saved)
        assertEquals(null, dao.getTransactionById(id.toInt()).first())
    }

    @Test
    fun `insertTransactions insere plusieurs lignes en une fois`() = runTest {
        dao.insertTransactions(listOf(tx(timestamp = 1_000L), tx(timestamp = 2_000L)))
        assertEquals(2, dao.getAllTransactions().first().size)
    }
}
