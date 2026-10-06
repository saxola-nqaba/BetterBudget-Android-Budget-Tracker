package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

data class Category(
    val firebaseId: String = "",
    val userEmail: String = "",
    val categoryName: String = "",
    val categoryIcon: String = "",
    val categoryDescription: String = ""
)