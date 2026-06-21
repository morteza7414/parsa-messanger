package com.example.parsamessenger


import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["address", "timestamp", "body"], unique = true)
    ]
)
data class MessageEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val address: String,

    val body: String,

    val timestamp: Long,

    val isMine: Boolean,

    val sent: Boolean,

    val delivered: Boolean,

    val isRead: Boolean,

    val failed: Boolean
)
