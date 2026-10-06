package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class AddSubCategoryFragment(private val parentFirebaseId: String)
    : Fragment(R.layout.fragment_add_subcategory) {

    private val firebaseCategoryRepository = FirebaseCategoryRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val tvCatName     = view.findViewById<TextView>(R.id.tvParentCategory)
        val etName        = view.findViewById<EditText>(R.id.etSubcategoryName)
        val etIcon        = view.findViewById<EditText>(R.id.etSubCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etSubDescription)
        val etMinGoal     = view.findViewById<EditText>(R.id.etSubMinGoal)
        val etMaxGoal     = view.findViewById<EditText>(R.id.etSubMaxGoal)
        val btnAdd        = view.findViewById<Button>(R.id.btnSubAdd)
        val btnCancel     = view.findViewById<Button>(R.id.btnSubCancel)

        // Load parent category name
        firebaseCategoryRepository.getCategoryById(uid, parentFirebaseId) { cat ->
            requireActivity().runOnUiThread {
                tvCatName.text = cat?.categoryName ?: ""
            }
        }

        btnAdd.setOnClickListener {
            val name        = etName.text.toString().trim()
            val icon        = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val minGoal     = etMinGoal.text.toString().toDoubleOrNull()
            val maxGoal     = etMaxGoal.text.toString().toDoubleOrNull()

            if (name.isEmpty()) { etName.error = "Name is required"; return@setOnClickListener }
            if (icon.isBlank()) { etIcon.error = "Icon is required"; return@setOnClickListener }

            // Validate min/max
            if (minGoal != null && maxGoal != null && minGoal > maxGoal) {
                Toast.makeText(requireContext(), "Min goal cannot exceed max goal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(uid)

            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val monthKey = "$year-$month"

            // 1️⃣ Load category goal
            userRef.child("categoryGoals")
                .child(parentFirebaseId)
                .child(monthKey)
                .get()
                .addOnSuccessListener { catGoalSnap ->

                    val catMinGoal = catGoalSnap.child("minGoal").getValue(Double::class.java) ?: 0.0
                    val catMaxGoal = catGoalSnap.child("maxGoal").getValue(Double::class.java) ?: 0.0

                    // 2️⃣ Load existing subcategory goals
                    userRef.child("subCategoryGoals")
                        .child(parentFirebaseId)
                        .child(monthKey)
                        .get()
                        .addOnSuccessListener { subGoalsSnap ->

                            var totalMin = 0.0
                            var totalMax = 0.0

                            for (child in subGoalsSnap.children) {
                                totalMin += child.child("minGoal").getValue(Double::class.java) ?: 0.0
                                totalMax += child.child("maxGoal").getValue(Double::class.java) ?: 0.0
                            }

                            val safeMin = minGoal ?: 0.0
                            val safeMax = maxGoal ?: 0.0

                            val categoryHasGoal = (catMinGoal > 0 || catMaxGoal > 0)

                            if (categoryHasGoal) {

                                val exceedsMin = catMinGoal < (totalMin + safeMin)
                                val exceedsMax = catMaxGoal < (totalMax + safeMax)

                                if (exceedsMin && exceedsMax) {
                                    Toast.makeText(requireContext(),
                                        "Both min and max goals exceed the category limits",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnSuccessListener
                                }

                                if (exceedsMin) {
                                    Toast.makeText(requireContext(),
                                        "Min goal exceeds the category's remaining min goal",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnSuccessListener
                                }

                                if (exceedsMax) {
                                    Toast.makeText(requireContext(),
                                        "Max goal exceeds the category's remaining max goal",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnSuccessListener
                                }
                            }



                            // 3️⃣ Create subcategory
                            val subCatId = userRef.child("subcategories").push().key!!
                            val newSubCategory = mapOf(
                                "parentFirebaseId" to parentFirebaseId,
                                "subCategoryName" to name,
                                "subCategoryIcon" to icon,
                                "subCategoryDescription" to description
                            )

                            userRef.child("subcategories").child(subCatId)
                                .setValue(newSubCategory)
                                .addOnSuccessListener {

                                    // 4️⃣ Create subcategory goal (if provided)
                                    if (minGoal != null || maxGoal != null) {
                                        val goal = mapOf(
                                            "subCategoryID" to subCatId,
                                            "categoryID" to parentFirebaseId,
                                            "minGoal" to minGoal,
                                            "maxGoal" to maxGoal,
                                            "month" to month,
                                            "year" to year
                                        )

                                        userRef.child("subCategoryGoals")
                                            .child(parentFirebaseId)
                                            .child(monthKey)
                                            .child(subCatId)
                                            .setValue(goal)
                                    }

                                    Toast.makeText(requireContext(), "Subcategory added!", Toast.LENGTH_SHORT).show()
                                    parentFragmentManager.popBackStack()
                                }
                        }
                }
        }

        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
    }
}

