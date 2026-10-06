package com.dachkaboiz.betterbudget_bestbudget.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.Photo
import com.dachkaboiz.betterbudget_bestbudget.databinding.FragmentPhotoCaptureBinding
import com.dachkaboiz.betterbudget_bestbudget.data.utils.ImageUtils
import kotlinx.coroutines.launch

class PhotoCaptureFragment: Fragment(R.layout.fragment_photo_capture) {

    private var _binding: FragmentPhotoCaptureBinding? = null
    private val binding get() = _binding!!

    // This holds the temporary URI of the image taken by the camera
    private var currentImageUri: Uri? = null


    private val currentUserEmail: String = "user@example.com"

    //PERMISSION LAUNCHER
    // This handles the pop-up asking the user for Camera permission.
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }

    }

    //CAMERA LAUNCHER
    // This opens the system camera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentImageUri != null) {
            // If successful, update the ImageView in your XML (imagePreview) to show the photo
            binding.imagePreview.setImageURI(currentImageUri)
        } else {
            Toast.makeText(requireContext(), "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPhotoCaptureBinding.bind(view)

        // Set up the Take Photo button
        binding.btnTakePhoto.setOnClickListener {
            checkPermissionAndLaunchCamera()
        }

        // Set up the Save Photo button
        binding.btnSave.setOnClickListener {
            savePhotoToDatabase()
        }

        // Set up Open Gallery to navigate to the list of saved photos
        binding.btnOpenGallery.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, PhotoGalleryFragment()) // Ensure R.id matches your MainActivity container
                .addToBackStack(null)
                .commit()
        }
    }

    private fun checkPermissionAndLaunchCamera() {
        // Check if we already have permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            // If not, ask the user
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        // Use the ImageUtils to create a file location for the new photo
        currentImageUri = ImageUtils.createImageFile(requireContext())

        // Pass that location to the camera launcher
        takePictureLauncher.launch(currentImageUri)
    }

    private fun savePhotoToDatabase() {
        val name = binding.etImageName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val uriString = currentImageUri?.toString()

        // Validation: Ensure a photo was actually taken
        if (uriString == null) {
            Toast.makeText(requireContext(), "Please take a photo first", Toast.LENGTH_SHORT).show()
            return
        }

        // Validation: Ensure the user gave it a name (required for your Search feature)
        if (name.isEmpty()) {
            binding.etImageName.error = "Name is required"
            return
        }

        // Create the Photo object using your data class
        val photoRecord = Photo(
            userEmail = currentUserEmail, // Links photo to the specific user
            imageName = name,
            imageDescription = description,
            imageUri = uriString
            // dateCaptured is automatically set to System.currentTimeMillis() in the data class
        )

        // Use lifecycleScope.launch to run the database insert on a background thread
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                db.photoDao().insertPhoto(photoRecord)

                Toast.makeText(requireContext(), "Photo saved successfully!", Toast.LENGTH_SHORT).show()

                // Send data back to add expense fragment
                val resultBundle = Bundle().apply {
                    putString("image_uri", uriString)
                    putString("image_name", name)
                    putString("image_description", description)
                }
                parentFragmentManager.setFragmentResult("camera_request", resultBundle)

                // Close the camera screen and go back to the form
                parentFragmentManager.popBackStack()

                resetUI()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error saving to database", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Clears the screen so the user can take another photo
    private fun resetUI() {
        binding.etImageName.text?.clear()
        binding.etDescription.text?.clear()
        binding.imagePreview.setImageResource(android.R.drawable.ic_menu_camera)
        currentImageUri = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Critical to prevent memory leaks
    }
}