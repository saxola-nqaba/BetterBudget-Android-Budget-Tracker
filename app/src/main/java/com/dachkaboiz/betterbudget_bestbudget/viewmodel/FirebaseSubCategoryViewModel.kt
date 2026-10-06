package com.dachkaboiz.betterbudget_bestbudget.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseSubCategoryGoalRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FirebaseSubCategoryViewModel : ViewModel() {

    private val _subCategories = MutableStateFlow<List<SubCategory>>(emptyList())
    val subCategories: StateFlow<List<SubCategory>> = _subCategories

    private val _subCategoriesWithGoals =
        MutableStateFlow<List<Triple<SubCategory, SubCategoryGoal?, List<Any>>>>(emptyList())
    val subCategoriesWithGoals: StateFlow<List<Triple<SubCategory, SubCategoryGoal?, List<Any>>>> =
        _subCategoriesWithGoals

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val userRef = FirebaseDatabase.getInstance().reference
        .child("users")
        .child(uid ?: "")

    fun loadSubCategories(parentFirebaseId: String) {
        viewModelScope.launch {
            userRef.child("subcategories")
                .get()
                .addOnSuccessListener { snap ->
                    Log.d("SubCatDebug", "Total children: ${snap.childrenCount}")
                    snap.children.forEach { child ->
                        Log.d("SubCatDebug", "key=${child.key} value=${child.value}")
                    }
                    userRef.get().addOnSuccessListener { snap ->
                        Log.d("SubCatDebug", "User root keys: ${snap.children.map { it.key }}")
                    }
                    Log.d("SubCatDebug", "Querying path: ${userRef.child("subcategories").toString()}")
                    val list = snap.children.mapNotNull { child ->
                        val sub = child.getValue(SubCategory::class.java)
                        sub?.copy(firebaseId = child.key!!)
                    }.filter { it.parentFirebaseId == parentFirebaseId }
                    Log.d(
                        "SubCatDebug",
                        "Filtered list size: ${list.size} for parent: $parentFirebaseId"
                    )

                    _subCategories.value = list

                    //Loading goals for each subcategory
                    if (uid != null) {
                        val goalRepository = FirebaseSubCategoryGoalRepository(uid)
                        viewModelScope.launch {
                            val withGoals = list.map { sub ->
                                val goal = goalRepository.getGoalForSubCategory(sub.firebaseId)
                                Triple(sub, goal, emptyList<Any>())
                            }
                            _subCategoriesWithGoals.value = withGoals
                        }
                    }

                }
        }
    }
}
