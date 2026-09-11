package com.tinah.transactify.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tinah.transactify.data.db.entity.Client
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("SELECT * FROM clients ORDER BY last_interaction DESC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE phone_number = :phoneNumber")
    suspend fun getClientByPhone(phoneNumber: String): Client?

    /** Variante réactive de [getClientByPhone], pour l'écran de détail client. */
    @Query("SELECT * FROM clients WHERE phone_number = :phoneNumber")
    fun observeClientByPhone(phoneNumber: String): Flow<Client?>

    /**
     * Liste triée par volume (reçu + envoyé) décroissant, filtrée par nom ou
     * numéro si [query] est non vide.
     */
    @Query(
        """
        SELECT * FROM clients
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR phone_number LIKE '%' || :query || '%')
        ORDER BY (total_received + total_sent) DESC
        """
    )
    fun searchClients(query: String): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE classification = :classification ORDER BY transaction_count DESC")
    fun getClientsByClassification(classification: String): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY transaction_count DESC LIMIT :limit")
    fun getTopClients(limit: Int = 10): Flow<List<Client>>

    @Query("SELECT COUNT(*) FROM clients")
    fun getClientCount(): Flow<Int>
}
