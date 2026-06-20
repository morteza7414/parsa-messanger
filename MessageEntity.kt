package com.example.parsamessenger

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["address"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val address: String,
    val body: String,
    val isMine: Boolean,
    val timestamp: Long,
    val sent:Boolean = false,
    val delivered:Boolean = false,
    val isRead: Boolean = false

)

