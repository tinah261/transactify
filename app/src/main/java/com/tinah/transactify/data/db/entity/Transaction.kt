package com.tinah.transactify.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * L'index unique sur (operator, timestamp, amount, transaction_type, phone_number)
 * empêche le retraitement d'un même SMS de créer un doublon en base — filet de
 * sécurité en plus du contrôle applicatif de
 * [com.tinah.transactify.domain.usecase.ProcessSmsUseCase]. `phone_number` est
 * inclus pour éviter qu'une collision fortuite entre deux clients différents
 * (même opérateur/montant/sens à la même milliseconde) ne fasse ignorer à tort
 * la transaction du second client.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(
            value = ["operator", "timestamp", "amount", "transaction_type", "phone_number"],
            unique = true,
        ),
    ],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "operator")
    val operator: String, // "Orange Money", "Airtel Money", "M-Vola"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String, // "REÇU", "ENVOYÉ"

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "reference")
    val reference: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "bonus_amount")
    val bonusAmount: Double = 0.0,

    @ColumnInfo(name = "bonus_linked")
    val bonusLinked: Boolean = false,

    @ColumnInfo(name = "profit_calculated")
    val profitCalculated: Double = 0.0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
