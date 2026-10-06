package com.dachkaboiz.betterbudget_bestbudget.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class EditExpenseFragment : Fragment(R.layout.fragment_edit_expense_v2) {

    private lateinit var repository: ExpenseRepository
    private val firebaseCategoryRepository = FirebaseCategoryRepository()
    private var currentImageUri: Uri? = null
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var vPhotoPlaceholder: View
    private var editingExpenseId: String = ""

    private var selectedCategoryFirebaseId: String = ""
    private var categoryList: List<Category> = emptyList()

    private val currentUserEmail: String by lazy {
        requireActivity().getSharedPreferences("auth", 0).getString("email", "") ?: ""
    }
    private fun openCamera() {
        currentImageUri = createImageUri()
        // Open camera safely
        currentImageUri?.let { uri ->
            // Launch camera
            takePictureLauncher.launch(uri)
        }
    }
    private fun createImageUri(): Uri {
        val photoFile = File(
            requireContext().externalCacheDir,
            "expense_${System.currentTimeMillis()}.jpg"
        )

        //  Ensure the file actually exists on disk
        photoFile.createNewFile()

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
    }

    // TAKE PICTURE
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {
                ivPhotoPreview.setImageURI(currentImageUri)
                ivPhotoPreview.visibility = View.VISIBLE
                vPhotoPlaceholder.visibility = View.GONE
            } else {
                Toast.makeText(requireContext(), "Could not take picture", Toast.LENGTH_SHORT).show()
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

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                currentImageUri = uri
                updatePhotoUI()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        repository = ExpenseRepository(uid)

        editingExpenseId = arguments?.getString("expenseId") ?: ""

        // FIXED: correct validation
        if (editingExpenseId.isEmpty()) {
            Toast.makeText(requireContext(), "No expense to edit.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        ivPhotoPreview = view.findViewById(R.id.ivEditPhotoPreview)
        vPhotoPlaceholder = view.findViewById(R.id.vEditPhotoPlaceholder)
        val btnCamera = view.findViewById<Button>(R.id.btnEditTakePhoto)
        val btnGallery = view.findViewById<Button>(R.id.btnEditAddFromGallery)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerEditExpenseCategory)
        val etDay = view.findViewById<EditText>(R.id.etEditExpenseDay)
        val etMonth = view.findViewById<EditText>(R.id.etEditExpenseMonth)
        val etYear = view.findViewById<EditText>(R.id.etEditExpenseYear)
        val etDescription = view.findViewById<EditText>(R.id.etEditExpenseDescription)
        val etAmount = view.findViewById<EditText>(R.id.etEditExpenseAmount)
        val btnCancel = view.findViewById<Button>(R.id.btnEditExpenseCancel)
        val btnUpdate = view.findViewById<Button>(R.id.btnEditExpenseUpdate)

        btnCamera.setOnClickListener { requestPermissionLauncher.launch(android.Manifest.permission.CAMERA) }
        btnGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }

        firebaseCategoryRepository.getCategories(uid) { list ->
            categoryList = list
            if (categoryList.isEmpty()) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "No categories found.", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
                return@getCategories
            }

            val names = categoryList.map { it.categoryName }
            requireActivity().runOnUiThread {
                spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
                spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                        selectedCategoryFirebaseId = categoryList[pos].firebaseId
                    }
                    override fun onNothingSelected(p: AdapterView<*>) {}
                }
            }

            lifecycleScope.launch {
                val expense = repository.getExpenseById(editingExpenseId)
                expense?.let {
                    val index = categoryList.indexOfFirst { c ->
                        c.firebaseId == it.categoryId
                    }

                    requireActivity().runOnUiThread {
                        if (index >= 0) spinnerCategory.setSelection(index)
                    }

                    etAmount.setText(it.expenseAmount.toString())
                    etDescription.setText(it.expenseDescription ?: "")

                    val cal = Calendar.getInstance().apply { timeInMillis = it.expenseDate }
                    etDay.setText(cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'))
                    etMonth.setText((cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'))
                    etYear.setText(cal.get(Calendar.YEAR).toString())

                    it.imageUri?.let { uriStr ->
                        currentImageUri = Uri.parse(uriStr)
                        updatePhotoUI()
                    }
                }
            }
        }

        btnUpdate.setOnClickListener {
            val dayText = etDay.text.toString().trim()
            val monthText = etMonth.text.toString().trim()
            val yearText = etYear.text.toString().trim()
            val amountText = etAmount.text.toString().trim()
            val description = etDescription.text.toString().trim()

            var hasError = false

            val amountDouble = amountText.toDoubleOrNull()
            if (amountDouble == null || amountDouble <= 0) {
                etAmount.error = "Enter a valid amount greater than 0"; hasError = true
            }

            val day = dayText.toIntOrNull()
            if (day == null || day !in 1..31) { etDay.error = "1–31"; hasError = true }

            val month = monthText.toIntOrNull()
            if (month == null || month !in 1..12) { etMonth.error = "1–12"; hasError = true }

            val year = yearText.toIntOrNull()
            if (year == null || year !in 2000..2100) { etYear.error = "e.g. 2025"; hasError = true }

            if (selectedCategoryFirebaseId.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                hasError = true
            }

            if (hasError) return@setOnClickListener

            val cal = Calendar.getInstance().apply {
                set(year!!, month!! - 1, day!!, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            lifecycleScope.launch {
                repository.updateExpense(
                    Expense(
                        expenseID = editingExpenseId,
                        userEmail = currentUserEmail,
                        categoryId = selectedCategoryFirebaseId,
                        subCategoryId = null,
                        expenseAmount = amountDouble!!,
                        expenseDate = cal.timeInMillis,
                        expenseDescription = description.ifEmpty { null },
                        imageUri = currentImageUri?.toString(),
                        imageName = null,
                        imageDescription = null,
                        automationFrequency = null,
                        automationMultiplier = null
                    )
                )

                Toast.makeText(requireContext(), "Expense updated!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
}
