package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ExpenseRepository(
    private val uid: String
) {

    private val db = FirebaseDatabase.getInstance().reference
    private val expensesRef = db.child("users").child(uid).child("expenses")
    fun generateExpenseId(): String = expensesRef.push().key!!

    suspend fun insertExpense(expense: Expense) = suspendCoroutine<Unit> { cont ->
        expensesRef.child(expense.expenseID)
            .setValue(expense)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resume(Unit) }
    }
    suspend fun getExpenseById(expenseId: String): Expense? = suspendCoroutine { cont ->
        expensesRef
            .child(expenseId)
            .get()
            .addOnSuccessListener { snap ->
                cont.resume(snap.getValue(Expense::class.java))
            }
            .addOnFailureListener {
                cont.resume(null)
            }
    }
    suspend fun getExpensesByUser(email: String): List<Expense> =
        suspendCoroutine { cont ->
            expensesRef.get()
                .addOnSuccessListener { snap ->
                    cont.resume(snap.children.mapNotNull { it.getValue(Expense::class.java) })
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }



    suspend fun updateExpense(expense: Expense) = suspendCoroutine<Unit> { cont ->
        expensesRef
            .child(expense.expenseID)
            .setValue(expense)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resume(Unit) }
    }


    suspend fun getExpensesByCategory(categoryId: String): List<Expense> =
        suspendCoroutine { cont ->
            expensesRef.get()
                .addOnSuccessListener { snap ->
                    val list = snap.children
                        .mapNotNull { it.getValue(Expense::class.java) }
                        .filter { it.categoryId == categoryId }
                    cont.resume(list)
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

    suspend fun getExpensesBySubCategory(subCategoryId: String): List<Expense> =
        suspendCoroutine { cont ->
            expensesRef.get()
                .addOnSuccessListener { snap ->
                    val list = snap.children
                        .mapNotNull { it.getValue(Expense::class.java) }
                        .filter { it.subCategoryId == subCategoryId }
                    cont.resume(list)
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }


    suspend fun deleteExpense(expense: Expense) =
        suspendCoroutine<Unit> { cont ->
            expensesRef.child(expense.expenseID)
                .removeValue()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

}

