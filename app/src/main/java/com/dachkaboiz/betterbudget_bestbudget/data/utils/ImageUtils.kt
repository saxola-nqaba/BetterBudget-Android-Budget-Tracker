package com.dachkaboiz.betterbudget_bestbudget.data.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build

object ImageUtils {
    // Function to create an image file and return its URI. Takes the app context as a parameter.

    // Code Attribution
    // This method was taken from Stack Overflow
    // https://stackoverflow.com/questions/38200282
    // CommonsWare
    // https://stackoverflow.com/users/115145/commonsware
    fun createImageFile(context: Context): Uri {
        // Creates a timestamp using the current date and time.
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // Defines the directory where images will be stored.
        val storageDir = File(context.getExternalFilesDir("Pictures"), "captured_images")

        // Checks if the folder exists, if not, create it
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        // Creates a new File object for the image.
        val imageFile = File(storageDir, "IMG_$timeStamp.jpg")

        // Converts the File into a secure content URI using FileProvider.
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",   // This must match your AndroidManifest.xml
            imageFile
        )
    }

    // Code Attribution
    // This method was taken from Stack Overflow
    // https://stackoverflow.com/questions/56904485
    // Android Admirer
    // https://stackoverflow.com/users/6377726/android-admirer
    fun saveImageToGallery(context: Context, imageFile: File) {
        val filename = "Receipt_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            // On Android 10 and above, we use the "Pictures" collection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BetterBudget")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { targetUri ->
            resolver.openOutputStream(targetUri).use { outputStream ->
                imageFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream!!)
                }
            }

            // On Android 10+, we tell the system we are done writing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)
            }
        }
    }
}