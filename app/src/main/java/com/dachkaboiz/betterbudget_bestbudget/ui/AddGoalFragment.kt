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
import java.util.Calendar

class AddGoalFragment : Fragment(R.layout.fragment_add_goal) {

    private var categoryList: List<Category> = emptyList()
    private val firebaseCategoryRepository = FirebaseCategoryRepository()

    private lateinit var goalRepository: FirebaseCategoryGoalRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        goalRepository = FirebaseCategoryGoalRepository(uid)

        val spinner   = view.findViewById<Spinner>(R.id.spCategorySelector)
        val etMin     = view.findViewById<EditText>(R.id.etGoalMinAmount)
        val etMax     = view.findViewById<EditText>(R.id.etGoalMaxAmount)
        val btnAdd    = view.findViewById<Button>(R.id.btnAddGoal)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelGoal)

        // Load categories from Firebase
        firebaseCategoryRepository.getCategories(uid) { list ->
            categoryList = list
            val names = list.map { it.categoryName }

            requireActivity().runOnUiThread {
                spinner.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
            }
        }

        btnAdd.setOnClickListener {
            val selectedIndex = spinner.selectedItemPosition
            val maxText = etMax.text.toString().trim()
            val minText = etMin.text.toString().trim()

            if (selectedIndex == AdapterView.INVALID_POSITION || categoryList.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (maxText.isBlank()) {
                Toast.makeText(requireContext(), "Max goal is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = categoryList[selectedIndex]
            val cal = Calendar.getInstance()

            val goalId = goalRepository.generateGoalId()

            val goal = CategoryGoal(
                goalId = goalId,
                categoryId = category.firebaseId,   // correct Firebase link
                minGoal = minText.toDoubleOrNull(),
                maxGoal = maxText.toDoubleOrNull(),
                month = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR)
            )

            lifecycleScope.launch {
                goalRepository.insertGoal(goal)
                Toast.makeText(requireContext(), "Goal Added!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
