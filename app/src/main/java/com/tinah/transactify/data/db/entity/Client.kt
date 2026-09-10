package com.tinah.transactify.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    indices = [Index(value = ["phone_number"], unique = true)]
)
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "name")
    val name: String? = null,

    @ColumnInfo(name = "total_received")
    val totalReceived: Double = 0.0,

    @ColumnInfo(name = "total_sent")
    val totalSent: Double = 0.0,

    @ColumnInfo(name = "transaction_count")
    val transactionCount: Int = 0,

    @ColumnInfo(name = "last_interaction")
    val lastInteraction: Long = 0,

    @ColumnInfo(name = "classification")
    val classification: String = "REGULAR", // VIP, REGULAR, ONE_TIME

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
