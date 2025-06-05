package com.example.vocabapp.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.vocabapp.data.VocabDatabase
import com.example.vocabapp.data.Word
import com.example.vocabapp.repository.WordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class WordListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository

    // Using StateFlow for the search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Flow of words, reacts to search query changes
    val words: StateFlow<List<Word>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allWords
            } else {
                repository.searchWords(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep flow active for 5s after last subscriber gone
            initialValue = emptyList()
        )

    init {
        val wordsDao = VocabDatabase.getDatabase(application).wordDao()
        repository = WordRepository(wordsDao)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Optional: Add delete functionality if needed directly from the list (e.g., swipe to delete)
    fun deleteWord(word: Word) = viewModelScope.launch {
        // Need to also delete the associated photo file
        word.photoFilename?.let {
            com.example.vocabapp.utils.FileStorageHelper.deleteImageFromInternalStorage(getApplication(), it)
        }
        repository.delete(word)
    }
}

// ViewModel Factory
class WordListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordListViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
