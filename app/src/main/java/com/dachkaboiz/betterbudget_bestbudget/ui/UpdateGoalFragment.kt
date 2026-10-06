package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UpdateGoalFragment : Fragment(R.layout.fragment_update_goal) {

    private lateinit var goalRepository: FirebaseCategoryGoalRepository
    private val categoryRepository = FirebaseCategoryRepository()

    private var currentGoal: CategoryGoal? = null
    private var categoryCache: List<Category> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        goalRepository = FirebaseCategoryGoalRepository(uid)

        val goalId = arguments?.getString("goalId") ?: ""

        val etMin     = view.findViewById<EditText>(R.id.etUpdateGoalMin)
        val etMax     = view.findViewById<EditText>(R.id.etUpdateGoalMax)
        val tvCategory = view.findViewById<TextView>(R.id.spCategorySelector)
        val btnUpdate = view.findViewById<Button>(R.id.btnUpdateGoal)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelUpdateGoal)

        // Load categories so we can show category name
        categoryRepository.getCategories(uid) { list ->
            categoryCache = list
        }

        // Load goal from Firebase
        lifecycleScope.launch {
            currentGoal = goalRepository.getGoalById(goalId)
            currentGoal?.let { goal ->
                val categoryName = categoryCache.find { it.firebaseId == goal.categoryId }?.categoryName
                    ?: "Category"

                //tvCategory.text = categoryName
                etMin.setText(goal.minGoal?.toString() ?: "")
                etMax.setText(goal.maxGoal?.toString() ?: "")
            }
        }

        btnUpdate.setOnClickListener {
            val maxText = etMax.text.toString().trim()
            val minText = etMin.text.toString().trim()

            if (maxText.isBlank()) {
                Toast.makeText(requireContext(), "Max goal is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentGoal?.let { goal ->
                val updatedGoal = goal.copy(
                    minGoal = minText.toDoubleOrNull(),
                    maxGoal = maxText.toDoubleOrNull()
                )

                lifecycleScope.launch {
                    goalRepository.updateGoal(updatedGoal)
                    Toast.makeText(requireContext(), "Goal Updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
