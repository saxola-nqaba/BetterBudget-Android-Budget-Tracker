package com.dachkaboiz.betterbudget_bestbudget.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.adapter.CategoryAdapter
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseSnoozeRepository
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.CategoryViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CategoryFragment : Fragment(R.layout.fragment_category) {

    private lateinit var viewModel: CategoryViewModel
    private lateinit var adapter: CategoryAdapter<Triple<Category, CategoryGoal?, Pair<List<Expense>, Boolean>>>
    // Hold references so onResume can trigger a rebuild without
    // re-running the full onViewCreated setup
    private var rgSortOrder: RadioGroup? = null
    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val rgSort  = view.findViewById<RadioGroup>(R.id.rgFullCategorySort)
        val dpStart  = view.findViewById<TextView>(R.id.dpFullCategoryStartDate)
        val dpFinish = view.findViewById<TextView>(R.id.dpFullCategoryEndDate)
        val listView = view.findViewById<ListView>(R.id.lvFullCategories)
        val btnAdd   = view.findViewById<Button>(R.id.btnAddFullCategory)


        rgSortOrder = rgSort

        viewModel = ViewModelProvider(this)[CategoryViewModel::class.java]

        btnAdd.setOnClickListener {
            swapToFragment(AddCategoryFragment())
        }

        adapter = CategoryAdapter(
            context          = requireActivity(),
            items            = emptyList(),
            parentFirebaseId = "ROOT",
            showBreakdownButton = true,
            onItemClick      = { triple ->
                swapToFragment(CategoryBreakdownFragment(triple.first.firebaseId))
            },
            onEditClick      = { triple ->
                swapToFragment(UpdateCategoryFragment(triple.first.firebaseId))
            },
            onDeleteClick    = { triple ->
                swapToFragment(DeleteCategoryFragment(triple.first.firebaseId))
            }
        )

        listView.adapter = adapter

        // ── Default date range: FROM = today, TO = 2 days from now ───────────
        val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val fromCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        dateFrom = fromCal.timeInMillis
        dpStart.text = "${fmt.format(fromCal.time)} ⌵"

        val toCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 2)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        dateTo = toCal.timeInMillis
        dpFinish.text = "${fmt.format(toCal.time)} ⌵"
// ─────────────────────────────────────────────────────────────────────

        // Keep a reference so onResume can call buildAndDisplayList

        viewModel.loadCategories(uid)

        // Rebuild the list every time the category data changes in the ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categoryList ->
                buildAndDisplayList(categoryList, rgSort)
            }
        }

        rgSort.setOnCheckedChangeListener { _, _ ->
            buildAndDisplayList(viewModel.categories.value, rgSort)
        }

        dpStart.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dateFrom = cal.timeInMillis
                dpStart.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                buildAndDisplayList(viewModel.categories.value, rgSort)
            }
        }

        dpFinish.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dateTo = cal.timeInMillis
                dpFinish.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                buildAndDisplayList(viewModel.categories.value, rgSort)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Re-fetch from Firebase so the list reflects any edits
        // made in UpdateCategoryFragment or AddCategoryFragment
        // before this screen was resumed
        viewModel.loadCategories(uid)

        // Also rebuild the display immediately with whatever
        // the ViewModel currently holds, in case loadCategories
        // is slow and the list looks stale for a moment
        val rg = rgSortOrder ?: return
        buildAndDisplayList(viewModel.categories.value, rg)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the reference to avoid a memory leak — the
        // RadioGroup belongs to the view which is being destroyed
        rgSortOrder = null
    }

    private fun buildAndDisplayList(
        categoryList: List<Category>,
        rgSort: RadioGroup
    ) {
        lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val now = Calendar.getInstance()
            val currentMonth = now.get(Calendar.MONTH) + 1
            val currentYear = now.get(Calendar.YEAR)

            val goalRepo    = FirebaseCategoryGoalRepository(uid)
            val expenseRepo = ExpenseRepository(uid)
            val snoozeRepo  = FirebaseSnoozeRepository(uid)
            val allGoals    = goalRepo.getAllGoals()

            val combined = categoryList.map { cat ->
                val goal = allGoals.firstOrNull { it.categoryId == cat.firebaseId }

                val filteredExpenses = expenseRepo
                    .getExpensesByCategory(cat.firebaseId)
                    .filter { exp ->
                        val fromOk = dateFrom?.let { exp.expenseDate >= it } ?: true
                        val toOk   = dateTo?.let { exp.expenseDate <= it } ?: true
                        fromOk && toOk
                    }
                val snoozedIds = snoozeRepo.getSnoozedExpenseIds(
                    cat.firebaseId, currentMonth, currentYear
                )

                // Tag the category as snoozed if any snooze exists this month
                val isSnoozed = snoozedIds.isNotEmpty()

                Triple(cat, goal, filteredExpenses to isSnoozed)
            }

            val sorted = when (rgSort.checkedRadioButtonId) {
                R.id.rbFullCategoryFirstAdded -> combined.sortedBy { it.first.firebaseId }
                R.id.rbFullCategoryLastAdded  -> combined.sortedByDescending { it.first.firebaseId }
                else                          -> combined.sortedBy { it.first.categoryName }
            }
            adapter.updateItems(sorted)
        }
    }

    private fun showDatePicker(onDateSelected: (Int, Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day -> onDateSelected(year, month, day) },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun swapToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }
}