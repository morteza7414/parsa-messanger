package com.example.parsamessenger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Delete
import androidx.room.Update

@Dao
interface MessageDao {

    @Query(
        """
        SELECT *
        FROM messages
        WHERE address = :address
        ORDER BY timestamp ASC
        """
    )
    fun getMessages(
        address: String
    ): Flow<List<MessageEntity>>

    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insert(
        message: MessageEntity
    )

    @Query(
        """
        SELECT *
        FROM messages
        GROUP BY address
        ORDER BY timestamp DESC
        """
    )
    fun getChats():
            Flow<List<MessageEntity>>

    @Delete
    suspend fun deleteMessage(
        message: MessageEntity
    )

    @Update
    suspend fun updateMessage(
        message: MessageEntity
    )

}