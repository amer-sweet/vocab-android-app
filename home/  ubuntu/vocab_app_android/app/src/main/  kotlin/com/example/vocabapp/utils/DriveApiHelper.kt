package com.example.vocabapp.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections

object DriveApiHelper {

    private const val TAG = "DriveApiHelper"
    private const val APP_NAME = "VocabApp"
    private const val APP_DATA_FOLDER = "appDataFolder"
    const val METADATA_FILENAME = "vocabulary_data.json"

    // Function to get Drive service instance
    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).setSelectedAccount(account.account)

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    // --- Metadata File Operations ---

    // Find or create the metadata file in the AppData folder
    suspend fun findOrCreateMetadataFile(context: Context, account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        try {
            val files = driveService.files().list()
                .setSpaces(APP_DATA_FOLDER)
                .setFields("files(id, name)")
                .setQ("name = 	'$METADATA_FILENAME'")
                .execute()

            val existingFile = files.files.firstOrNull()
            if (existingFile != null) {
                Log.d(TAG, "Metadata file found with ID: ${existingFile.id}")
                existingFile.id
            } else {
                Log.d(TAG, "Metadata file not found, creating new one.")
                val fileMetadata = File().apply {
                    name = METADATA_FILENAME
                    parents = listOf(APP_DATA_FOLDER)
                }
                val createdFile = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute()
                Log.d(TAG, "Metadata file created with ID: ${createdFile.id}")
                createdFile.id
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error finding/creating metadata file: ${e.message}", e)
            null
        }
    }

    // Download the metadata file content
    suspend fun downloadMetadataFile(context: Context, account: GoogleSignInAccount, fileId: String): String? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        try {
            ByteArrayOutputStream().use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                val content = outputStream.toString()
                Log.d(TAG, "Metadata file downloaded successfully (ID: $fileId)")
                content
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error downloading metadata file (ID: $fileId): ${e.message}", e)
            null
        }
    }

    // Upload/Update the metadata file content
    suspend fun uploadMetadataFile(context: Context, account: GoogleSignInAccount, fileId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        try {
            // Create a temporary file to upload
            val tempFile = java.io.File.createTempFile("metadata", ".json", context.cacheDir)
            tempFile.writeText(content)
            val mediaContent = FileContent("application/json", tempFile)

            driveService.files().update(fileId, null, mediaContent).execute()
            tempFile.delete() // Clean up temp file
            Log.d(TAG, "Metadata file uploaded successfully (ID: $fileId)")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error uploading metadata file (ID: $fileId): ${e.message}", e)
            false
        }
    }

     // Get metadata file modification time
    suspend fun getFileModificationTime(context: Context, account: GoogleSignInAccount, fileId: String): Long? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        try {
            val file = driveService.files().get(fileId).setFields("modifiedTime").execute()
            file.modifiedTime?.value
        } catch (e: IOException) {
            Log.e(TAG, "Error getting modification time for file (ID: $fileId): ${e.message}", e)
            null
        }
    }

    // --- Photo File Operations ---

    // Upload a photo file
    suspend fun uploadPhotoFile(context: Context, account: GoogleSignInAccount, localFile: java.io.File): String? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        try {
            val fileMetadata = File().apply {
                name = localFile.name // Use the local filename
                parents = listOf(APP_DATA_FOLDER)
            }
            val mediaContent = FileContent("image/jpeg", localFile) // Assuming JPEG

            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            Log.d(TAG, "Photo file uploaded successfully: ${localFile.name}, ID: ${uploadedFile.id}")
            uploadedFile.id
        } catch (e: IOException) {
            Log.e(TAG, "Error uploading photo file ${localFile.name}: ${e.message}", e)
            null
        }
    }

    // Download a photo file
    suspend fun downloadPhotoFile(context: Context, account: GoogleSignInAccount, fileId: String, destinationFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        try {
            FileOutputStream(destinationFile).use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }
            Log.d(TAG, "Photo file downloaded successfully: ID: $fileId to ${destinationFile.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error downloading photo file ID: $fileId: ${e.message}", e)
            // Clean up potentially partially downloaded file
            if (destinationFile.exists()) destinationFile.delete()
            false
        }
    }

    // Delete a file by ID
    suspend fun deleteDriveFile(context: Context, account: GoogleSignInAccount, fileId: String): Boolean = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context, account)
        try {
            driveService.files().delete(fileId).execute()
            Log.d(TAG, "File deleted successfully from Drive: ID: $fileId")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error deleting file from Drive ID: $fileId: ${e.message}", e)
            false
        }
    }
}
