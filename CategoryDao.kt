package com.example.parsamessenger

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignChatToCategory(chat: ChatCategoryEntity)

    @Query("SELECT categoryId FROM chat_category WHERE address = :address LIMIT 1")
    suspend fun getCategoryForChat(address: String): Long?

    @Query("SELECT address FROM chat_category WHERE categoryId = :categoryId")
    suspend fun getChatsForCategory(categoryId: Long): List<String>
}
