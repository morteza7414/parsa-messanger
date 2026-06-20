package com.example.parsamessenger

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_category")
data class ChatCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val categoryId: Long
)
