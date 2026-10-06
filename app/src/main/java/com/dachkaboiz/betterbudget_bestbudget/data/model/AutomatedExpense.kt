package com.dachkaboiz.betterbudget_bestbudget.data.model

data class AutomatedExpense(
    var firebaseId: String = "",
    var categoryFirebaseId: String = "",
    var amount: Double = 0.0,
    var description: String? = null,
    var imageUri: String? = null,
    var frequencyUnit: String = "",
    var frequencyMultiplier: Int = 1,
    var nextRunDate: Long = 0L,
    var startDate: Long = 0L,
    var lastRunDate: Long? = null,
    var active: Boolean = true,
    var userEmail: String = ""
)
