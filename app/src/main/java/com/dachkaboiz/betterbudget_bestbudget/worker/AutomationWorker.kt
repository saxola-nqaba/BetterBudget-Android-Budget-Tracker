package com.dachkaboiz.betterbudget_bestbudget.Worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dachkaboiz.betterbudget_bestbudget.data.model.AutomatedExpense
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.AutomatedExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.utils.AutomationScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min


class AutomationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.success()

        val userEmail = applicationContext
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("email", "") ?: ""

        val automatedRepo = AutomatedExpenseRepository(uid)
        val expenseRepo   = ExpenseRepository(uid)

        return try {
            val allAutomated = automatedRepo.getAll()

            for (auto in allAutomated) {

                if (!auto.active) continue

                // -----------------------------
                // 1. CALCULATE MISSED RUNS
                // -----------------------------
                val lastRun = auto.lastRunDate ?: auto.nextRunDate ?: continue

                val missed = AutomationScheduler.calculateMissedRuns(
                    lastRunDate = lastRun,
                    unit = auto.frequencyUnit
                )

                val runsToExecute = kotlin.math.min(missed, auto.frequencyMultiplier)

                if (runsToExecute <= 0) continue

                // -----------------------------
                // 2. EXECUTE AUTOMATION RUNS
                // -----------------------------
                repeat(runsToExecute) { i ->
                    val newExpenseId = expenseRepo.generateExpenseId()

                    val expense = Expense(
                        expenseID = newExpenseId,
                        userEmail = userEmail,
                        categoryId = auto.categoryFirebaseId,
                        subCategoryId = null,
                        expenseAmount = auto.amount,
                        expenseDate = lastRun + (i * 24L * 60 * 60 * 1000),
                        expenseDescription = auto.description,
                        imageUri = auto.imageUri,
                        imageName = null,
                        imageDescription = null,
                        automationFrequency = null,
                        automationMultiplier = null
                    )

                    expenseRepo.insertExpense(expense)
                }

                // -----------------------------
                // 3. UPDATE AUTOMATION ENTRY
                // -----------------------------
                val now = System.currentTimeMillis()

                val updated = auto.copy(
                    lastRunDate = now,
                    nextRunDate = AutomationScheduler.calculateNextRunDate(
                        fromDate = now,
                        unit = auto.frequencyUnit
                    ),
                    frequencyMultiplier = auto.frequencyMultiplier - runsToExecute,
                    active = (auto.frequencyMultiplier - runsToExecute) > 0
                )

                automatedRepo.update(updated)
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }
}
