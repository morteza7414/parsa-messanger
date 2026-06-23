package com.example.parsamessenger

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // افزودن چت به دسته
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChatToCategory(crossRef: ChatCategoryCrossRef)

    // حذف چت از دسته
    @Query("""
        DELETE FROM chat_category_cross_ref
        WHERE address = :address AND categoryId = :categoryId
    """)
    suspend fun removeChatFromCategory(address: String, categoryId: Long)

    // گرفتن دسته های یک چت
    @Query("""
        SELECT categoryId FROM chat_category_cross_ref
        WHERE address = :address
    """)
    fun getCategoriesForChat(address: String): Flow<List<Long>>

    // گرفتن چت های یک دسته
    @Query("""
        SELECT address FROM chat_category_cross_ref
        WHERE categoryId = :categoryId
    """)
    fun getChatsInCategory(categoryId: Long): Flow<List<String>>
}
