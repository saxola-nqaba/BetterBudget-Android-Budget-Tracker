package com.dachkaboiz.betterbudget_bestbudget.data.repository

import android.util.Log
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseCategoryRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // INSERT

    fun insertCategory(
        uid: String,
        category: Category,
        callback: (Boolean, String?) -> Unit
    ) {
        val catRef = db.child("users").child(uid).child("categories")
        val newId = catRef.push().key

        if (newId == null) {
            callback(false, null)
            return
        }

        val categoryWithId = category.copy(firebaseId = newId)

        catRef.child(newId)
            .setValue(categoryWithId)
            .addOnSuccessListener { callback(true, newId) }
            .addOnFailureListener { callback(false, null) }
    }

    fun getCategories(uid: String, callback: (List<Category>) -> Unit) {
        db.child("users").child(uid).child("categories")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.children.mapNotNull { it.getValue(Category::class.java) }
                callback(list)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    // GET SINGLE BY FIREBASE ID
    fun getCategoryById(uid: String, firebaseId: String, onResult: (Category?) -> Unit) {
        db.child("users").child(uid).child("categories").child(firebaseId)

            // Code Attribution
            // This method was adapted from Stack Overflow
            // https://stackoverflow.com/questions/32886546/how-to-get-single-value-from-firebase-database-android
            // Frank van Puffelen
            // https://stackoverflow.com/users/209103/frank-van-puffelen

            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val category = snapshot.getValue(Category::class.java)
                    Log.d("CategoryDebug", "getCategoryById result: $category")
                    onResult(category)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.d("CategoryDebug", "getCategoryById cancelled: ${error.message}")
                    onResult(null)
                }
            })
    }

    // UPDATE
    fun updateCategory(uid: String, category: Category, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("categories").child(category.firebaseId)
            .setValue(category)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // DELETE
    fun deleteCategory(uid: String, firebaseId: String, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("categories").child(firebaseId)
            .removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
    suspend fun getCategoriesSuspend(uid: String): List<Category> =
        suspendCoroutine { cont ->
            getCategories(uid) { list -> cont.resume(list) }
        }

}