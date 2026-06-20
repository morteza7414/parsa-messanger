package com.example.parsamessenger

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE address = :address ORDER BY timestamp ASC")
    fun getMessages(address: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE address = :address AND isRead = 0 AND isMine = 0
    """)
    fun getUnreadCount(address: String): Flow<Int>

    @Query("UPDATE messages SET isRead = 1 WHERE address = :address")
    suspend fun markAsRead(address: String)

    @Query("""
        SELECT * FROM messages
        WHERE id IN (
            SELECT MAX(id) FROM messages GROUP BY address
        )
        ORDER BY timestamp DESC
    """)
    fun getConversations(): Flow<List<MessageEntity>>
}
