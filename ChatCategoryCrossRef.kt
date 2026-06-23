package com.example.parsamessenger

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "chat_category_cross_ref",
    primaryKeys = ["address", "categoryId"],
    indices = [Index(value = ["address"]), Index(value = ["categoryId"])]
)
data class ChatCategoryCrossRef(
    val address: String,
    val categoryId: Long
)
