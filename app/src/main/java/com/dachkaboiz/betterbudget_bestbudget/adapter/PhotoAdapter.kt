package com.dachkaboiz.betterbudget_bestbudget.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Photo

class PhotoAdapter (
    context: Context,
    private val photos: List<Photo>,
    private val onDelete: (Photo) -> Unit
) : ArrayAdapter<Photo>(context, 0, photos){
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Inflate the layout (item_photo.xml) if a recycled view isn't available
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_photo, parent, false)

        // Find the UI components using the IDs defined in item_photo.xml
        val imgThumb = view.findViewById<ImageView>(R.id.imgThumb)
        val tvName = view.findViewById<TextView>(R.id.tvPhotoName)
        val tvDescription = view.findViewById<TextView>(R.id.tvPhotoDescription)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDelete)

        // Get the data for the current position in the list
        val photo = photos[position]

        // Bind the data to the views
        tvName.text = photo.imageName
        tvDescription.text = photo.imageDescription

        // Convert the String URI from the database back into a Uri object to display the image
        imgThumb.setImageURI(Uri.parse(photo.imageUri))

        // Set up the delete button click listener
        btnDelete.setOnClickListener {
            // This triggers the confirmDelete(photo) function inside your GalleryFragment
            onDelete(photo)
        }

        return view
    }
}