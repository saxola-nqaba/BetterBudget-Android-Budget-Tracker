package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.adapter.PhotoAdapter
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Photo
import com.dachkaboiz.betterbudget_bestbudget.databinding.FragmentPhotoGalleryBinding

import kotlinx.coroutines.launch
import java.io.File

class PhotoGalleryFragment : Fragment(R.layout.fragment_photo_gallery) {
    private var _binding: FragmentPhotoGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private var photoList: List<Photo> = emptyList()

    // Placeholder for the logged-in user's email.
    // In a full implementation, you would get this from SharedPreferences or arguments.
    private val currentUserEmail: String = "user@example.com"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPhotoGalleryBinding.bind(view)

        db = AppDatabase.getDatabase(requireContext())

        // Initial load of photos for the current user
        loadAllPhotos()

        // Search logic
        binding.btnSearch.setOnClickListener {
            searchPhotos()
        }

        // Reset logic: clears the search bar and reloads all user photos
        binding.btnShowAll.setOnClickListener {
            binding.etSearch.text?.clear()
            loadAllPhotos()
        }

        // Navigation back to the capture screen
        binding.btnBackToCapture.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Fetches all photos belonging to the current user from the database.
     */
    private fun loadAllPhotos() {
        lifecycleScope.launch {
            // We use the email to ensure the user only sees their own photos
            photoList = db.photoDao().getPhotosByUser(currentUserEmail)

            // Note: You will need to create a PhotoAdapter class to handle the ListView display
            binding.listViewPhotos.adapter = PhotoAdapter(requireContext(), photoList) { photo ->
                confirmDelete(photo)
            }
        }
    }

    /**
     * Filters the user's photos based on the name or description entered.
     */
    private fun searchPhotos() {
        val query = binding.etSearch.text.toString().trim()

        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Enter search text", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Search DAO method uses the email + the keyword
            photoList = db.photoDao().searchPhotos(currentUserEmail, query)

            binding.listViewPhotos.adapter = PhotoAdapter(requireContext(), photoList) { photo ->
                confirmDelete(photo)
            }
        }
    }

    /**
     * Standard confirmation dialog to prevent accidental deletions.
     */
    private fun confirmDelete(photo: Photo) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete '${photo.imageName}'?")
            .setPositiveButton("Yes") { _, _ ->
                deletePhoto(photo)
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Handles the two-step deletion process: Physical File and Database Record.
     */
    private fun deletePhoto(photo: Photo) {
        lifecycleScope.launch {
            try {
                // 1. Delete the physical image file from the phone's storage
                val uri = android.net.Uri.parse(photo.imageUri)
                val path = uri.path
                if (!path.isNullOrEmpty()) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // If file deletion fails, we still want to remove the database record
            }

            // 2. Delete the record from the Room database
            db.photoDao().deletePhoto(photo)

            Toast.makeText(requireContext(), "Photo deleted", Toast.LENGTH_SHORT).show()

            // Refresh the list to show it's gone
            loadAllPhotos()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}