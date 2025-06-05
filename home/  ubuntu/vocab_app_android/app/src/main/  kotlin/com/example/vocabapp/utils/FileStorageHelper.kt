package com.example.vocabapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

object FileStorageHelper {

    private const val IMAGE_DIRECTORY = "word_images"
    private const val TAG = "FileStorageHelper"

    // Function to save an image from a Bitmap to internal storage
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        val directory = File(context.filesDir, IMAGE_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val filename = "${UUID.randomUUID()}.jpg"
        val file = File(directory, filename)

        return try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            Log.d(TAG, "Image saved successfully: ${file.absolutePath}")
            filename // Return only the filename, not the full path
        } catch (e: IOException) {
            Log.e(TAG, "Error saving image: ${e.message}", e)
            null
        }
    }

    // Function to save an image from a Uri (e.g., from gallery or camera) to internal storage
    fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                saveBitmapToInternalStorage(context, bitmap)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading image from URI: ${e.message}", e)
            null
        }
    }


    // Function to load an image from internal storage as a Bitmap
    fun loadBitmapFromInternalStorage(context: Context, filename: String): Bitmap? {
        val directory = File(context.filesDir, IMAGE_DIRECTORY)
        val file = File(directory, filename)

        return if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image: ${e.message}", e)
                null
            }
        } else {
            Log.w(TAG, "Image file not found: ${file.absolutePath}")
            null
        }
    }

    // Function to get the File object for an image
    fun getImageFile(context: Context, filename: String): File? {
         val directory = File(context.filesDir, IMAGE_DIRECTORY)
         val file = File(directory, filename)
         return if (file.exists()) file else null
    }

    // Function to delete an image from internal storage
    fun deleteImageFromInternalStorage(context: Context, filename: String): Boolean {
        val directory = File(context.filesDir, IMAGE_DIRECTORY)
        val file = File(directory, filename)

        return if (file.exists()) {
            try {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Image deleted successfully: ${file.absolutePath}")
                } else {
                    Log.w(TAG, "Failed to delete image: ${file.absolutePath}")
                }
                deleted
            } catch (e: SecurityException) {
                Log.e(TAG, "Error deleting image due to security exception: ${e.message}", e)
                false
            }
        } else {
            Log.w(TAG, "Image file not found for deletion: ${file.absolutePath}")
            false // File doesn't exist, consider it 'deleted' or handle as needed
        }
    }
}

