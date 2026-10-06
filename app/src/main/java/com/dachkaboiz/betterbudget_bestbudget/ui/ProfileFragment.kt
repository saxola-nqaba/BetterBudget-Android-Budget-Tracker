package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.dachkaboiz.betterbudget_bestbudget.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.util.*

class ProfileFragment : Fragment(R.layout.fragment_profile_v2) {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage

    private lateinit var etEmail: EditText
    private lateinit var etProfileName: EditText
    private lateinit var etProfileLastName: EditText
    private lateinit var tvAge: TextView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvUploadProfilePicture: TextView
    private lateinit var tvTakeProfilePicture: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button

    private lateinit var spProfileDay: Spinner
    private lateinit var spProfileMonth: Spinner
    private lateinit var spProfileYear: Spinner

    private var currentImageUri: Uri? = null
    private var isInitializing = false

    private val today = Calendar.getInstance()
    private val thisYear = today.get(Calendar.YEAR)

    // CAMERA PERMISSION
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    private fun createImageUri(): Uri {
        val photoFile = File(
            requireContext().externalCacheDir,
            "user_${System.currentTimeMillis()}.jpg"
        )

        //  Ensure the file actually exists on disk
        photoFile.createNewFile()

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
    }


    private fun openCamera() {
        currentImageUri = createImageUri()
        // Open camera safely
        currentImageUri?.let { uri ->
            // Launch camera
            takePictureLauncher.launch(uri)
    }
}

