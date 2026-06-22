package com.example.parsamessenger

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE address = :address ORDER BY timestamp ASC")
    fun getMessages(address: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE address = :address AND isRead = 0 AND isMine = 0
    """)
    fun getUnreadCount(address: String): Flow<Int>

    @Query("""
        UPDATE messages
        SET isRead = 1
        WHERE address = :address AND isMine = 0
    """)
    suspend fun markAsRead(address: String)

    // آخرین پیام هر مکالمه + وضعیت unread
    @Query("""
        SELECT 
            m.address as address,
            m.body as body,
            m.timestamp as timestamp,
            EXISTS(
                SELECT 1 FROM messages 
                WHERE address = m.address 
                AND isRead = 0 
                AND isMine = 0
            ) as hasUnread
        FROM messages m
        INNER JOIN (
            SELECT address, MAX(timestamp) as maxTime
            FROM messages
            GROUP BY address
        ) grouped
        ON m.address = grouped.address 
        AND m.timestamp = grouped.maxTime
        ORDER BY m.timestamp DESC
    """)
    fun getConversations(): Flow<List<ConversationPreview>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessagesById(id: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE address = :address")
    suspend fun deleteConversation(address: String)
}
