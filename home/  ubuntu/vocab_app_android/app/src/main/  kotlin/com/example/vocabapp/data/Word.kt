package com.example.vocabapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single vocabulary word entry in the database.
 *
 * @property id The unique identifier for the word entry (auto-generated).
 * @property wordText The vocabulary word itself.
 * @property definition The definition or translation of the word.
 * @property exampleSentence An optional example sentence using the word.
 * @property photoFilename The filename of the associated photo stored locally (if any).
 * @property googleDriveFileId The Google Drive file ID for the associated photo (if synced).
 * @property lastModified Timestamp (milliseconds since epoch) of the last modification, used for sync logic.
 * @property syncStatus Indicates the synchronization status (e.g., "local_only", "synced", "needs_sync").
 */
@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val wordText: String,
    val definition: String,
    val exampleSentence: String?,
    val photoFilename: String?, // Stores the filename of the locally saved image
    val googleDriveFileId: String?, // Stores the Google Drive file ID for the photo
    val lastModified: Long, // Timestamp for sync logic
    var syncStatus: String // e.g., "local_only", "synced", "needs_sync"
)

