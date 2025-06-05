package com.example.vocabapp.sync

import android.content.Context
import android.util.Log
import com.example.vocabapp.data.Word
import com.example.vocabapp.repository.WordRepository
import com.example.vocabapp.utils.DriveApiHelper
import com.example.vocabapp.utils.FileStorageHelper
import com.example.vocabapp.utils.GoogleSignInHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR, NO_ACCOUNT }

data class SyncResult(val status: SyncStatus, val message: String? = null)

class SyncManager(private val context: Context, private val repository: WordRepository) {

    private val gson = Gson()
    private val _syncStatus = MutableStateFlow(SyncResult(SyncStatus.IDLE))
    val syncStatus: StateFlow<SyncResult> = _syncStatus.asStateFlow()

    companion object {
        private const val TAG = "SyncManager"
    }

    suspend fun performSync() = withContext(Dispatchers.IO) {
        val account = GoogleSignInHelper.getLastSignedInAccount(context)
        if (account == null) {
            _syncStatus.value = SyncResult(SyncStatus.NO_ACCOUNT, "User not signed in.")
            Log.w(TAG, "Sync failed: User not signed in.")
            return@withContext
        }

        _syncStatus.value = SyncResult(SyncStatus.SYNCING)
        Log.i(TAG, "Starting sync process...")

        try {
            val metadataFileId = DriveApiHelper.findOrCreateMetadataFile(context, account)
            if (metadataFileId == null) {
                throw IOException("Could not find or create metadata file on Drive.")
            }

            val driveModTime = DriveApiHelper.getFileModificationTime(context, account, metadataFileId)
            val localWordsNeedingSync = repository.getWordsNeedingSync()
            val localLastSyncTime = 0L // TODO: Store and retrieve last sync timestamp locally

            // Determine sync direction (simplified: check if Drive is newer or if local changes exist)
            val driveIsNewer = driveModTime != null && driveModTime > localLastSyncTime
            val hasLocalChanges = localWordsNeedingSync.isNotEmpty()

            var remoteWordsMap: Map<Int, Word> = emptyMap()
            var needsUpload = false

            // 1. Download from Drive if it's newer
            if (driveIsNewer) {
                Log.d(TAG, "Drive metadata is newer, downloading...")
                val remoteJson = DriveApiHelper.downloadMetadataFile(context, account, metadataFileId)
                if (remoteJson != null) {
                    val type = object : TypeToken<List<Word>>() {}.type
                    val remoteWords: List<Word> = gson.fromJson(remoteJson, type) ?: emptyList()
                    remoteWordsMap = remoteWords.associateBy { it.id }
                    Log.d(TAG, "Downloaded ${remoteWords.size} words from Drive.")

                    // Merge remote changes into local DB
                    mergeRemoteChanges(remoteWords, account)
                    // TODO: Update localLastSyncTime to driveModTime
                } else {
                    Log.w(TAG, "Failed to download remote metadata, proceeding with local changes only.")
                }
            }

            // 2. Upload local changes if any exist
            if (hasLocalChanges) {
                Log.d(TAG, "Local changes detected, preparing upload...")
                uploadLocalChanges(localWordsNeedingSync, remoteWordsMap, account)
                needsUpload = true
            }

            // 3. If we uploaded local changes, update the metadata file on Drive
            if (needsUpload) {
                 Log.d(TAG, "Uploading updated metadata file to Drive...")
                 val allLocalWords = repository.allWords.first() // Get current state after potential merge
                 val updatedJson = gson.toJson(allLocalWords)
                 val uploadSuccess = DriveApiHelper.uploadMetadataFile(context, account, metadataFileId, updatedJson)
                 if (!uploadSuccess) {
                     throw IOException("Failed to upload updated metadata file.")
                 }
                 // TODO: Update localLastSyncTime to current time
            }

            _syncStatus.value = SyncResult(SyncStatus.SUCCESS, "Sync completed successfully.")
            Log.i(TAG, "Sync process finished successfully.")

        } catch (e: Exception) {
            _syncStatus.value = SyncResult(SyncStatus.ERROR, "Sync failed: ${e.message}")
            Log.e(TAG, "Sync failed: ${e.message}", e)
        }
    }

