package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.Worker.AutomationWorker
import com.dachkaboiz.betterbudget_bestbudget.data.model.AutomatedExpense
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SnoozeCount
import com.dachkaboiz.betterbudget_bestbudget.data.repository.AutomatedExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseSnoozeRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.utils.AutomationScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AddExpenseFragment : Fragment(R.layout.fragment_add_expense_v2) {

    private lateinit var repository: ExpenseRepository
    private lateinit var automatedRepo: AutomatedExpenseRepository
    private lateinit var snoozeRepository: FirebaseSnoozeRepository
    private lateinit var goalRepository: FirebaseCategoryGoalRepository
    private val firebaseCategoryRepository = FirebaseCategoryRepository()
    private lateinit var subCategoryRepository: SubCategoryRepository

    private lateinit var spinnerSubCategory: Spinner

    private var editingExpenseId: String? = null
    private var selectedCategoryFirebaseId: String = ""
    private var firebaseSubCategoryID: String = ""
    private var categoryList: List<Category> = emptyList()

    // Holds the expense waiting for the snooze dialog result
    private var pendingExpense: Expense? = null

    // Prevents stale dialog results from firing on subsequent dialogs
    private var isWaitingForSnoozeResult = false

    private var currentImageUri: Uri? = null
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var vPhotoPlaceholder: View

    private val currentUserEmail: String by lazy {
        requireActivity().getSharedPreferences("auth", 0).getString("email", "") ?: ""
    }
    private var selectedFrequencyUnit: String? = null   // null = no automation selected
    private var selectedMultiplier: Int = 1
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(
                requireContext(),
                "Camera permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }

    private fun createImageUri(): Uri {
        val photoFile = File(
            requireContext().externalCacheDir,
            "expense_${System.currentTimeMillis()}.jpg"
        )
        photoFile.createNewFile()
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    currentImageUri = uri
                    updatePhotoUI()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to persist permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private fun openCamera() {
        currentImageUri = createImageUri()
        currentImageUri?.let { takePictureLauncher.launch(it) }
    }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {
                ivPhotoPreview.setImageURI(currentImageUri)
                ivPhotoPreview.visibility = View.VISIBLE
                vPhotoPlaceholder.visibility = View.GONE
            } else {
                Toast.makeText(
                    requireContext(),
                    "Could not take picture",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun updatePhotoUI() {
        if (currentImageUri != null) {
            ivPhotoPreview.setImageURI(currentImageUri)
            ivPhotoPreview.visibility = View.VISIBLE
            vPhotoPlaceholder.visibility = View.GONE
        } else {
            ivPhotoPreview.visibility = View.GONE
            vPhotoPlaceholder.visibility = View.VISIBLE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        repository           = ExpenseRepository(uid)
        snoozeRepository     = FirebaseSnoozeRepository(uid)
        goalRepository       = FirebaseCategoryGoalRepository(uid)
        automatedRepo = AutomatedExpenseRepository(uid)
        subCategoryRepository = SubCategoryRepository(uid)

        editingExpenseId = arguments?.getString("expenseId")

        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerExpenseCategory)
        spinnerSubCategory  = view.findViewById(R.id.spinnerExpenseSubCategory)
        val etDay           = view.findViewById<EditText>(R.id.etExpenseDay)
        val etMonth         = view.findViewById<EditText>(R.id.etExpenseMonth)
        val etYear          = view.findViewById<EditText>(R.id.etExpenseYear)
        val etDescription   = view.findViewById<EditText>(R.id.etExpenseDescription)
        val etAmount        = view.findViewById<EditText>(R.id.etExpenseAmount)
        val btnCancel       = view.findViewById<Button>(R.id.btnExpenseCancel)
        val btnAdd          = view.findViewById<Button>(R.id.btnExpenseAdd)
        ivPhotoPreview      = view.findViewById(R.id.ivPhotoPreview)
        vPhotoPlaceholder   = view.findViewById(R.id.vPhotoPlaceholder)
        val btnTakePhoto    = view.findViewById<Button>(R.id.btnTakePhoto)
        val btnGallery      = view.findViewById<Button>(R.id.btnAddFromGallery)

        // ── Automation views ────────────────────────
        val rgFrequency      = view.findViewById<RadioGroup>(R.id.rgAutomateFrequency)
        val etMultiplier     = view.findViewById<EditText>(R.id.etMultiplier)
        val tvFrequencyUnit  = view.findViewById<TextView>(R.id.tvFrequencyUnit)

        btnTakePhoto.setOnClickListener {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        btnGallery.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        rgFrequency.setOnCheckedChangeListener { _, checkedId ->
            selectedFrequencyUnit = when (checkedId) {
                R.id.rbDay   -> "DAY"
                R.id.rbWeek  -> "WEEK"
                R.id.rbMonth -> "MONTH"
                R.id.rbYear  -> "YEAR"
                else         -> null
            }
            // Update the label next to the multiplier field so the user
            // sees e.g. "3  months" as they type
            tvFrequencyUnit.text = when (selectedFrequencyUnit) {
                "DAY"   -> "day(s)"
                "WEEK"  -> "week(s)"
                "MONTH" -> "month(s)"
                "YEAR"  -> "year(s)"
                else    -> "—"
            }
        }

        // Listen for result from SnoozeDialogFragment
        // isWaitingForSnoozeResult guards against stale results
        // firing from a previous dialog session

        // Code Attribution
        // This method was adapted from Android Developers
        // https://developer.android.com/guide/fragments/communicate
        // Android Developers

        parentFragmentManager.setFragmentResultListener(
            "snooze_result",
            viewLifecycleOwner
        ) { _, bundle ->

            if (!isWaitingForSnoozeResult) return@setFragmentResultListener
            isWaitingForSnoozeResult = false

            val action  = bundle.getString("action")
            val expense = pendingExpense ?: return@setFragmentResultListener

            when (action) {

                "USE_SNOOZE" -> {
                    // Save expense and save a SnoozeCount record
                    // The SnoozeCount record tells GoalHomeFragment
                    // to exclude this expense from the progress bar
                    lifecycleScope.launch {
                        repository.insertExpense(expense)

                        val snoozeId = snoozeRepository.generateSnoozeId()
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = expense.expenseDate
                        }
                        val record = SnoozeCount(
                            snoozeId   = snoozeId,
                            expenseId  = expense.expenseID,
                            categoryId = expense.categoryId,
                            month      = cal.get(Calendar.MONTH) + 1,
                            year       = cal.get(Calendar.YEAR)
                        )
                        snoozeRepository.saveSnooze(record)

                        Toast.makeText(
                            requireContext(),
                            "Expense saved. Snooze used.",
                            Toast.LENGTH_SHORT
                        ).show()
                        pendingExpense = null
                        parentFragmentManager.popBackStack()
                    }
                }

                "SAVE" -> {
                    // Save expense without a snooze record
                    // It WILL count toward the maximum on the progress bar
                    lifecycleScope.launch {
                        repository.insertExpense(expense)
                        Toast.makeText(
                            requireContext(),
                            "Expense saved (over budget).",
                            Toast.LENGTH_SHORT
                        ).show()
                        pendingExpense = null
                        parentFragmentManager.popBackStack()
                    }
                }

                "CANCEL" -> {
                    // User cancelled — do not save anything
                    pendingExpense = null
                    Toast.makeText(
                        requireContext(),
                        "Expense not saved.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Load categories from Firebase
        firebaseCategoryRepository.getCategories(uid) { list ->
            categoryList = list

            if (categoryList.isEmpty()) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Please create a category first.",
                        Toast.LENGTH_LONG
                    ).show()
                    parentFragmentManager.popBackStack()
                }
                return@getCategories
            }

            val categoryNames = categoryList.map { it.categoryName }
            requireActivity().runOnUiThread {
                spinnerCategory.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    categoryNames
                )
            }

            selectedCategoryFirebaseId = categoryList[0].firebaseId
            loadSubCategoriesFor(selectedCategoryFirebaseId)


            // Setting this listener will automatically trigger the initial subcategory load pass safely.
            spinnerCategory.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>, v: View?,
                        position: Int, id: Long
                    ) {
                        selectedCategoryFirebaseId = categoryList[position].firebaseId
                        loadSubCategoriesFor(selectedCategoryFirebaseId)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
        }

        // Pre-fill fields if editing an existing expense
        lifecycleScope.launch {
            if (editingExpenseId != null) {
                val expense = repository.getExpenseById(editingExpenseId!!)
                if (expense != null) {
                    etAmount.setText(expense.expenseAmount.toString())
                    etDescription.setText(expense.expenseDescription ?: "")
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = expense.expenseDate
                    }
                    etDay.setText(
                        cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                    )
                    etMonth.setText(
                        (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                    )
                    etYear.setText(cal.get(Calendar.YEAR).toString())
                    expense.imageUri?.let {
                        currentImageUri = Uri.parse(it)
                        updatePhotoUI()
                    }
                }
            } else {
                val cal = Calendar.getInstance()
                etDay.setText(
                    cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                )
                etMonth.setText(
                    (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                )
                etYear.setText(cal.get(Calendar.YEAR).toString())
            }
        }

        btnAdd.setOnClickListener {
            val day = etDay.text.toString().trim().toIntOrNull()
            val month = etMonth.text.toString().trim().toIntOrNull()
            val year = etYear.text.toString().trim().toIntOrNull()
            val amount = etAmount.text.toString().trim().toDoubleOrNull()
            val description = etDescription.text.toString().trim()
            selectedMultiplier = if (selectedFrequencyUnit != null) {
                etMultiplier.text.toString().toIntOrNull() ?: 1
            } else {
                1   // ignored when automation is off
            }


            if (day == null || month == null || year == null ||
                amount == null || amount <= 0
            ) {
                Toast.makeText(
                    requireContext(),
                    "Please fill all fields correctly.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val cal = Calendar.getInstance().apply {
                set(year, month - 1, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val id = editingExpenseId ?: repository.generateExpenseId()

            val expense = Expense(
                expenseID = id,
                userEmail = currentUserEmail,
                categoryId = selectedCategoryFirebaseId,
                subCategoryId = if (firebaseSubCategoryID.isBlank()) null else firebaseSubCategoryID,
                expenseAmount = amount,
                expenseDate = cal.timeInMillis,
                expenseDescription = description.ifEmpty { null },
                imageUri = currentImageUri?.toString(),
                imageName = null,
                imageDescription = null,
                automationFrequency = null,
                automationMultiplier = null
            )

            lifecycleScope.launch {
                var automationToSave: AutomatedExpense? = null

                if (editingExpenseId == null && selectedFrequencyUnit != null) {




                    automationToSave = AutomatedExpense(
                        firebaseId = "",
                        categoryFirebaseId = selectedCategoryFirebaseId,
                        amount = expense.expenseAmount,
                        description = expense.expenseDescription,
                        imageUri = expense.imageUri,
                        frequencyUnit = selectedFrequencyUnit!!,
                        frequencyMultiplier = selectedMultiplier-1,
                        nextRunDate = expense.expenseDate,
                        startDate = expense.expenseDate,
                        lastRunDate = null,
                        active = true,
                        userEmail = currentUserEmail
                    )
                }


                // Editing an existing expense — just update, no snooze check
                if (editingExpenseId != null) {
                    repository.updateExpense(expense)
                    Toast.makeText(
                        requireContext(),
                        "Expense updated!",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                    return@launch
                }

                // Check if a goal exists for this category this month
                val allGoals = goalRepository.getAllGoals()
                val goal = allGoals.firstOrNull {
                    it.categoryId == selectedCategoryFirebaseId &&
                            it.month == month &&
                            it.year == year
                }

                val maxGoal = goal?.maxGoal

                if (maxGoal != null) {

                    // Get all expenses for this category this month
                    val existingExpenses = repository
                        .getExpensesByCategory(selectedCategoryFirebaseId)
                        .filter {
                            val expCal = Calendar.getInstance().apply {
                                timeInMillis = it.expenseDate
                            }
                            expCal.get(Calendar.MONTH) + 1 == month &&
                                    expCal.get(Calendar.YEAR) == year
                        }

                    // Get snoozed IDs to exclude from current total
                    val snoozedIds = snoozeRepository.getSnoozedExpenseIds(
                        selectedCategoryFirebaseId, month, year
                    )

                    // Only count non-snoozed expenses toward the total
                    val currentTotal = existingExpenses
                        .filter { it.expenseID !in snoozedIds }
                        .sumOf { it.expenseAmount }

                    if (currentTotal + amount > maxGoal) {

                        // Check how many snoozes the user has left this month
                        val snoozesUsed = snoozeRepository.getSnoozeCountForMonth(
                            month, year
                        )
                        val snoozesLeft = maxOf(0, 2 - snoozesUsed)

                        val catName = categoryList
                            .find { it.firebaseId == selectedCategoryFirebaseId }
                            ?.categoryName ?: "this category"

                        // Store the expense and set the flag before
                        // showing the dialog so the listener knows
                        // this result is valid
                        pendingExpense = expense
                        isWaitingForSnoozeResult = true

                        SnoozeDialogFragment.newInstance(
                            categoryName = catName,
                            currentTotal = currentTotal,
                            maxGoal = maxGoal,
                            newAmount = amount,
                            snoozesLeft = snoozesLeft
                        ).show(parentFragmentManager, "snooze_dialog")

                        return@launch
                    }
                }


                if (automationToSave == null) {
                    repository.insertExpense(expense)
                    parentFragmentManager.popBackStack()
                }

                Toast.makeText(
                    requireContext(),
                    "Expense saved!",
                    Toast.LENGTH_SHORT
                ).show()
                automationToSave?.let { auto ->

                    automatedRepo.insertAutomatedExpense(auto) { savedAuto ->

                        if (savedAuto == null) {
                            Toast.makeText(
                                requireContext(),
                                "Automation failed to save.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@insertAutomatedExpense
                        }

                        lifecycleScope.launch {

                            // Run catch-up automation
                            AutomationScheduler.runCatchUpFor(
                                savedAuto,
                                ExpenseRepository(uid),
                                AutomatedExpenseRepository(uid)
                            )


                            // Trigger Worker
                            val request = OneTimeWorkRequestBuilder<AutomationWorker>().build()
                            WorkManager.getInstance(requireContext()).enqueue(request)

                            Toast.makeText(
                                requireContext(),
                                "Automation saved!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // NOW it is safe to close the fragment
                            if (isAdded) {
                                parentFragmentManager.popBackStack()
                            }
                        }
                    }

                    return@launch   // prevent double popBackStack
                }
            }
            }

    }

    private fun loadSubCategoriesFor(categoryId: String) {
        if (categoryId.isBlank()) return   // guard against empty ID race condition

        lifecycleScope.launch {
            val subList = subCategoryRepository.getSubCategories(categoryId)

            // Always update spinner on main thread
            requireActivity().runOnUiThread {
                if (subList.isEmpty()) {
                    spinnerSubCategory.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("None")
                    )
                    firebaseSubCategoryID = ""
                    spinnerSubCategory.onItemSelectedListener = null
                    return@runOnUiThread
                }

                firebaseSubCategoryID = subList[0].firebaseId

                spinnerSubCategory.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    subList.map { it.subCategoryName }
                )

                spinnerSubCategory.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>, v: View?,
                            position: Int, id: Long
                        ) {
                            firebaseSubCategoryID = subList[position].firebaseId
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
            }
        }
    }
}