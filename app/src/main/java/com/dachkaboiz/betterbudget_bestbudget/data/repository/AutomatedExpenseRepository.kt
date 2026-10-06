package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.AutomatedExpense
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutomatedExpenseRepository(private val uid: String) {

    private val ref = FirebaseDatabase.getInstance().reference
        .child("users").child(uid).child("automatedExpenses")

    fun generateId(): String = ref.push().key!!

    fun insertAutomatedExpense(
        expense: AutomatedExpense,
        callback: (AutomatedExpense?) -> Unit
    ) {
        val key = generateId()
        val finalExpense = expense.copy(firebaseId = key)

        ref.child(key).setValue(finalExpense)
            .addOnSuccessListener { callback(finalExpense) }
            .addOnFailureListener { callback(null) }
    }

    suspend fun getAll(): List<AutomatedExpense> = suspendCoroutine { cont ->
        ref.get()
            .addOnSuccessListener { snap ->
                val list = snap.children.mapNotNull { child ->
                    child.getValue(AutomatedExpense::class.java)
                        ?.copy(firebaseId = child.key ?: "")
                }
                cont.resume(list)
            }
            .addOnFailureListener { cont.resume(emptyList()) }
    }

    suspend fun update(expense: AutomatedExpense) = suspendCoroutine<Unit> { cont ->
        ref.child(expense.firebaseId).setValue(expense)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resume(Unit) }
    }
}
