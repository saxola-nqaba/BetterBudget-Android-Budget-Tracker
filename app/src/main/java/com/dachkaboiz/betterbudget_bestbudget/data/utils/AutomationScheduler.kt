package com.dachkaboiz.betterbudget_bestbudget.data.utils

import com.dachkaboiz.betterbudget_bestbudget.data.model.AutomatedExpense
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.AutomatedExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object AutomationScheduler {

    private val zoneId = ZoneId.systemDefault()

    // Filter list to items that are active and whose scheduled runtime has arrived or passed
    fun getDueExpenses(allAutomated: List<AutomatedExpense>): List<AutomatedExpense> {
        val now = System.currentTimeMillis()
        return allAutomated.filter { it.active && it.nextRunDate <= now }
    }

    // Helper to transform epoch millis into a manageable LocalDate
    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    // Helper to transform LocalDate back to epoch millis at the start of that local day
    private fun LocalDate.toEpochMillis(): Long =
        this.atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun calculateNextRunDate(fromDate: Long, unit: String): Long {
        val localDate = fromDate.toLocalDate()
        val nextDate = when (unit.uppercase()) {
            "DAY"   -> localDate.plusDays(1)
            "WEEK"  -> localDate.plusWeeks(1)
            "MONTH" -> localDate.plusMonths(1)
            "YEAR"  -> localDate.plusYears(1)
            else    -> localDate.plusDays(1)
        }
        return nextDate.toEpochMillis()
    }

    suspend fun runCatchUpFor(
        auto: AutomatedExpense,
        expenseRepo: ExpenseRepository,
        automatedRepo: AutomatedExpenseRepository
    ): AutomatedExpense {

        val nowLocalDate = LocalDate.now(zoneId)
        var currentRunDate = auto.nextRunDate.toLocalDate()

        while (!currentRunDate.isAfter(nowLocalDate) && auto.frequencyMultiplier > 0) {

            val newId = expenseRepo.generateExpenseId()

            val expense = Expense(
                expenseID = newId,
                userEmail = auto.userEmail,
                categoryId = auto.categoryFirebaseId,
                subCategoryId = null,
                expenseAmount = auto.amount,
                expenseDate = currentRunDate.toEpochMillis(),
                expenseDescription = auto.description,
                imageUri = auto.imageUri,
                imageName = null,
                imageDescription = null,
                automationFrequency = auto.frequencyUnit,
                automationMultiplier = auto.frequencyMultiplier
            )

            // Insert the expense
            expenseRepo.insertExpense(expense)

            // Move to next scheduled date
            val nextDate = when (auto.frequencyUnit.uppercase()) {
                "DAY" -> currentRunDate.plusDays(1)
                "WEEK" -> currentRunDate.plusWeeks(1)
                "MONTH" -> currentRunDate.plusMonths(1)
                "YEAR" -> currentRunDate.plusYears(1)
                else -> currentRunDate.plusDays(1)
            }

            //  Update ALL automation fields that change
            auto.frequencyMultiplier -= 1
            auto.lastRunDate = expense.expenseDate
            auto.nextRunDate = nextDate.toEpochMillis()

            //  Save automation immediately (critical)
            automatedRepo.update(auto)

            // Continue loop
            currentRunDate = nextDate
        }

        return auto
    }





    fun calculateMissedRuns(lastRunDate: Long, unit: String): Int {
        val zoneId = ZoneId.systemDefault() // Employs user's local timezone

        // 1. Convert raw epoch timestamps cleanly into Calendar Dates
        val lastRunLocalDate = Instant.ofEpochMilli(lastRunDate).atZone(zoneId).toLocalDate()
        val nowLocalDate = LocalDate.now(zoneId)

        // If we haven't even passed the last run date, missed runs is zero
        if (!nowLocalDate.isAfter(lastRunLocalDate)) return 0

        // 2. Calculate the exact calendar intervals passed
        val base = when (unit.uppercase()) {
            "DAY"   -> ChronoUnit.DAYS.between(lastRunLocalDate, nowLocalDate)
            "WEEK"  -> ChronoUnit.WEEKS.between(lastRunLocalDate, nowLocalDate)
            "MONTH" -> ChronoUnit.MONTHS.between(lastRunLocalDate, nowLocalDate)
            "YEAR"  -> ChronoUnit.YEARS.between(lastRunLocalDate, nowLocalDate)
            else    -> 0L
        }

        // 3. Return your base count. ChronoUnit.between is exclusive of the end date,
        // so if it's a new day/week/month/year, this naturally serves as your "+ 1"
        return base.toInt()
    }
}
