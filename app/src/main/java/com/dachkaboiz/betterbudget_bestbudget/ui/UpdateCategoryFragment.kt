package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class UpdateCategoryFragment(
    private val firebaseId: String
) : Fragment(R.layout.fragment_edit_category) {

    private val repository = FirebaseCategoryRepository()
    private var currentCategory: Category? = null
    private var currentGoal: CategoryGoal? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val etName        = view.findViewById<EditText>(R.id.etEditCategoryName)
        val etIcon        = view.findViewById<EditText>(R.id.etEditCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etEditCategoryDescription)

        val etMinGoal     = view.findViewById<EditText>(R.id.etEditCategoryMinGoal)
        val etMaxGoal     = view.findViewById<EditText>(R.id.etEditCategoryMaxGoal)

        val btnUpdate     = view.findViewById<Button>(R.id.btnEditCategoryUpdate)
        val btnCancel     = view.findViewById<Button>(R.id.btnEditCategoryCancel)

        val goalRepo = FirebaseCategoryGoalRepository(uid)

        // -----------------------------
        // LOAD CATEGORY
        // -----------------------------
        repository.getCategoryById(uid, firebaseId) { cat ->
            currentCategory = cat
            requireActivity().runOnUiThread {
                cat?.let {
                    etName.setText(it.categoryName)
                    etIcon.setText(it.categoryIcon)
                    etDescription.setText(it.categoryDescription)
                }
            }
        }

        // -----------------------------
        // LOAD EXISTING GOAL
        // -----------------------------
        lifecycleScope.launch {
            val allGoals = goalRepo.getAllGoals()
            currentGoal = allGoals.firstOrNull { it.categoryId == firebaseId }

            currentGoal?.let { goal ->
                etMinGoal.setText(goal.minGoal?.toString() ?: "")
                etMaxGoal.setText(goal.maxGoal?.toString() ?: "")
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnUpdate.setOnClickListener {

            val name        = etName.text.toString().trim()
            val icon        = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }

            if (icon.isEmpty()) {
                etIcon.error = "Icon is required"
                return@setOnClickListener
            }

            if (description.isEmpty()) {
                etDescription.error = "Description is required"
                return@setOnClickListener
            }

            val updatedCategory = currentCategory?.copy(
                categoryName = name,
                categoryIcon = icon,
                categoryDescription = description
            ) ?: return@setOnClickListener

            // -----------------------------
            // UPDATE CATEGORY
            // -----------------------------
            repository.updateCategory(uid, updatedCategory) { success ->
                requireActivity().runOnUiThread {
                    if (!success) {
                        Toast.makeText(requireContext(), "Failed to update category", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    // -----------------------------
                    // UPDATE OR CREATE GOAL
                    // -----------------------------
                    val minGoal = etMinGoal.text.toString().trim().toDoubleOrNull()
                    val maxGoal = etMaxGoal.text.toString().trim().toDoubleOrNull()

                    lifecycleScope.launch {
                        if (minGoal != null || maxGoal != null) {

                            val cal = Calendar.getInstance()

                            if (currentGoal == null) {
                                // CREATE NEW GOAL
                                val newGoal = CategoryGoal(
                                    goalId = goalRepo.generateGoalId(),
                                    categoryId = firebaseId,
                                    minGoal = minGoal,
                                    maxGoal = maxGoal,
                                    month = cal.get(Calendar.MONTH) + 1,
                                    year = cal.get(Calendar.YEAR)
                                )
                                goalRepo.insertGoal(newGoal)

                            } else {
                                // UPDATE EXISTING GOAL
                                val updatedGoal = currentGoal!!.copy(
                                    minGoal = minGoal,
                                    maxGoal = maxGoal
                                )
                                goalRepo.updateGoal(updatedGoal)
                            }
                        }
                    }

                    Toast.makeText(requireContext(), "Category updated!", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }
}
