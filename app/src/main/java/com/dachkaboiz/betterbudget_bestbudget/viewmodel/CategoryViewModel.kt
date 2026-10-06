package com.dachkaboiz.betterbudget_bestbudget.viewmodel

import androidx.lifecycle.ViewModel
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CategoryViewModel : ViewModel() {

    private val repository = FirebaseCategoryRepository()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category

    fun loadCategories(uid: String) {
        // Clear first so StateFlow always emits even when the new list
        // contains the same items as the previous one. Without this,
        // StateFlow compares the old and new list by reference — if
        // Firebase returns a new List object with identical contents,
        // the value appears unchanged and the collector never fires,
        // so the adapter never redraws after a delete or update.
        _categories.value = emptyList()

        repository.getCategories(uid) { list ->
            _categories.value = list
        }
    }

    fun loadCategory(uid: String, firebaseId: String) {
        _category.value = null

        repository.getCategoryById(uid, firebaseId) { cat ->
            _category.value = cat
        }
    }
}