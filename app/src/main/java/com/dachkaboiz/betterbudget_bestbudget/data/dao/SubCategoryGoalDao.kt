package com.dachkaboiz.betterbudget_bestbudget.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal

@Dao
interface SubCategoryGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategoryGoal(subCategoryGoal: SubCategoryGoal)

    @Update
    suspend fun updateSubCategoryGoal(subCategoryGoal: SubCategoryGoal)

    @Delete
    suspend fun deleteSubCategoryGoal(subCategoryGoal: SubCategoryGoal)

    @Query("SELECT * FROM subcategory_goals WHERE subCategoryID = :subCategoryID LIMIT 1")
    suspend fun getGoalsBySubCategory(subCategoryID: Int): SubCategoryGoal?

    @Query("SELECT * FROM subcategory_goals WHERE subCategoryID = :subCategoryID AND month = :month AND year = :year")
    suspend fun getGoalBySubCategoryAndMonth(subCategoryID: Int, month: Int, year: Int): SubCategoryGoal?

    @Query("SELECT * FROM subcategory_goals WHERE categoryID = :categoryID AND month = :month AND year = :year")
    suspend fun getGoalsByCategoryAndMonth(categoryID: Int, month: Int, year: Int): List<SubCategoryGoal>

    @Query("SELECT * FROM subcategory_goals WHERE categoryID = :categoryID")
    suspend fun getGoalsByCategory(categoryID: Int): List<SubCategoryGoal>
}