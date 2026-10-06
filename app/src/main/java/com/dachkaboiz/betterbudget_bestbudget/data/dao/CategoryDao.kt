package com.dachkaboiz.betterbudget_bestbudget.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE userEmail = :email")
    suspend fun getCategoriesByUser(email: String): List<Category>

    @Query("SELECT * FROM categories WHERE categoryID = :categoryID")
    suspend fun getCategoryById(categoryID: Int): Category?
}