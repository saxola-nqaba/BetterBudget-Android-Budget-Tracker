package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.Worker.AutomationWorker
import java.util.concurrent.TimeUnit

/*
  MainActivity is the root activity for all authenticated users.
  It hosts the bottom navigation bar and a single fragment container
  that swaps between Expenses, Categories, Goals, Insights, and Profile.

  On launch it checks whether the user is logged in via SharedPreferences.
  If no email is found, it redirects to LoginActivity immediately.
 It also schedules the AutomationWorker to run daily in the background.
 */
class MainActivity : AppCompatActivity() {
    // Email retrieved from SharedPreferences to verify the user is logged in
    var email: String? = null
    // Top bar title that updates dynamically based on the active tab
    private lateinit var tvScreenTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // Schedule the background automation worker as early as possible
        scheduleAutomationWorker()

        // Check login state and redirect to login if no session exists
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        email = prefs.getString("email", null)

        if(email == null){
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvScreenTitle = findViewById(R.id.tvScreenTitle)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load the default fragment on first creation only to prevent reload on rotation
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, ExpensesFragment())
                .commit()
            tvScreenTitle.text = "EXPENSES"
            highlightNavSelection(R.id.btnExpense)
        }

        //Bottom Navigation Click Listeners
        // Each button replaces the fragment container contents and updates the screen title and active nav highlight accordingly
        findViewById<ImageButton>(R.id.btnExpense).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, ExpensesFragment())
                .commit()
            tvScreenTitle.text = "EXPENSES"
            highlightNavSelection(R.id.btnExpense)
        }

        findViewById<ImageButton>(R.id.btnCategories).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, CategoryFragment())
                .commit()
            tvScreenTitle.text = "CATEGORIES"
            highlightNavSelection(R.id.btnCategories)
        }

        findViewById<ImageButton>(R.id.btnGoals).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, GoalHomeFragment())
                .addToBackStack(null)
                .commit()
            tvScreenTitle.text = "GOALS"
            highlightNavSelection(R.id.btnGoals)
        }

        findViewById<ImageButton>(R.id.btnRewards).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, InsightsFragment())
                .addToBackStack(null)
                .commit()
            tvScreenTitle.text = "INSIGHTS"
            highlightNavSelection(R.id.btnRewards)
        }

        findViewById<ImageButton>(R.id.btnCategoryViewProfile).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, ProfileFragment())
                .addToBackStack(null)
                .commit()
            tvScreenTitle.text = "PROFILE"
        }

    }


    // Loops through navigation assets to highlight the active view
    // and dim inactive layout assets cleanly.

    private fun highlightNavSelection(selectedButtonId: Int) {
        // Pairs each navigation Button with its designated title text element
        val navTabs = listOf(
            Pair(R.id.btnExpense, R.id.tvNavExpense),
            Pair(R.id.btnCategories, R.id.tvNavCategories),
            Pair(R.id.btnGoals, R.id.tvNavGoals),
            Pair(R.id.btnRewards, R.id.tvNavRewards)
        )

        // Active tab uses the brand mid-blue accent; inactive tabs use plain white
        val activeColor = Color.parseColor("#2196F3")  // Material Light Blue highlight accent
        val inactiveColor = Color.parseColor("#FFFFFF") // Regular clean white text color

        for ((btnId, tvId) in navTabs) {
            val button = findViewById<ImageButton>(btnId)
            val textView = findViewById<TextView>(tvId)

            if (btnId == selectedButtonId) {
                button.setColorFilter(activeColor)
                textView.setTextColor(activeColor)
            } else {
                button.setColorFilter(inactiveColor)
                textView.setTextColor(inactiveColor)
            }
        }
    }
    private fun scheduleAutomationWorker() {
        val request = PeriodicWorkRequestBuilder<AutomationWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "automation_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

        // Also run immediately on app start
        val immediate = OneTimeWorkRequestBuilder<AutomationWorker>().build()
        WorkManager.getInstance(this).enqueue(immediate)
    }

}

