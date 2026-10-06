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

class AddCategoryFragment : Fragment(R.layout.fragment_add_category) {

    private val repository = FirebaseCategoryRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val email = requireActivity()
            .getSharedPreferences("auth", 0)
            .getString("email", "") ?: ""

        val etName        = view.findViewById<EditText>(R.id.etCategoryName)
        val etIcon        = view.findViewById<EditText>(R.id.etCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etCategoryDescription)

        // Correct IDs matching fragment_add_category.xml
        // Previous code used etGoalMinAmount / etGoalMaxAmount which
        // do not exist in this layout — findViewById returned null,
        // causing the crash and silently skipping goal saves
        val etMinGoal = view.findViewById<EditText>(R.id.etCategoryMinGoal)
        val etMaxGoal = view.findViewById<EditText>(R.id.etCategoryMaxGoal)

        val btnCancel = view.findViewById<Button>(R.id.btnCategoryCancel)
        val btnAdd    = view.findViewById<Button>(R.id.btnCategoryAdd)

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnAdd.setOnClickListener {
            if (uid == null) {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name        = etName.text.toString().trim()
            val icon        = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()

            // Capture goal values synchronously here, before the async
            // Firebase call below. This is safe because the view is
            // guaranteed alive at this point in onClick.
            val minGoal = etMinGoal.text.toString().trim().toDoubleOrNull()
            val maxGoal = etMaxGoal.text.toString().trim().toDoubleOrNull()

            var hasError = false

            if (name.isEmpty()) {
                etName.error = "Category name is required"
                hasError = true
            }

            if (icon.isEmpty()) {
                etIcon.error = "Icon is required"
                hasError = true
            } else if (!isEmojiOnly(icon)) {
                etIcon.error = "Please enter only emojis"
                hasError = true
            }

            if (description.isEmpty()) {
                etDescription.error = "Description is required"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            val category = Category(
                userEmail           = email,
                categoryName        = name,
                categoryIcon        = icon,
                categoryDescription = description
            )

            repository.insertCategory(uid, category) { success, firebaseId ->

                // Guard: fragment may have detached by the time this
                // Firebase callback fires
                if (!isAdded) return@insertCategory

                requireActivity().runOnUiThread {

                    if (!isAdded) return@runOnUiThread

                    if (!success || firebaseId == null) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to add category",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@runOnUiThread
                    }

                    // Save goal if the user entered at least one value.
                    // minGoal / maxGoal were captured before the async
                    // call so they are safe to read here.
                    if (minGoal != null || maxGoal != null) {
                        val goalRepo = FirebaseCategoryGoalRepository(uid)
                        val goalId   = goalRepo.generateGoalId()
                        val cal      = Calendar.getInstance()

                        val goal = CategoryGoal(
                            goalId     = goalId,
                            categoryId = firebaseId,
                            minGoal    = minGoal,
                            maxGoal    = maxGoal,
                            month      = cal.get(Calendar.MONTH) + 1,
                            year       = cal.get(Calendar.YEAR)
                        )

                        lifecycleScope.launch {
                            goalRepo.insertGoal(goal)
                        }
                    }

                    Toast.makeText(
                        requireContext(),
                        "Category added!",
                        Toast.LENGTH_LONG
                    ).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun isEmojiOnly(text: String): Boolean {
        if (text.isEmpty()) return false
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val type      = Character.getType(codePoint)
            if (type != Character.SURROGATE.toInt() &&
                type != Character.OTHER_SYMBOL.toInt() &&
                type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt()
            ) {
                return false
            }
            i += Character.charCount(codePoint)
        }
        return true
    }
}