package com.tinah.transactify.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tinah.transactify.data.db.AppDatabase
import com.tinah.transactify.data.db.entity.Client
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClientDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ClientDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.clientDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun client(phone: String, name: String?, received: Double, sent: Double) = Client(
        phoneNumber = phone,
        name = name,
        totalReceived = received,
        totalSent = sent,
    )

    @Test
    fun `searchClients trie par volume decroissant sans filtre`() = runTest {
        dao.insertClient(client("+26132000001", "Rakoto", received = 10_000.0, sent = 0.0))
        dao.insertClient(client("+26132000002", "Rabe", received = 100_000.0, sent = 50_000.0))

        val result = dao.searchClients("").first()

        assertEquals(listOf("+26132000002", "+26132000001"), result.map { it.phoneNumber })
    }

    @Test
    fun `searchClients filtre par nom, insensible a la casse partielle`() = runTest {
        dao.insertClient(client("+26132000001", "Rakoto Jean", received = 0.0, sent = 0.0))
        dao.insertClient(client("+26132000002", "Rabe", received = 0.0, sent = 0.0))

        val result = dao.searchClients("rako").first()

        assertEquals(listOf("+26132000001"), result.map { it.phoneNumber })
    }

    @Test
    fun `searchClients filtre aussi par numero`() = runTest {
        dao.insertClient(client("+26132000001", "Rakoto", received = 0.0, sent = 0.0))
        dao.insertClient(client("+26134000002", "Rabe", received = 0.0, sent = 0.0))

        val result = dao.searchClients("000001").first()

        assertEquals(listOf("+26132000001"), result.map { it.phoneNumber })
    }

    @Test
    fun `observeClientByPhone reagit aux mises a jour`() = runTest {
        dao.insertClient(client("+26132000001", null, received = 0.0, sent = 0.0))

        val before = dao.observeClientByPhone("+26132000001").first()
        assertEquals(null, before?.name)

        val updated = before!!.copy(name = "Rakoto")
        dao.updateClient(updated)

        val after = dao.observeClientByPhone("+26132000001").first()
        assertEquals("Rakoto", after?.name)
    }

    @Test
    fun `getClientByPhone renvoie null si absent`() = runTest {
        assertEquals(null, dao.getClientByPhone("+26139999999"))
    }

    @Test
    fun `getAllClients trie par derniere interaction decroissante`() = runTest {
        dao.insertClient(client("+26132000001", "Rakoto", 0.0, 0.0).copy(lastInteraction = 1_000L))
        dao.insertClient(client("+26132000002", "Rabe", 0.0, 0.0).copy(lastInteraction = 2_000L))

        assertEquals(
            listOf("+26132000002", "+26132000001"),
            dao.getAllClients().first().map { it.phoneNumber },
        )
    }

    @Test
    fun `getClientsByClassification ne renvoie que la classification demandee`() = runTest {
        dao.insertClient(client("+26132000001", "Rakoto", 0.0, 0.0).copy(classification = "VIP"))
        dao.insertClient(client("+26132000002", "Rabe", 0.0, 0.0).copy(classification = "REGULAR"))

        val vips = dao.getClientsByClassification("VIP").first()

        assertEquals(listOf("+26132000001"), vips.map { it.phoneNumber })
    }

    @Test
    fun `getTopClients limite et trie par nombre de transactions`() = runTest {
        dao.insertClient(client("+26132000001", "Rakoto", 0.0, 0.0).copy(transactionCount = 5))
        dao.insertClient(client("+26132000002", "Rabe", 0.0, 0.0).copy(transactionCount = 20))
        dao.insertClient(client("+26132000003", "Be", 0.0, 0.0).copy(transactionCount = 1))

        val top2 = dao.getTopClients(2).first()

        assertEquals(listOf("+26132000002", "+26132000001"), top2.map { it.phoneNumber })
    }

    @Test
    fun `getClientCount compte tous les clients`() = runTest {
        dao.insertClient(client("+26132000001", "Rakoto", 0.0, 0.0))
        dao.insertClient(client("+26132000002", "Rabe", 0.0, 0.0))

        assertEquals(2, dao.getClientCount().first())
    }

    @Test
    fun `deleteClient retire le client cible`() = runTest {
        dao.insertClient(client("+26132000001", "Rakoto", 0.0, 0.0))
        val saved = dao.getClientByPhone("+26132000001")!!

        dao.deleteClient(saved)

        assertEquals(null, dao.getClientByPhone("+26132000001"))
    }
}
