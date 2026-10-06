package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseCategoryGoalRepository(private val uid: String) {

    private val db = FirebaseDatabase.getInstance().reference
    private val goalsRef = db.child("users").child(uid).child("categoryGoals")

    fun generateGoalId(): String = goalsRef.push().key!!

    suspend fun insertGoal(goal: CategoryGoal) = suspendCoroutine<Unit> { cont ->
        goalsRef.child(goal.goalId)
            .setValue(goal)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resume(Unit) }
    }
    suspend fun getGoalById(goalId: String): CategoryGoal? =
        suspendCoroutine { cont ->
            goalsRef.child(goalId)
                .get()
                .addOnSuccessListener { snap ->
                    cont.resume(snap.getValue(CategoryGoal::class.java))
                }
                .addOnFailureListener { cont.resume(null) }
        }

    suspend fun updateGoal(goal: CategoryGoal) =
        suspendCoroutine<Unit> { cont ->
            goalsRef.child(goal.goalId)
                .setValue(goal)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }
    suspend fun getAllGoals(): List<CategoryGoal> =
        suspendCoroutine { cont ->
            goalsRef.get()
                .addOnSuccessListener { snap ->
                    cont.resume(snap.children.mapNotNull { it.getValue(CategoryGoal::class.java) })
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }


    suspend fun deleteGoal(goalId: String) =
        suspendCoroutine<Unit> { cont ->
            goalsRef.child(goalId).removeValue()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

}
