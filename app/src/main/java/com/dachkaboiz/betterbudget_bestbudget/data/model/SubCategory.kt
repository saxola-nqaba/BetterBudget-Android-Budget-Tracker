package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

data class SubCategory(
    val firebaseId: String = "",
    val parentFirebaseId: String = "",
    val subCategoryName: String = "",
    val subCategoryIcon: String = "",
    val subCategoryDescription: String = ""
)
