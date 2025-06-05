package com.example.vocabapp.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.vocabapp.data.VocabDatabase
import com.example.vocabapp.data.Word
import com.example.vocabapp.repository.WordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import kotlin.random.Random

data class QuizQuestion(
    val wordToDefine: Word,
    val options: List<String>, // List of definitions, one is correct
    val correctAnswer: String // The correct definition
)

data class QuizUiState(
    val currentQuestion: QuizQuestion? = null,
    val questionNumber: Int = 0, // 1-based index for display
    val totalQuestions: Int = 0,
    val score: Int = 0,
    val selectedAnswer: String? = null,
    val feedback: String? = null, // e.g., "Correct!", "Incorrect. The answer was..."
    val isAnswerSubmitted: Boolean = false,
    val isQuizComplete: Boolean = false
)

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository
    private var allWords: List<Word> = emptyList()
    private var quizQuestions: List<QuizQuestion> = emptyList()
    private var currentQuestionIndex = -1
    private var currentScore = 0

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    companion object {
        private const val QUIZ_SIZE = 10 // Number of questions per quiz
        private const val NUM_OPTIONS = 4 // Number of choices per question
    }

    init {
        val wordsDao = VocabDatabase.getDatabase(application).wordDao()
        repository = WordRepository(wordsDao)
        loadWordsAndStartQuiz()
    }

    private fun loadWordsAndStartQuiz() {
        viewModelScope.launch {
            repository.allWords.firstOrNull()?.let { words ->
                allWords = words
                if (allWords.size >= NUM_OPTIONS) { // Need enough words for options
                    generateQuiz()
                    nextQuestion()
                } else {
                    // Handle state where there are not enough words for a quiz
                    _uiState.value = QuizUiState(isQuizComplete = true, feedback = "Need at least $NUM_OPTIONS words to start a quiz.")
                }
            }
        }
    }

    private fun generateQuiz() {
        if (allWords.size < NUM_OPTIONS) return

        val shuffledWords = allWords.shuffled()
        val selectedWords = shuffledWords.take(minOf(QUIZ_SIZE, allWords.size))
        currentScore = 0

        quizQuestions = selectedWords.map { correctWord ->
            val incorrectOptions = allWords
                .filter { it.id != correctWord.id } // Exclude the correct word
                .shuffled()
                .take(NUM_OPTIONS - 1)
                .map { it.definition }

            val options = (incorrectOptions + correctWord.definition).shuffled()

            QuizQuestion(
                wordToDefine = correctWord,
                options = options,
                correctAnswer = correctWord.definition
            )
        }
    }

    fun submitAnswer(selectedOption: String) {
        val currentState = _uiState.value
        val currentQuestion = currentState.currentQuestion ?: return

        val isCorrect = selectedOption == currentQuestion.correctAnswer
        if (isCorrect) {
            currentScore++
        }

        _uiState.value = currentState.copy(
            selectedAnswer = selectedOption,
            isAnswerSubmitted = true,
            score = currentScore,
            feedback = if (isCorrect) "Correct!" else "Incorrect. The answer was: ${currentQuestion.correctAnswer}"
        )
    }

    fun nextQuestion() {
        currentQuestionIndex++
        if (currentQuestionIndex < quizQuestions.size) {
            _uiState.value = QuizUiState(
                currentQuestion = quizQuestions[currentQuestionIndex],
                questionNumber = currentQuestionIndex + 1,
                totalQuestions = quizQuestions.size,
                score = currentScore, // Carry over score
                isAnswerSubmitted = false,
                selectedAnswer = null,
                feedback = null,
                isQuizComplete = false
            )
        } else {
            // Quiz finished
            _uiState.value = _uiState.value.copy(
                isQuizComplete = true,
                currentQuestion = null,
                feedback = "Quiz Complete! Your score: $currentScore / ${quizQuestions.size}"
            )
        }
    }

    fun restartQuiz() {
        currentQuestionIndex = -1
        currentScore = 0
        if (allWords.size >= NUM_OPTIONS) {
             generateQuiz()
             nextQuestion()
        } else {
             _uiState.value = QuizUiState(isQuizComplete = true, feedback = "Need at least $NUM_OPTIONS words to start a quiz.")
        }
    }
}

// ViewModel Factory
class QuizViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuizViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