    private suspend fun mergeRemoteChanges(remoteWords: List<Word>, account: GoogleSignInAccount) {
        val localWordsMap = repository.allWords.first().associateBy { it.id }

        for (remoteWord in remoteWords) {
            val localWord = localWordsMap[remoteWord.id]

            if (localWord == null) {
                // New word from Drive
                Log.d(TAG, "Merging new word from Drive: ${remoteWord.wordText}")
                downloadPhotoIfNeeded(remoteWord, account)
                repository.insert(remoteWord.copy(syncStatus = "synced"))
            } else if (remoteWord.lastModified > localWord.lastModified) {
                // Updated word from Drive
                Log.d(TAG, "Merging updated word from Drive: ${remoteWord.wordText}")
                downloadPhotoIfNeeded(remoteWord, account)
                repository.update(remoteWord.copy(syncStatus = "synced"))
            }
            // Else: Local word is newer or same, will be handled during upload phase if needed
        }

        // Handle deletions (words present locally but not remotely - simplified)
        // A more robust approach would track deletions explicitly
        val remoteIds = remoteWords.map { it.id }.toSet()
        localWordsMap.values.forEach { localWord ->
            if (localWord.id !in remoteIds && localWord.syncStatus == "synced") {
                 // Word was likely deleted on another device
                 Log.d(TAG, "Deleting local word removed from Drive: ${localWord.wordText}")
                 localWord.photoFilename?.let { FileStorageHelper.deleteImageFromInternalStorage(context, it) }
                 repository.delete(localWord)
            }
        }
    }

    private suspend fun downloadPhotoIfNeeded(word: Word, account: GoogleSignInAccount) {
        if (word.photoFilename != null && word.googleDriveFileId != null) {
            val localImageFile = FileStorageHelper.getImageFile(context, word.photoFilename)
            if (localImageFile == null || !localImageFile.exists()) {
                Log.d(TAG, "Downloading photo for word '${word.wordText}' from Drive ID: ${word.googleDriveFileId}")
                val destinationFile = File(File(context.filesDir, "word_images"), word.photoFilename)
                destinationFile.parentFile?.mkdirs() // Ensure directory exists
                val success = DriveApiHelper.downloadPhotoFile(context, account, word.googleDriveFileId, destinationFile)
                if (!success) {
                    Log.w(TAG, "Failed to download photo ${word.photoFilename} (Drive ID: ${word.googleDriveFileId})")
                    // Handle error - maybe mark word as needing photo download later?
                }
            }
        }
    }

    private suspend fun uploadLocalChanges(localWordsNeedingSync: List<Word>, remoteWordsMap: Map<Int, Word>, account: GoogleSignInAccount) {
        for (localWord in localWordsNeedingSync) {
            val remoteWord = remoteWordsMap[localWord.id]

            // Check if local change is still relevant (remote might be newer)
            if (remoteWord != null && remoteWord.lastModified >= localWord.lastModified) {
                Log.d(TAG, "Skipping upload for '${localWord.wordText}', remote version is newer or same.")
                // Mark local as synced if content matches remote exactly?
                if (localWord.copy(syncStatus = "synced") == remoteWord.copy(syncStatus = "synced")) {
                     repository.update(localWord.copy(syncStatus = "synced"))
                }
                continue
            }

            Log.d(TAG, "Uploading changes for word: ${localWord.wordText}")
            var wordToUpdate = localWord

            // Upload photo if it exists locally but not on Drive (or needs update)
            if (localWord.photoFilename != null) {
                val localImageFile = FileStorageHelper.getImageFile(context, localWord.photoFilename)
                if (localImageFile != null && localImageFile.exists()) {
                    // Check if photo needs upload (new photo or no drive ID yet)
                    if (localWord.googleDriveFileId == null /* || photo content changed - harder to check */) {
                        Log.d(TAG, "Uploading photo ${localWord.photoFilename} to Drive...")
                        val driveFileId = DriveApiHelper.uploadPhotoFile(context, account, localImageFile)
                        if (driveFileId != null) {
                            wordToUpdate = localWord.copy(googleDriveFileId = driveFileId)
                        } else {
                            Log.w(TAG, "Failed to upload photo ${localWord.photoFilename}")
                            // Proceed without Drive ID, maybe retry later?
                        }
                    }
                } else {
                     Log.w(TAG, "Local photo file not found for upload: ${localWord.photoFilename}")
                     // Photo was deleted locally? Clear Drive ID if exists?
                     if(localWord.googleDriveFileId != null) {
                         // TODO: Delete photo from Drive?
                         wordToUpdate = localWord.copy(googleDriveFileId = null, photoFilename = null)
                     }
                }
            } else if (localWord.googleDriveFileId != null) {
                 // Photo removed locally, delete from Drive
                 Log.d(TAG, "Deleting photo ${localWord.googleDriveFileId} from Drive...")
                 DriveApiHelper.deleteDriveFile(context, account, localWord.googleDriveFileId)
                 wordToUpdate = localWord.copy(googleDriveFileId = null)
            }

            // Update local word status after potential photo upload/deletion
            repository.update(wordToUpdate.copy(syncStatus = "synced"))
        }

        // TODO: Handle Deletions - Need a way to track deleted items locally (e.g., tombstone table)
    }
}
