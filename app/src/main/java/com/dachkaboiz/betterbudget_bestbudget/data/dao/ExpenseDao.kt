package com.dachkaboiz.betterbudget_bestbudget.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userEmail = :email")
    suspend fun getExpensesByUser(email: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE categoryID = :categoryID")
    suspend fun getExpensesByCategory(categoryID: Int): List<Expense>

    @Query("SELECT * FROM expenses WHERE subCategoryID = :subCategoryID")
    suspend fun getExpensesBySubCategory(subCategoryID: Int): List<Expense>

    @Query("SELECT * FROM expenses WHERE expenseID = :expenseID")
    suspend fun getExpenseById(expenseID: Int): Expense?

    @Query("SELECT SUM(expenseAmount) FROM expenses WHERE categoryID = :categoryID AND expenseDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalSpentByCategory(categoryID: Int, startDate: Long, endDate: Long): Double?
}