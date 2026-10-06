package com.dachkaboiz.betterbudget_bestbudget.data.model

data class SubCategoryGoal(
    val subCatGoalID: String ="",
    val subCategoryID: String = "",
    val categoryID: String = "",
    val minGoal: Double? = null,
    val maxGoal: Double? = null,
    val month: Int = 0,
    val year: Int = 0
)