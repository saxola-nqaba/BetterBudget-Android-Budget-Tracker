package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth

class DeleteCategoryFragment(
    private val firebaseId: String  // was: categoryID: Int
) : Fragment(R.layout.fragment_delete_category) {

    private val repository = FirebaseCategoryRepository()
    private var targetCategory: Category? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val tvIcon        = view.findViewById<TextView>(R.id.tvDeleteCategoryIcon)
        val tvName        = view.findViewById<TextView>(R.id.tvDeleteCategoryName)
        val tvDescription = view.findViewById<TextView>(R.id.tvDeleteCategoryDescription)
        val tvMinGoal     = view.findViewById<TextView>(R.id.tvDeleteCategoryMinGoal)
        val tvMaxGoal     = view.findViewById<TextView>(R.id.tvDeleteCategoryMaxGoal)
        val btnConfirm    = view.findViewById<Button>(R.id.btnDeleteCategoryConfirm)
        val btnCancel     = view.findViewById<Button>(R.id.btnDeleteCategoryCancel)

        // Load category details for the summary card
        if (uid != null) {
            repository.getCategoryById(uid, firebaseId) { cat ->
                targetCategory = cat
                requireActivity().runOnUiThread {
                    cat?.let {
                        tvIcon.text = it.categoryIcon
                        tvName.text = it.categoryName
                        tvDescription.text = "Description: ${it.categoryDescription}"
                    }
                    // Goals display — CategoryGoal owner will wire this up
                    tvMinGoal.text = "Min Goal: —"
                    tvMaxGoal.text = "Max Goal: —"
                }
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnConfirm.setOnClickListener {
            if (uid == null) {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Expense check — temporarily removed since expenses
            // still use Room integer IDs. Expense owner will
            // restore this check when they migrate expenses.
            // TODO: restore expense check after expense migration

            repository.deleteCategory(uid, firebaseId) { success ->
                requireActivity().runOnUiThread {
                    if (success) {
                        Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete category", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}