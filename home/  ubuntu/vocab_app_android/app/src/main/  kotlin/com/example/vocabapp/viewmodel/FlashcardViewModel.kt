package com.example.vocabapp.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.*
import com.example.vocabapp.data.VocabDatabase
import com.example.vocabapp.data.Word
import com.example.vocabapp.repository.WordRepository
import com.example.vocabapp.utils.FileStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

data class FlashcardUiState(
    val currentWord: Word? = null,
    val currentBitmap: Bitmap? = null,
    val isFrontVisible: Boolean = true,
    val currentIndex: Int = 0,
    val totalWords: Int = 0,
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false
)

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository

    private val _allWords = MutableStateFlow<List<Word>>(emptyList())
    private val _currentIndex = MutableStateFlow(0)
    private val _isFrontVisible = MutableStateFlow(true)
    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)

    val uiState: StateFlow<FlashcardUiState> = combine(
        _allWords,
        _currentIndex,
        _isFrontVisible,
        _currentBitmap
    ) { words, index, isFront, bitmap ->
        val currentWord = words.getOrNull(index)
        FlashcardUiState(
            currentWord = currentWord,
            currentBitmap = bitmap,
            isFrontVisible = isFront,
            currentIndex = index,
            totalWords = words.size,
            canGoPrevious = index > 0,
            canGoNext = index < words.size - 1
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FlashcardUiState()
    )

    init {
        val wordsDao = VocabDatabase.getDatabase(application).wordDao()
        repository = WordRepository(wordsDao)
        loadAllWords()
    }

    private fun loadAllWords() {
        viewModelScope.launch {
            repository.allWords.collect { words ->
                _allWords.value = words
                // Reset index if list changes significantly, or handle appropriately
                if (_currentIndex.value >= words.size && words.isNotEmpty()) {
                    _currentIndex.value = 0
                }
                loadBitmapForCurrentIndex()
            }
        }
    }

    private fun loadBitmapForCurrentIndex() {
        val words = _allWords.value
        val index = _currentIndex.value
        val word = words.getOrNull(index)

        if (word?.photoFilename != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val bitmap = FileStorageHelper.loadBitmapFromInternalStorage(getApplication(), word.photoFilename)
                _currentBitmap.value = bitmap
            }
        } else {
            _currentBitmap.value = null
        }
    }

    fun flipCard() {
        _isFrontVisible.value = !_isFrontVisible.value
    }

    fun nextCard() {
        val nextIndex = _currentIndex.value + 1
        if (nextIndex < _allWords.value.size) {
            _currentIndex.value = nextIndex
            _isFrontVisible.value = true // Show front by default on new card
            loadBitmapForCurrentIndex()
        }
    }

    fun previousCard() {
        val prevIndex = _currentIndex.value - 1
        if (prevIndex >= 0) {
            _currentIndex.value = prevIndex
            _isFrontVisible.value = true // Show front by default on new card
            loadBitmapForCurrentIndex()
        }
    }
}

// Simple combine function for StateFlows (can be placed in a utils file)
fun <T1, T2, T3, T4, R> combine(
    flow1: StateFlow<T1>,
    flow2: StateFlow<T2>,
    flow3: StateFlow<T3>,
    flow4: StateFlow<T4>,
    transform: suspend (T1, T2, T3, T4) -> R
): StateFlow<R> = kotlinx.coroutines.flow.combine(flow1, flow2, flow3, flow4) { args: Array<*> ->
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4
    )
}.stateIn( // Need a scope, typically viewModelScope
    scope = kotlinx.coroutines.GlobalScope, // WARNING: Use a proper scope in real app
    started = SharingStarted.Eagerly,
    initialValue = runBlocking { transform(flow1.value, flow2.value, flow3.value, flow4.value) } // Calculate initial value
)

// ViewModel Factory
class FlashcardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashcardViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
