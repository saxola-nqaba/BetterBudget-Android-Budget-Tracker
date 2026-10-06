package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class DeleteSubCategoryFragment(
    private val parentFirebaseId: String,
    private val subFirebaseId: String
) : Fragment(R.layout.fragment_delete_subcategory) {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val userRef = FirebaseDatabase.getInstance().reference
        .child("users")
        .child(uid ?: "")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvIcon        = view.findViewById<TextView>(R.id.tvDeleteSubIcon)
        val tvName        = view.findViewById<TextView>(R.id.tvDeleteSubName)
        val tvParent      = view.findViewById<TextView>(R.id.tvDeleteSubParentCategory)
        val tvDescription = view.findViewById<TextView>(R.id.tvDeleteSubDescription)
        val tvMinGoal     = view.findViewById<TextView>(R.id.tvDeleteSubMinGoal)
        val tvMaxGoal     = view.findViewById<TextView>(R.id.tvDeleteSubMaxGoal)
        val btnConfirm    = view.findViewById<Button>(R.id.btnDeleteSubConfirm)
        val btnCancel     = view.findViewById<Button>(R.id.btnDeleteSubCancel)

        // Load subcategory
        userRef.child("subcategories").child(subFirebaseId)
            .get()
            .addOnSuccessListener { snap ->
                val sub = snap.getValue(SubCategory::class.java)

                tvIcon.text        = sub?.subCategoryIcon ?: ""
                tvName.text        = sub?.subCategoryName ?: ""
                tvDescription.text = "Description: ${sub?.subCategoryDescription ?: "—"}"
            }

        // Load parent category name
        userRef.child("categories").child(parentFirebaseId)
            .get()
            .addOnSuccessListener { snap ->
                val name = snap.child("categoryName").value?.toString() ?: "—"
                tvParent.text = "Parent Category: $name"
            }

        // Load subcategory goal
        val cal = Calendar.getInstance()
        val monthKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"

        userRef.child("subCategoryGoals")
            .child(parentFirebaseId)
            .child(monthKey)
            .child(subFirebaseId)
            .get()
            .addOnSuccessListener { snap ->
                val min = snap.child("minGoal").value?.toString()
                val max = snap.child("maxGoal").value?.toString()

                tvMinGoal.text = "Min Goal: ${min?.let { "R $it" } ?: "—"}"
                tvMaxGoal.text = "Max Goal: ${max?.let { "R $it" } ?: "—"}"
            }

        // Confirm delete
        btnConfirm.setOnClickListener {
            // Check if subcategory has expenses
//            userRef.child("expenses")
//                .orderByChild("subCategoryID")
//                .equalTo(subFirebaseId)
//                .get()
//                .addOnSuccessListener { expSnap ->

//                    if (expSnap.exists()) {
//                        Toast.makeText(requireContext(), "Cannot delete: this subcategory has expenses.", Toast.LENGTH_LONG).show()
//                        return@addOnSuccessListener
//                    }

                    // Delete subcategory
                    userRef.child("subcategories").child(subFirebaseId).removeValue()

                    // Delete goal
                    userRef.child("subCategoryGoals")
                        .child(parentFirebaseId)
                        .child(monthKey)
                        .child(subFirebaseId)
                        .removeValue()

                    Toast.makeText(requireContext(), "Subcategory deleted", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }


        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
    }
}

