package com.example.vocabapp.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.*
import com.example.vocabapp.data.VocabDatabase
import com.example.vocabapp.data.Word
import com.example.vocabapp.repository.WordRepository
import com.example.vocabapp.utils.FileStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class WordEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository
    private val _currentPhotoBitmap = MutableLiveData<Bitmap?>()
    val currentPhotoBitmap: LiveData<Bitmap?> = _currentPhotoBitmap

    private var currentPhotoFilename: String? = null
    private var existingWordId: Int? = null // To track if we are editing an existing word
    private var existingWord: Word? = null // Store the full existing word for updates

    init {
        val wordsDao = VocabDatabase.getDatabase(application).wordDao()
        repository = WordRepository(wordsDao)
    }

    fun loadWord(wordId: Int) {
        existingWordId = wordId
        viewModelScope.launch {
            repository.getWordById(wordId).collect { word ->
                existingWord = word
                // Pre-fill data if needed (handled in Fragment observing this)
                word?.photoFilename?.let {
                    loadAndSetBitmap(getApplication(), it)
                }
            }
        }
    }

    fun setPhotoUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save the new image from URI and update the bitmap LiveData
            val filename = FileStorageHelper.saveUriToInternalStorage(context, uri)
            if (filename != null) {
                // Delete old photo if it exists and is different
                existingWord?.photoFilename?.let {
                    if (it != filename) {
                        FileStorageHelper.deleteImageFromInternalStorage(context, it)
                    }
                }
                currentPhotoFilename = filename
                loadAndSetBitmap(context, filename)
            } else {
                // Handle error - maybe show a toast
            }
        }
    }

    fun setPhotoBitmap(context: Context, bitmap: Bitmap) {
         viewModelScope.launch(Dispatchers.IO) {
            // Save the new image from Bitmap and update the bitmap LiveData
            val filename = FileStorageHelper.saveBitmapToInternalStorage(context, bitmap)
            if (filename != null) {
                 // Delete old photo if it exists and is different
                existingWord?.photoFilename?.let {
                    if (it != filename) {
                        FileStorageHelper.deleteImageFromInternalStorage(context, it)
                    }
                }
                currentPhotoFilename = filename
                _currentPhotoBitmap.postValue(bitmap) // Update LiveData on main thread
            } else {
                // Handle error
            }
        }
    }

    private fun loadAndSetBitmap(context: Context, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = FileStorageHelper.loadBitmapFromInternalStorage(context, filename)
            _currentPhotoBitmap.postValue(bitmap)
            currentPhotoFilename = filename // Ensure filename is set when loading existing
        }
    }

    fun saveWord(wordText: String, definition: String, example: String?) {
        if (wordText.isBlank() || definition.isBlank()) {
            // Handle validation error - maybe expose a LiveData for errors
            return
        }

        val timestamp = System.currentTimeMillis()

        viewModelScope.launch {
            if (existingWordId != null && existingWord != null) {
                // Update existing word
                val updatedWord = existingWord!!.copy(
                    wordText = wordText,
                    definition = definition,
                    exampleSentence = example?.takeIf { it.isNotBlank() },
                    photoFilename = currentPhotoFilename, // Use the latest filename
                    lastModified = timestamp,
                    syncStatus = "needs_sync" // Mark as needing sync
                )
                repository.update(updatedWord)
            } else {
                // Insert new word
                val newWord = Word(
                    wordText = wordText,
                    definition = definition,
                    exampleSentence = example?.takeIf { it.isNotBlank() },
                    photoFilename = currentPhotoFilename,
                    googleDriveFileId = null, // Will be set during sync
                    lastModified = timestamp,
                    syncStatus = "local_only" // New words are local only initially
                )
                repository.insert(newWord)
            }
            // Potentially navigate back or clear fields via LiveData event
        }
    }

    // Optional: Clear photo when user removes it
    fun clearPhoto(context: Context) {
        currentPhotoFilename?.let {
            FileStorageHelper.deleteImageFromInternalStorage(context, it)
        }
        currentPhotoFilename = null
        _currentPhotoBitmap.value = null
    }
}

// ViewModel Factory to pass Application context
class WordEntryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordEntryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
