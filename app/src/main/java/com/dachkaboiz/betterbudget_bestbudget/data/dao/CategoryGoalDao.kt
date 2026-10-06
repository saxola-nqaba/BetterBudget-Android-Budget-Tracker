package com.dachkaboiz.betterbudget_bestbudget.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal

@Dao
interface CategoryGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryGoal(categoryGoal: CategoryGoal)

    @Update
    suspend fun updateCategoryGoal(categoryGoal: CategoryGoal)

    @Delete
    suspend fun deleteCategoryGoal(categoryGoal: CategoryGoal)


    @Query("SELECT * FROM category_goals WHERE categoryID = :categoryID LIMIT 1")
    suspend fun getGoalsByCategory(categoryID: Int): CategoryGoal?

    @Query("SELECT * FROM category_goals WHERE categoryID = :categoryID AND month = :month AND year = :year")
    suspend fun getGoalByCategoryAndMonth(categoryID: Int, month: Int, year: Int): CategoryGoal?

    @Query("SELECT * FROM category_goals WHERE month = :month AND year = :year")
    suspend fun getAllGoalsByMonth(month: Int, year: Int): List<CategoryGoal>

    @Query("SELECT * FROM category_goals WHERE categoryGoalID = :id")
    suspend fun getGoalById(id: Int): CategoryGoal?

    @Query("SELECT * FROM category_goals")
    suspend fun getAllGoals(): List<CategoryGoal>



}