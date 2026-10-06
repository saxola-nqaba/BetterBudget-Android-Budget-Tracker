package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dachkaboiz.betterbudget_bestbudget.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var forgotPasswordTv: TextView
    private lateinit var signUpTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // UI
        emailInput = findViewById(R.id.etLIEmailAddress)
        passwordInput = findViewById(R.id.etLIPassword)
        loginBtn = findViewById(R.id.btnLogin)
        forgotPasswordTv = findViewById(R.id.tvForgotPassword)
        signUpTv = findViewById(R.id.tvRegister)

        // LOGIN BUTTON
        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // FORGOT PASSWORD
        forgotPasswordTv.setOnClickListener {
            showEmailDialog()
        }

        // REGISTER
        signUpTv.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val uid = auth.currentUser!!.uid

                    // Load user profile from Realtime Database
                    database.child("users").child(uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {

                                if (!snapshot.exists()) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "User profile not found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return
                                }

                                // Save email and UID locally
                                val prefs = getSharedPreferences("auth", MODE_PRIVATE)
                                prefs.edit()
                                    .putString("email", email)
                                    .putString("uid", uid)
                                    .apply()

                                AlertDialog.Builder(this@LoginActivity)
                                    .setTitle("Success")
                                    .setMessage("Login successful")
                                    .setPositiveButton("Continue") { _, _ ->
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        intent.putExtra("email", email)
                                        startActivity(intent)
                                    }
                                    .setCancelable(false)
                                    .show()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Database error: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })

                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Login failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun showEmailDialog() {
        val emailInput = EditText(this).apply {
            hint = "Enter your email"
            setPadding(40, 40, 40, 40)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Email Required")
            .setMessage("Please enter your email address")
            .setView(emailInput)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT)
                    .show()
            } else {
                auth.sendPasswordResetEmail(email)
                Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }
}
