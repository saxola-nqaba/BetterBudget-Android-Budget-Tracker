package com.dachkaboiz.betterbudget_bestbudget.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.adapter.ExpenseAdapter
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpensesFragment : Fragment(R.layout.fragment_expenses) {

    private lateinit var adapter: ExpenseAdapter
    private lateinit var rvExpenses: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var rgSortOrder: RadioGroup
    private lateinit var tvDateFrom: TextView
    private lateinit var tvDateTo: TextView

    private lateinit var repository: ExpenseRepository
    private val firebaseCategoryRepository = FirebaseCategoryRepository()

    private var categoryCache: List<Category> = emptyList()

    private var subCategoryCache: List<SubCategory> =emptyList()

    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    private val currentUserEmail: String by lazy {
        requireActivity().getSharedPreferences("auth", 0).getString("email", "") ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        repository = ExpenseRepository(uid)

        rvExpenses   = view.findViewById(R.id.rvExpenses)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        rgSortOrder  = view.findViewById(R.id.rgSortOrder)
        tvDateFrom   = view.findViewById(R.id.tvDateFrom)
        tvDateTo     = view.findViewById(R.id.tvDateTo)

        // ── Default date range: FROM = 2 days ago, TO = today ─────────────────
        val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val fromCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        dateFrom = fromCal.timeInMillis
        tvDateFrom.text = "${fmt.format(fromCal.time)} ⌵"

        val toCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 2)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        dateTo = toCal.timeInMillis
        tvDateTo.text = "${fmt.format(toCal.time)} ⌵"
        // ─────────────────────────────────────────────────────────────────────

        adapter = ExpenseAdapter(
            onItemClick     = { expense -> showDetailDialog(expense) },
            onItemLongClick = { },
            onEditClick     = { expense -> navigateToEdit(expense) },
            onDeleteClick   = { expense -> navigateToDelete(expense) },
            categoryNameResolver = { categoryId ->
                val cat = categoryCache.find { it.firebaseId == categoryId }
                "${cat?.categoryIcon ?: "💰"} ${cat?.categoryName ?: "Category $categoryId"}"
            },
            subCategoryNameResolver = { subCategoryId ->
                val sub = subCategoryCache.find { it.firebaseId == subCategoryId }
                "${sub?.subCategoryIcon ?: ""} ${sub?.subCategoryName ?: "Subcategory $subCategoryId"}"
            }
        )

        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvExpenses.adapter = adapter

        firebaseCategoryRepository.getCategories(uid) { list ->
            categoryCache = list
            lifecycleScope.launch {
                val subCatRepo = com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository(uid)
                subCategoryCache = list.flatMap { cat ->
                    subCatRepo.getSubCategories(cat.firebaseId)
                }
                loadExpenses()
            }
        }

        tvDateFrom.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dateFrom = cal.timeInMillis
                tvDateFrom.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                loadExpenses()
            }
        }

        tvDateTo.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dateTo = cal.timeInMillis
                tvDateTo.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                loadExpenses()
            }
        }

        rgSortOrder.setOnCheckedChangeListener { _, _ -> loadExpenses() }

        view.findViewById<MaterialButton>(R.id.btnAddExpense).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, AddExpenseFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firebaseCategoryRepository.getCategories(uid) { list ->
            categoryCache = list
            lifecycleScope.launch {
                val subCatRepo = com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository(uid)
                subCategoryCache = list.flatMap { cat ->
                    subCatRepo.getSubCategories(cat.firebaseId)
                }
                loadExpenses()
            }
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            var expenses = repository.getExpensesByUser(currentUserEmail)

            val from = dateFrom
            val to   = dateTo

            if (from != null && to != null && from > to) {
                Toast.makeText(
                    requireContext(),
                    "FROM date cannot be after TO date — resetting to defaults",
                    Toast.LENGTH_SHORT
                ).show()

                // Reset to defaults instead of null
                val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                val fromCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -2)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                dateFrom = fromCal.timeInMillis
                tvDateFrom.text = "${fmt.format(fromCal.time)} ⌵"

                val toCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }
                dateTo = toCal.timeInMillis
                tvDateTo.text = "${fmt.format(toCal.time)} ⌵"
            } else {
                if (from != null) expenses = expenses.filter { it.expenseDate >= from }
                if (to   != null) expenses = expenses.filter { it.expenseDate <= to }
            }

            val sorted = when (rgSortOrder.checkedRadioButtonId) {
                R.id.rbSortFirstAdded -> expenses.sortedBy { it.expenseID }
                R.id.rbSortLastAdded  -> expenses.sortedByDescending { it.expenseID }

                // Code Attribution
                // This method was adapted from Kotlin Documentation
                // https://kotlinlang.org/docs/collection-ordering.html
                // Kotlin

                else -> expenses.sortedBy { exp ->
                    categoryCache.find { it.firebaseId == exp.categoryId }?.categoryName ?: ""
                }
            }

            adapter.submitList(sorted.toMutableList())

            if (sorted.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                rvExpenses.visibility   = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                rvExpenses.visibility   = View.VISIBLE
            }
        }
    }

    private fun navigateToEdit(expense: Expense) {
        val fragment = EditExpenseFragment()
        fragment.arguments = Bundle().apply {
            putString("expenseId", expense.expenseID)
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToDelete(expense: Expense) {
        val fragment = DeleteExpenseFragment()
        fragment.arguments = Bundle().apply {
            putString("expenseId", expense.expenseID)
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
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

    private fun showDetailDialog(expense: Expense) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val catName = categoryCache.find { it.firebaseId == expense.categoryId }?.categoryName
            ?: "Category ${expense.categoryId}"

        val message = buildString {
            append("Category: $catName\n")
            append("Amount: R %.2f\n".format(expense.expenseAmount))
            append("Date: ${dateFormat.format(Date(expense.expenseDate))}\n")
            if (!expense.expenseDescription.isNullOrBlank())
                append("Description: ${expense.expenseDescription}\n")
            if (expense.imageUri != null)
                append("Photo: Attached")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Expense Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
}
