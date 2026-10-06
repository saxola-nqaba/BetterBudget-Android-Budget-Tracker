package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase.Companion.getDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.User
import com.dachkaboiz.betterbudget_bestbudget.data.repository.UserRepository
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.UserViewModel
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

class DeleteProfileFragment : Fragment(R.layout.fragment_delete_profile) {

    private lateinit var userViewModel: UserViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel setup
        val dao = AppDatabase.getDatabase(requireContext()).userDao()
        val repository = UserRepository(dao)
        val factory = UserViewModelFactory(repository)
        userViewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]

        // SharedPrefs (logged-in email)
        val prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)

        // Bind views
        val tvName = view.findViewById<TextView>(R.id.tvDeleteProfileName)
        val tvEmail = view.findViewById<TextView>(R.id.tvProfileDeleteEmail)
        val tvAge = view.findViewById<TextView>(R.id.tvProfileDeleteAge)
        val btnDelete = view.findViewById<Button>(R.id.btnDeleteProfileConfirm)
        val btnCancel = view.findViewById<Button>(R.id.btnDeleteProfileCancel)

        // -----------------------------
        // 1. Load Data for Summary Card
        // -----------------------------
        if (email != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val user: User? = userViewModel.getUserByEmail(email)

                if (user != null) {
                    tvName.text = "Name — ${user.firstName ?: ""} ${user.surname ?: ""}"
                    tvEmail.text = "EMAIL — ${user.email}"
                    tvAge.text = "AGE — ${user.age ?: ""}"

                }
            }
        }

        // -----------------------------
        // 2. Delete Logic
        // -----------------------------
        btnDelete.setOnClickListener {
            val userEmail = email ?: return@setOnClickListener

            // Delete user
            userViewModel.deleteUser(userEmail)

            // Clear saved login
            prefs.edit().remove("email").apply()

            // Navigate back to login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Cancel button
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
