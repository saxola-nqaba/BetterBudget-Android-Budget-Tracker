package com.dachkaboiz.betterbudget_bestbudget.data.repository

import android.util.Log
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseSubCategoryGoalRepository(private val uid: String) {
    // Reference to the subCategoryGoals node for this user
    private val goalsRef = FirebaseDatabase.getInstance().reference
        .child("users").child(uid).child("subCategoryGoals")

    /*
     Retrieves the goal associated with a specific subcategory.

     Queries the subCategoryGoals node by the subCategoryID field.
     Returns the first matching goal, or null if none exists.

     Requires ".indexOn": ["subCategoryID"] in Firebase Database Rules
     for this query to succeed without a permission/index error.

     @param subCategoryId the Firebase ID of the subcategory to look up
     @return the matching SubCategoryGoal, or null if not found
     */

    suspend fun getGoalForSubCategory(subCategoryId: String): SubCategoryGoal? =
        suspendCoroutine { cont ->
            goalsRef.orderByChild("subCategoryID")
                .equalTo(subCategoryId)
                .get()
                .addOnSuccessListener { snap ->
                    Log.d("GoalDebug", "Goal query for $subCategoryId: childrenCount=${snap.childrenCount}")
                    val goal = snap.children.firstOrNull()
                        ?.getValue(SubCategoryGoal::class.java)
                    Log.d("GoalDebug", "Goal found: $goal")
                    cont.resume(goal)
                }
                .addOnFailureListener { e->
                    Log.d("GoalDebug", "Goal query failed for $subCategoryId: ${e.message}")
                    cont.resume(null)
                }
        }

    /*
        Saves or overwrites a subcategory goal in Firebase.

        Uses the subCategoryID as the node key so each subcategory
        can only have one active goal at a time. Writing the same
        subcategory ID again will overwrite the previous goal.

        Failures are silently swallowed and the coroutine resumes
        normally to avoid crashing the caller.

        @param goal the SubCategoryGoal object to persist
     */
    suspend fun setGoal(goal: SubCategoryGoal) =
        suspendCoroutine<Unit> { cont ->
            goalsRef.child(goal.subCategoryID)
                .setValue(goal)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }
}