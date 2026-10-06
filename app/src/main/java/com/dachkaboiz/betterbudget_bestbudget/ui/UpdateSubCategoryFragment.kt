package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dachkaboiz.betterbudget_bestbudget.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class UpdateSubCategoryFragment(
    private val parentFirebaseId: String,
    private val subFirebaseId: String
) : Fragment(R.layout.fragment_edit_subcategory) {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val userRef get() = FirebaseDatabase.getInstance().reference.child("users").child(uid!!)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val primaryName  = view.findViewById<TextView>(R.id.tvEditParentCategory)
        val etName       = view.findViewById<EditText>(R.id.etEditSubcategoryName)
        val etIcon       = view.findViewById<EditText>(R.id.etEditSubCategoryIcon)
        val etDescription= view.findViewById<EditText>(R.id.etEditSubDescription)
        val etMinGoal    = view.findViewById<EditText>(R.id.etEditSubMinGoal)
        val etMaxGoal    = view.findViewById<EditText>(R.id.etEditSubMaxGoal)
        val btnUpdate    = view.findViewById<Button>(R.id.btnEditSubUpdate)
        val btnCancel    = view.findViewById<Button>(R.id.btnEditSubCancel)

        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val monthKey = "$year-$month"

        // Load parent category name
        userRef.child("categories").child(parentFirebaseId)
            .get()
            .addOnSuccessListener { snap ->
                primaryName.text = snap.child("categoryName").value?.toString() ?: ""
            }

        // 2️⃣ Load subcategory data
        userRef.child("subcategories").child(subFirebaseId)
            .get()
            .addOnSuccessListener { snap ->
                etName.setText(snap.child("subCategoryName").value?.toString() ?: "")
                etIcon.setText(snap.child("subCategoryIcon").value?.toString() ?: "")
                etDescription.setText(snap.child("subCategoryDescription").value?.toString() ?: "")
            }

        // 3️⃣ Load existing subcategory goal (if any)
        userRef.child("subCategoryGoals")
            .child(parentFirebaseId)
            .child(monthKey)
            .child(subFirebaseId)
            .get()
            .addOnSuccessListener { snap ->
                etMinGoal.setText(snap.child("minGoal").value?.toString() ?: "")
                etMaxGoal.setText(snap.child("maxGoal").value?.toString() ?: "")
            }

        // 4️⃣ Update button
        btnUpdate.setOnClickListener {
            val name = etName.text.toString().trim()
            val icon = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val minGoal = etMinGoal.text.toString().toDoubleOrNull()
            val maxGoal = etMaxGoal.text.toString().toDoubleOrNull()

            if (name.isEmpty()) {
                etName.error = "Name is required"; return@setOnClickListener
            }
            if (icon.isBlank()) {
                etIcon.error = "Icon is required"; return@setOnClickListener
            }


            // 5️⃣ Validate against category goal
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
                                if(child.child("categoryID").value?.toString() != parentFirebaseId) {
                                    totalMin += child.child("minGoal").getValue(Double::class.java)
                                        ?: 0.0
                                    totalMax += child.child("maxGoal").getValue(Double::class.java)
                                        ?: 0.0
                                }
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



                            // 6️⃣ Update subcategory
                            val updatedSub = mapOf(
                                "subCategoryName" to name,
                                "subCategoryIcon" to icon,
                                "subCategoryDescription" to description
                            )

                            userRef.child("subcategories").child(subFirebaseId)
                                .updateChildren(updatedSub)

                            // 7️⃣ Add or Update subcategory goal
                            val updatedGoal = mapOf(
                                "minGoal" to minGoal,
                                "maxGoal" to maxGoal,
                                "month" to month,
                                "year" to year,
                                "categoryID" to parentFirebaseId,
                                "subCategoryID" to subFirebaseId
                            )

                            userRef.child("subCategoryGoals")
                                .child(parentFirebaseId)
                                .child(monthKey)
                                .child(subFirebaseId)
                                .setValue(updatedGoal)

                            Toast.makeText(requireContext(), "Subcategory updated!", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                }
        }





        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
    }
}
