package com.dachkaboiz.betterbudget_bestbudget.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory

@Dao
interface SubCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: SubCategory): Long //did this for add subcategory, may remove long

    @Update
    suspend fun updateSubCategory(subCategory: SubCategory)

    @Delete
    suspend fun deleteSubCategory(subCategory: SubCategory)

    @Query("SELECT * FROM subcategories WHERE parentCategoryID = :categoryID")
    suspend fun getSubCategoriesByCategory(categoryID: Int): List<SubCategory>

    @Query("SELECT * FROM subcategories WHERE subCategoryID = :subCategoryID")
    suspend fun getSubCategoryById(subCategoryID: Int): SubCategory?
}