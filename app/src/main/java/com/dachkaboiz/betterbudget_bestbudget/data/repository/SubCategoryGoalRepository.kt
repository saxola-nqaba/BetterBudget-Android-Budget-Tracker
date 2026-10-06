package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SubCategoryGoalRepository(private val uid: String) {

    private val db = FirebaseDatabase.getInstance().reference
    private val goalsRef = db.child("users").child(uid).child("subCategoryGoals")

    fun generateGoalId(): String = goalsRef.push().key!!

    // INSERT
    suspend fun insertSubCategoryGoal(goal: SubCategoryGoal) =
        suspendCoroutine<Unit> { cont ->
            goalsRef.child(goal.subCatGoalID)
                .setValue(goal)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

    // UPDATE
    suspend fun updateSubCategoryGoal(goal: SubCategoryGoal) =
        suspendCoroutine<Unit> { cont ->
            goalsRef.child(goal.subCatGoalID)
                .setValue(goal)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

    // DELETE
    suspend fun deleteSubCategoryGoal(goalId: String) =
        suspendCoroutine<Unit> { cont ->
            goalsRef.child(goalId)
                .removeValue()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

    // GET GOAL FOR SUBCATEGORY (latest or only)
    suspend fun getGoalsBySubCategory(subCategoryId: String): SubCategoryGoal? =
        suspendCoroutine { cont ->
            goalsRef.orderByChild("subCategoryId")
                .equalTo(subCategoryId)
                .get()
                .addOnSuccessListener { snap ->
                    val list = snap.children.mapNotNull {
                        it.getValue(SubCategoryGoal::class.java)
                    }
                    cont.resume(list.firstOrNull())
                }
                .addOnFailureListener { cont.resume(null) }
        }

    // GET GOAL FOR SUBCATEGORY + MONTH/YEAR
    suspend fun getGoalBySubCategoryAndMonth(
        subCategoryId: String,
        month: Int,
        year: Int
    ): SubCategoryGoal? =
        suspendCoroutine { cont ->
            goalsRef.get()
                .addOnSuccessListener { snap ->
                    val match = snap.children.mapNotNull {
                        it.getValue(SubCategoryGoal::class.java)
                    }.firstOrNull {
                        it.subCategoryID == subCategoryId &&
                                it.month == month &&
                                it.year == year
                    }
                    cont.resume(match)
                }
                .addOnFailureListener { cont.resume(null) }
        }

    // GET ALL GOALS FOR CATEGORY + MONTH/YEAR
    suspend fun getGoalsByCategoryAndMonth(
        categoryId: String,
        month: Int,
        year: Int
    ): List<SubCategoryGoal> =
        suspendCoroutine { cont ->
            goalsRef.get()
                .addOnSuccessListener { snap ->
                    val list = snap.children.mapNotNull {
                        it.getValue(SubCategoryGoal::class.java)
                    }.filter {
                        it.categoryID == categoryId &&
                                it.month == month &&
                                it.year == year
                    }
                    cont.resume(list)
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
}
