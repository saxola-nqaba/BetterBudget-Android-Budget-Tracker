package com.dachkaboiz.betterbudget_bestbudget.ui


import android.content.Intent
import android.os.Bundle
import android.widget.Spinner
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.User
import com.dachkaboiz.betterbudget_bestbudget.data.repository.UserRepository
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.UserViewModel
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.AuthState
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.UserViewModelFactory
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth


class RegisterActivity : BaseRegister() {

    private lateinit var signUpBtn: Button
    private lateinit var loginTv: TextView
    private lateinit var userViewModel: UserViewModel
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        val spinnerDay = findViewById<Spinner>(R.id.spDay)
        val spinnerMonth = findViewById<Spinner>(R.id.spMonth)
        val spinnerYear = findViewById<Spinner>(R.id.spYear)
        val dao = AppDatabase.getDatabase(this).userDao()
        val repository = UserRepository(dao)
        val factory = UserViewModelFactory(repository)
        userViewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]

// Use inherited functions

        setupYearSpinner(spinnerYear)
        spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val year = spinnerYear.selectedItem.toString().toIntOrNull()


                // Update months based on max date logic
                updateMonthSpinner(spinnerMonth, year)

                // Update days based on new month + year
                val monthIndex = spinnerMonth.selectedItemPosition
                updateDaysSpinner(spinnerDay, monthIndex, year)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

// 3. When MONTH changes → update DAYS
        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val year = spinnerYear.selectedItem.toString().toIntOrNull()


                updateDaysSpinner(spinnerDay, position, year)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        newPassword = findViewById(R.id.etNewPassword)
        confirmPassword = findViewById(R.id.etConfirmPassword)
        email = findViewById(R.id.etEmail)
        firstName = findViewById(R.id.etFirstName)
        lastName = findViewById(R.id.etLastName)
        signUpBtn = findViewById(R.id.btnSignUp)
        loginTv = findViewById(R.id.tvLogin)

        passwordRequirementsContainer = findViewById(R.id.passwordRequirementsContainer)
        pRequirementsView = layoutInflater.inflate(
            R.layout.dialog_password_requirements,
            passwordRequirementsContainer,
            true
        )
        pConfirmContainer = findViewById(R.id.passwordConfirmContainer)
        pConfirmView = layoutInflater.inflate(
            R.layout.dialog_confirm_password,
            pConfirmContainer,
            true
        )


        initPasswordViews(
            newPassword,
            confirmPassword,
            passwordRequirementsContainer,
            pRequirementsView,
            pConfirmContainer
        )

        loginTv.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signUpBtn.setOnClickListener {

            val email = email.text.toString().trim()
            val password = newPassword.text.toString().trim()
            val confirm = confirmPassword.text.toString().trim()
            val firstName = firstName.text.toString().trim().ifEmpty { null }
            val lastName = lastName.text.toString().trim().ifEmpty { null }
            val year = spinnerYear.selectedItem.toString().toIntOrNull()
            val month = spinnerMonth.selectedItemPosition.takeIf { it > 0 }?.let { it }
            val day = spinnerDay.selectedItem.toString().toIntOrNull()


            // -------------------------
            // VALIDATION
            // -------------------------
            if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var age: Int? = null
            var birthDateMillis: Long? = null

            if (year != null && month != null && day != null) {
                age = calculateAge(day, month, year)
                birthDateMillis = LocalDate.of(year, month, day)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            }

            // -------------------------
            // FIREBASE REGISTRATION
            // -------------------------
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val uid = auth.currentUser!!.uid

                        val userData = hashMapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email,
                            "birthDate" to birthDateMillis,
                            "age" to age
                        )

                        database.child("users")
                            .child(uid)
                            .setValue(userData)
                            .addOnSuccessListener {
                                AlertDialog.Builder(this)
                                    .setTitle("Account Created")
                                    .setMessage("Your account has been created successfully")
                                    .setPositiveButton("Continue") { _, _ ->
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                            .addOnSuccessListener {

                                AlertDialog.Builder(this)
                                    .setTitle("Account Created")
                                    .setMessage("Your account has been created successfully")
                                    .setPositiveButton("Continue") { _, _ ->
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Registration failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }



        }
        observeRegisterState()
    }
    fun calculateAge(day: Int, month: Int, year: Int): Int {
        val birthDate = LocalDate.of(year, month, day)
        val today = LocalDate.now()
        return Period.between(birthDate, today).years
    }
    private fun observeRegisterState() {
        lifecycleScope.launchWhenStarted {
            userViewModel.registerState.collect { state ->
                when (state) {

                    is AuthState.Success -> {
                        AlertDialog.Builder(this@RegisterActivity)
                            .setTitle("Account Created")
                            .setMessage("Your account has been created successfully")
                            .setPositiveButton("Continue") { _, _ ->
                                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    }

                    is AuthState.Error -> {
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_SHORT).show()
                    }

                    else -> Unit
                }
            }
        }
    }
}
