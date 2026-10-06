package com.dachkaboiz.betterbudget_bestbudget.ui

import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import android.view.View
import android.widget.*
import android.widget.FrameLayout
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import android.util.Patterns
import com.dachkaboiz.betterbudget_bestbudget.R
import com.google.firebase.auth.FirebaseAuth
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan



open class BaseRegister : AppCompatActivity() {
    val today = Calendar.getInstance()
    val thisYear = today.get(Calendar.YEAR)
    val thisMonth = today.get(Calendar.MONTH)
    val thisDay = today.get(Calendar.DAY_OF_MONTH)
    protected lateinit var newPassword: EditText
    protected lateinit var email: EditText
    protected lateinit var firstName: EditText
    protected lateinit var auth: FirebaseAuth

    protected lateinit var lastName: EditText

    protected lateinit var confirmPassword: EditText
    protected lateinit var passwordRequirementsContainer: FrameLayout
    protected lateinit var pConfirmContainer: FrameLayout
    protected lateinit var pConfirmView: View
    protected lateinit var pRequirementsView: View



    protected fun setupYearSpinner(spinner: Spinner) {
        val years = mutableListOf("YYYY")
        years.addAll((1930..thisYear).map { it.toString() })

        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
    }


    protected fun updateMonthSpinner(spinner: Spinner, year: Int?) {

        val months = mutableListOf("MM")

        if (year != null && year == thisYear) {
            months.addAll(
                listOf("January","February","March","April","May","June",
                    "July","August","September","October","November","December"
                ).take(thisMonth + 1)
            )
        } else {
            months.addAll(
                listOf("January","February","March","April","May","June",
                    "July","August","September","October","November","December")
            )
        }

        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
    }



    protected fun updateDaysSpinner(spinner: Spinner, monthIndex: Int?, year: Int?) {

        val days = mutableListOf("DD")
        var daysInMonth: Int = 31

        if (monthIndex != null && year != null && monthIndex > 0) {
            val month = monthIndex+1
            daysInMonth = when (month) {
                2 -> if (year % 4 == 0) 29 else 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
        }
        if (monthIndex == thisMonth && year == thisYear) {
            days += (1..thisDay).map { it.toString() }
        }
    else {
        days += (1..daysInMonth).map { it.toString() }
    }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

    }

    protected fun initPasswordViews(
        password: EditText,
        confirm: EditText,
        container: FrameLayout,
        requirementsView: View,
        pConfirmContainer: FrameLayout
    ) {
        newPassword = password
        confirmPassword = confirm
        passwordRequirementsContainer = container
        pRequirementsView = requirementsView
        pConfirmView = pConfirmContainer



        setupPasswordWatcher()
        setupConfirmPasswordWatcher()
    }

    private fun setupPasswordWatcher() {
        newPassword.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                var strength = 0

                val tvRequirements = pRequirementsView.findViewById<TextView>(R.id.tvPRequirements)
                val pbPStrength = pRequirementsView.findViewById<ProgressBar>(R.id.pbPStrength)

                val hasDigit = password.any { it.isDigit() }
                val hasUppercase = password.any { it.isUpperCase() }
                val hasLowercase = password.any { it.isLowerCase() }
                val hasSpecialChar = password.any { !it.isLetterOrDigit() }
                val isLongEnough = password.length >= 8

                if (isLongEnough) strength += 20
                if (hasDigit) strength += 20
                if (hasUppercase) strength += 20
                if (hasLowercase) strength += 20
                if (hasSpecialChar) strength += 20

                val parts = listOf(
                    "a digit" to hasDigit,
                    "an uppercase char" to hasUppercase,
                    "a lowercase char" to hasLowercase,
                    "a special char" to hasSpecialChar,
                    "8+ chars" to isLongEnough
                ).filter { (_, met) -> !met }

                if (parts.isEmpty()) {
                    tvRequirements.visibility = View.GONE
                } else {
                    tvRequirements.visibility = View.VISIBLE
                    val spannable = SpannableStringBuilder("Must include: ")
                    parts.forEachIndexed { index, (label, _) ->
                        if (index > 0) spannable.append(" · ")
                        spannable.append(label)
                    }
                    tvRequirements.text = spannable
                }

                val colorRes = when {
                    strength <= 40 -> R.color.rule_weak
                    strength in 41..80 -> R.color.rule_medium
                    else -> R.color.rule_strong
                }
                pbPStrength.max = 100
                pbPStrength.progress = strength

                if (strength == 100) passwordRequirementsContainer.visibility = View.GONE
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                passwordRequirementsContainer.visibility = View.VISIBLE
            }
        })
    }
    private fun setupConfirmPasswordWatcher() {
        confirmPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val confirm = s.toString()
                val password = newPassword.text.toString()
                val tvIsMatching = pConfirmView.findViewById<TextView>(R.id.tvPIsMatching)

                val isMatch = confirm == password

                val colorRes = if (isMatch) R.color.rule_strong else R.color.text_white

                // Update confirm password text color
                confirmPassword.setTextColor(
                    ContextCompat.getColor(confirmPassword.context, colorRes)
                )

                // Update message color
                tvIsMatching.setTextColor(
                    ContextCompat.getColor(confirmPassword.context, colorRes)
                )

                // Show or hide the confirm message
                pConfirmView.visibility = if (isMatch) View.GONE else View.VISIBLE
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupEmailWatcher() {
        email.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()

                if (!isValidEmail(text)) {
                    email.setTextColor(
                        ContextCompat.getColor(email.context, R.color.rule_weak)
                    )
                } else {
                    email.setTextColor(
                        ContextCompat.getColor(email.context, R.color.rule_strong)
                    )
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
