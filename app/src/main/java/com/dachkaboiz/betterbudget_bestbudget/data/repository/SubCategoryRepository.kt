package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SubCategoryRepository(private val uid: String) {

    private val db = FirebaseDatabase.getInstance().reference
    private val subCatRef = db.child("users").child(uid).child("subcategories")

    fun generateSubCategoryId(): String = subCatRef.push().key!!

    suspend fun insertSubCategory(sub: SubCategory) =
        suspendCoroutine<Unit> { cont ->
            subCatRef.child(sub.firebaseId)
                .setValue(sub)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

    suspend fun getSubCategories(categoryFirebaseId: String): List<SubCategory> =
        suspendCoroutine { cont ->

            subCatRef.get()                          // ← was `ref` (didn't exist)
                .addOnSuccessListener { snap ->
                    val list = snap.children.mapNotNull { child ->
                        child.getValue(SubCategory::class.java)
                            ?.copy(firebaseId = child.key ?: "")
                    }.filter { it.parentFirebaseId == categoryFirebaseId }  // ← was `parentFirebaseId` (not in scope)
                    cont.resume(list)
                }
                .addOnFailureListener {
                    cont.resume(emptyList())
                }
        }



    suspend fun getSubCategoryById(id: String): SubCategory? =
        suspendCoroutine { cont ->
            subCatRef.child(id)
                .get()
                .addOnSuccessListener { snap ->
                    cont.resume(snap.getValue(SubCategory::class.java))
                }
                .addOnFailureListener { cont.resume(null) }
        }

    suspend fun updateSubCategory(sub: SubCategory) =
        suspendCoroutine<Unit> { cont ->
            subCatRef.child(sub.firebaseId)
                .setValue(sub)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

    suspend fun deleteSubCategory(id: String) =
        suspendCoroutine<Unit> { cont ->
            subCatRef.child(id)
                .removeValue()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }
}


