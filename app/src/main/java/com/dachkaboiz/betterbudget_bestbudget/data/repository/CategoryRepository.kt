package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.dao.CategoryDao
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category

class CategoryRepository(private val categoryDao: CategoryDao) {

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    suspend fun getCategoriesByUser(email: String): List<Category> {
        return categoryDao.getCategoriesByUser(email)
    }

    suspend fun getCategoryById(categoryID: Int): Category? {
        return categoryDao.getCategoryById(categoryID)
    }
}