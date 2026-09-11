package com.tinah.transactify.domain.model

import com.tinah.transactify.data.db.entity.Client

/** Modèle UI d'un client : classification déjà typée (enum) plutôt qu'en chaîne brute. */
data class ClientItem(
    val id: Int,
    val phoneNumber: String,
    val name: String?,
    val totalReceived: Double,
    val totalSent: Double,
    val transactionCount: Int,
    val lastInteraction: Long,
    val classification: ClientClassification,
) {
    /** Volume cumulé (reçu + envoyé), utilisé pour le tri et le seuil VIP. */
    val totalVolume: Double get() = totalReceived + totalSent

    /** Nom si renseigné, sinon le numéro de téléphone. */
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: phoneNumber
}

fun Client.toItem(): ClientItem = ClientItem(
    id = id,
    phoneNumber = phoneNumber,
    name = name,
    totalReceived = totalReceived,
    totalSent = totalSent,
    transactionCount = transactionCount,
    lastInteraction = lastInteraction,
    classification = ClientClassification.fromStorage(classification),
)
