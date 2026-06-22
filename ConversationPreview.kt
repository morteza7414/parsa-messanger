package com.example.parsamessenger

data class ConversationPreview(
    val address: String,
    val body: String,
    val timestamp: Long,
    val hasUnread: Boolean
)
