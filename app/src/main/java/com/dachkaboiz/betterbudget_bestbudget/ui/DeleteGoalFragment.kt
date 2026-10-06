package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
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

class DeleteGoalFragment : Fragment(R.layout.fragment_delete_goal) {

    private lateinit var goalRepo: FirebaseCategoryGoalRepository
    private val categoryRepo = FirebaseCategoryRepository()

    private var targetGoal: CategoryGoal? = null
    private var categoryCache: List<Category> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        goalRepo = FirebaseCategoryGoalRepository(uid)

        val goalId = arguments?.getString("goalId") ?: ""

        val tvCategory = view.findViewById<TextView>(R.id.tvDeleteGoalCategory)
        val tvMax      = view.findViewById<TextView>(R.id.tvDeleteGoalMax)
        val tvMin      = view.findViewById<TextView>(R.id.tvDeleteGoalMin)
        val tvPeriod   = view.findViewById<TextView>(R.id.tvDeleteGoalPeriod)
        val btnConfirm = view.findViewById<Button>(R.id.btnDeleteGoalConfirm)
        val btnCancel  = view.findViewById<Button>(R.id.btnDeleteGoalCancel)

        // Load categories first
        categoryRepo.getCategories(uid) { list ->
            categoryCache = list
        }

        // Load goal from Firebase
        lifecycleScope.launch {
            targetGoal = goalRepo.getGoalById(goalId)

            targetGoal?.let { goal ->
                val category = categoryCache.find { it.firebaseId == goal.categoryId }

                tvCategory.text = category?.categoryName ?: "Unknown Category"
                tvMax.text      = "Limit: R${String.format("%.2f", goal.maxGoal ?: 0.0)}"
                tvMin.text      = "Min Target: R${String.format("%.2f", goal.minGoal ?: 0.0)}"
                tvPeriod.text   = "Period: ${goal.month} - ${goal.year}"
            }
        }

        btnConfirm.setOnClickListener {
            targetGoal?.let { goal ->
                lifecycleScope.launch {
                    goalRepo.deleteGoal(goal.goalId)
                    Toast.makeText(requireContext(), "Goal deleted successfully", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
