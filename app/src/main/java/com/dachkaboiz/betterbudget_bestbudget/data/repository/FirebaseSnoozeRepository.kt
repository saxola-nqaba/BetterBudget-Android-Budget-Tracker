package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.SnoozeCount
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseSnoozeRepository(private val uid: String) {

    private val db = FirebaseDatabase.getInstance().reference
    private val snoozesRef = db.child("users").child(uid).child("snoozes")

    // Returns how many snoozes the user has used this month
    suspend fun getSnoozeCountForMonth(month: Int, year: Int): Int =

    // Code Attribution
    // This method was adapted from ProAndroidDev
    // https://proandroiddev.com/suspending-firebase-realtime-database-with-kotlin-coroutines-76b4651bc0e8
    // Kaustubh Patange
        
        suspendCoroutine { cont ->
            snoozesRef.get()
                .addOnSuccessListener { snap ->
                    val count = snap.children
                        .mapNotNull { it.getValue(SnoozeCount::class.java) }
                        .count { it.month == month && it.year == year }
                    cont.resume(count)
                }
                .addOnFailureListener { cont.resume(0) }
        }

    // Saves a snooze record to Firebase when user chooses USE_SNOOZE
    suspend fun saveSnooze(record: SnoozeCount) =
        suspendCoroutine<Unit> { cont ->
            snoozesRef.child(record.snoozeId)
                .setValue(record)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

    // Returns list of expense IDs that have been snoozed for a
    // specific category and month — used to exclude them from
    // the progress bar total in GoalHomeFragment
    suspend fun getSnoozedExpenseIds(
        categoryId: String,
        month: Int,
        year: Int
    ): List<String> =
        suspendCoroutine { cont ->
            snoozesRef.get()
                .addOnSuccessListener { snap ->
                    val ids = snap.children
                        .mapNotNull { it.getValue(SnoozeCount::class.java) }
                        .filter {
                            it.categoryId == categoryId &&
                                    it.month == month &&
                                    it.year == year
                        }
                        .map { it.expenseId }
                    cont.resume(ids)
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

    // Generates a unique ID for a new snooze record

    // Code Attribution
    // This method was adapted from Stack Overflow
    // https://stackoverflow.com/questions/37094631/how-to-get-push-key-before-pushing-data-to-firebase-android
    // Frank van Puffelen
    // https://stackoverflow.com/users/209103/frank-van-puffelen
    fun generateSnoozeId(): String = snoozesRef.push().key ?: ""
}