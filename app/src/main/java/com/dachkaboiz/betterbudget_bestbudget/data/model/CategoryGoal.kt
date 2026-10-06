package com.dachkaboiz.betterbudget_bestbudget.data.model

data class CategoryGoal(
    val goalId: String = "",
    val categoryId: String = "",   // firebaseId of Category
    val minGoal: Double? = null,
    val maxGoal: Double? = null,
    val month: Int = 0,
    val year: Int = 0
)
