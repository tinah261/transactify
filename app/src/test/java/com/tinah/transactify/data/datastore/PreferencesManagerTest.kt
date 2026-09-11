package com.tinah.transactify.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.tinah.transactify.domain.model.OperatorType
import com.tinah.transactify.domain.model.TransactionType
import com.tinah.transactify.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var manager: PreferencesManager

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + Job())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            tmpFolder.newFile("transactify.preferences_pb")
        }
        manager = PreferencesManager(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `renvoie les taux par defaut quand rien n'est persiste`() = runBlocking {
        val rates = manager.currentRates()
        assertEquals(
            Constants.DefaultRates.ORANGE_RECU,
            rates.rateFor(OperatorType.ORANGE_MONEY, TransactionType.RECU),
            0.0,
        )
    }

    @Test
    fun `un taux persiste surcharge la valeur par defaut`() = runBlocking {
        manager.setRate(OperatorType.MVOLA, TransactionType.ENVOYE, 0.08)

        val rates = manager.currentRates()
        assertEquals(0.08, rates.rateFor(OperatorType.MVOLA, TransactionType.ENVOYE), 0.0)
        // les autres restent aux valeurs par defaut
        assertEquals(
            Constants.DefaultRates.ORANGE_ENVOYE,
            rates.rateFor(OperatorType.ORANGE_MONEY, TransactionType.ENVOYE),
            0.0,
        )
    }

    @Test
    fun `resetRates efface les surcharges`() = runBlocking {
        manager.setRate(OperatorType.AIRTEL_MONEY, TransactionType.RECU, 0.5)
        manager.resetRates()

        assertEquals(
            Constants.DefaultRates.AIRTEL_RECU,
            manager.currentRates().rateFor(OperatorType.AIRTEL_MONEY, TransactionType.RECU),
            0.0,
        )
    }

    @Test
    fun `le curseur SMS ne recule jamais`() = runBlocking {
        manager.setLastProcessedSmsTimestamp(5_000L)
        manager.setLastProcessedSmsTimestamp(3_000L)
        assertEquals(5_000L, manager.lastProcessedSmsTimestamp.first())

        manager.setLastProcessedSmsTimestamp(9_000L)
        assertEquals(9_000L, manager.lastProcessedSmsTimestamp.first())
    }

    @Test
    fun `resetSmsCursor peut faire reculer le curseur, contrairement a setLastProcessedSmsTimestamp`() = runBlocking {
        manager.setLastProcessedSmsTimestamp(9_000L)

        manager.resetSmsCursor(2_000L)

        assertEquals(2_000L, manager.lastProcessedSmsTimestamp.first())
    }

    @Test
    fun `un taux hors bornes est rejete`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { manager.setRate(OperatorType.ORANGE_MONEY, TransactionType.RECU, 1.5) }
        }
    }
}