    // TAKE PICTURE
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {
                ivProfilePicture.setImageURI(currentImageUri)
            } else {
                Toast.makeText(requireContext(), "Could not take picture", Toast.LENGTH_SHORT).show()
            }
        }

    // PICK IMAGE
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                currentImageUri = uri
                ivProfilePicture.setImageURI(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        bindViews(view)
        setupSpinners()
        loadUserData()
        setupUpdateButton()
        setupLogoutButton()
        setupDeleteButton()
    }

    // ---------------------------------------------------------
    // Bind Views
    // ---------------------------------------------------------
    private fun bindViews(view: View) {
        spProfileDay = view.findViewById(R.id.spProfileDay)
        spProfileMonth = view.findViewById(R.id.spProfileMonth)
        spProfileYear = view.findViewById(R.id.spProfileYear)

        etProfileName = view.findViewById(R.id.etProfileFirstName)
        etProfileLastName = view.findViewById(R.id.etProfileLastName)
        etEmail = view.findViewById(R.id.etProfileEmail)
        tvAge = view.findViewById(R.id.tvProfileAge)

        ivProfilePicture = view.findViewById(R.id.ivProfilePicture)
        tvUploadProfilePicture = view.findViewById(R.id.tvUploadProfilePicture)
        tvTakeProfilePicture = view.findViewById(R.id.tvTakeProfilePicture)

        btnLogout = view.findViewById(R.id.btnProfileLogOut)
        btnUpdate = view.findViewById(R.id.btnProfileUpdate)
        btnDelete = view.findViewById(R.id.btnProfileDelete)

        tvUploadProfilePicture.setOnClickListener { pickImageLauncher.launch("image/*") }
        tvTakeProfilePicture.setOnClickListener {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // ---------------------------------------------------------
    // Load User Data from Firebase
    // ---------------------------------------------------------
    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        database.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (!snapshot.exists()) return

                    val firstName = snapshot.child("firstName").value?.toString()
                    val lastName = snapshot.child("lastName").value?.toString()
                    val email = snapshot.child("email").value?.toString()
                    val birthDate = snapshot.child("birthDate").value?.toString()?.toLongOrNull()
                    val age = snapshot.child("age").value?.toString()


                    etProfileName.setText(firstName ?: "")
                    etProfileLastName.setText(lastName ?: "")
                    etEmail.setText(email ?: "")
                    tvAge.text = age ?: ""

                    // Load profile picture
                    val profilePicUrl = snapshot.child("profilePicUrl").value?.toString()

                    if (!profilePicUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(profilePicUrl)
                            .placeholder(R.drawable.ic_profile)
                            .into(ivProfilePicture)
                    }


                    if (birthDate != null) {
                        val localDate = java.time.Instant.ofEpochMilli(birthDate)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()

                        isInitializing = true

                        // REMOVE LISTENERS
                        spProfileYear.onItemSelectedListener = null
                        spProfileMonth.onItemSelectedListener = null

                        // YEAR
                        spProfileYear.setSelection(getYearIndex(localDate.year))

                        // MONTH
                        spProfileMonth.setSelection(localDate.monthValue)

                        // REBUILD DAYS BEFORE SELECTING DAY
                        updateDaysSpinner(spProfileDay, localDate.monthValue, localDate.year)

                        // DAY
                        spProfileDay.setSelection(localDate.dayOfMonth)

                        isInitializing = false

                    }




                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ---------------------------------------------------------
    // Update Profile
    // ---------------------------------------------------------
    private fun setupUpdateButton() {
        btnUpdate.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            val firstNameText = etProfileName.text.toString().trim().ifEmpty { null }
            val lastNameText = etProfileLastName.text.toString().trim().ifEmpty { null }
            val emailText = etEmail.text.toString().trim()

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val year = spProfileYear.selectedItem.toString().toIntOrNull()
            val month = spProfileMonth.selectedItemPosition.takeIf { it > 0 }
            val day = spProfileDay.selectedItemPosition.takeIf { it > 0 }

            var age: Int? = null
            var birthDateMillis: Long? = null

            if (year != null && month != null && day != null) {
                age = calculateAge(day, month, year)
                birthDateMillis = LocalDate.of(year, month, day)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            }

            val updates = mutableMapOf<String, Any>()
            firstNameText?.let { updates["firstName"] = it }
            lastNameText?.let { updates["lastName"] = it }
            updates["email"] = emailText
            birthDateMillis?.let { updates["birthDate"] = it }
            age?.let { updates["age"] = it }


            database.child("users").child(uid)
                .updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        if (currentImageUri != null) {
                            val storageRef = storage.reference.child("profile_pictures/$uid.jpg")

                            storageRef.putFile(currentImageUri!!)
                                .addOnSuccessListener {
                                    storageRef.downloadUrl
                                        .addOnSuccessListener { downloadUrl ->

                                            database.child("users").child(uid)
                                                .child("profilePicUrl")
                                                .setValue(downloadUrl.toString())
                                                .addOnSuccessListener {
                                                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()

                                                    val intent = Intent(requireContext(), MainActivity::class.java)
                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    startActivity(intent)
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(requireContext(), "Failed to save image URL: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(requireContext(), "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }

                        } else {
                            // No image selected → just navigate
                            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()

                            val intent = Intent(requireContext(), MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    }

                    else {
                        Toast.makeText(
                            requireContext(),
                            task.exception?.message ?: "Failed to update profile",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }



    // ---------------------------------------------------------
    // Logout
    // ---------------------------------------------------------
    private fun setupLogoutButton() {
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("LOG OUT") { _, _ ->
                    auth.signOut()

                    val prefs = requireActivity().getSharedPreferences("auth", 0)
                    prefs.edit().clear().apply()

                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }
    }

    // ---------------------------------------------------------
    // Delete Account
    // ---------------------------------------------------------
    private fun setupDeleteButton() {
        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("DELETE") { _, _ ->
                    val uid = auth.currentUser?.uid ?: return@setPositiveButton

                    // Delete from Realtime DB
                    database.child("users").child(uid).removeValue()

                    // Delete profile picture
                    storage.reference.child("profile_pictures/$uid.jpg").delete()

                    // Delete FirebaseAuth account
                    auth.currentUser?.delete()

                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }
    }
    private fun setupSpinners() {
        setupYearSpinner(spProfileYear)
        updateMonthSpinner(spProfileMonth)
        updateDaysSpinner(spProfileDay, 1, 2000)

        attachSpinnerListeners()
    }

    private fun attachSpinnerListeners() {
        spProfileMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (isInitializing) return
                val year = spProfileYear.selectedItem.toString().toIntOrNull()
                updateDaysSpinner(spProfileDay, pos, year)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spProfileYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (isInitializing) return
                // Year change does nothing for now
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------
    private fun getYearIndex(year: Int): Int = (year - 1930) + 1
    private fun setupYearSpinner(spinner: Spinner) {
        val years = mutableListOf("YYYY") + (1930..thisYear).map { it.toString() }

        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            years
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
    private fun updateMonthSpinner(spinner: Spinner) {
        val months = mutableListOf("MM") + listOf(
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
        )

        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            months
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun updateDaysSpinner(spinner: Spinner, monthIndex: Int?, year: Int?) {
        val days = mutableListOf("DD")

        // Convert spinner index to real month number
        val realMonth = if (monthIndex != null && monthIndex > 0) monthIndex else 1

        val daysInMonth = when (realMonth) {
            2 -> if (year != null && year % 4 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        days += (1..daysInMonth).map { it.toString() }

        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            days
        )
    }


    private fun calculateAge(day: Int, month: Int, year: Int): Int {
        val birthDate = LocalDate.of(year, month, day)
        return Period.between(birthDate, LocalDate.now()).years
    }
}
