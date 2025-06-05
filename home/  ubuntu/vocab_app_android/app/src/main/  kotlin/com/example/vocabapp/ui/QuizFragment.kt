package com.example.vocabapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.vocabapp.R // Ensure R is generated
import com.example.vocabapp.databinding.FragmentQuizBinding // Use ViewBinding
import com.example.vocabapp.viewmodel.QuizUiState
import com.example.vocabapp.viewmodel.QuizViewModel
import com.example.vocabapp.viewmodel.QuizViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by viewModels {
        QuizViewModelFactory(requireActivity().application)
    }

    private val radioButtons: List<RadioButton> by lazy {
        listOf(
            binding.option1RadioButton,
            binding.option2RadioButton,
            binding.option3RadioButton,
            binding.option4RadioButton
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtonClickListeners()
        observeUiState()
    }

    private fun setupButtonClickListeners() {
        binding.submitButton.setOnClickListener {
            val selectedRadioButtonId = binding.optionsRadioGroup.checkedRadioButtonId
            if (selectedRadioButtonId != -1) {
                val selectedRadioButton = view?.findViewById<RadioButton>(selectedRadioButtonId)
                val selectedAnswer = selectedRadioButton?.text?.toString()
                if (selectedAnswer != null) {
                    viewModel.submitAnswer(selectedAnswer)
                }
            } else {
                Toast.makeText(context, "Please select an answer", Toast.LENGTH_SHORT).show()
            }
        }

        binding.nextQuestionButton.setOnClickListener {
            viewModel.nextQuestion()
        }

        // Consider adding a restart button if quiz is complete
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: QuizUiState) {
        binding.feedbackTextView.visibility = View.GONE
        binding.nextQuestionButton.visibility = View.GONE
        binding.submitButton.visibility = View.VISIBLE
        binding.optionsRadioGroup.clearCheck()
        enableRadioButtons(true)

        if (state.isQuizComplete) {
            binding.wordToDefineTextView.text = "Quiz Complete!"
            binding.optionsRadioGroup.visibility = View.GONE
            binding.submitButton.text = "Restart Quiz"
            binding.submitButton.setOnClickListener { viewModel.restartQuiz() }
            binding.feedbackTextView.text = state.feedback
            binding.feedbackTextView.visibility = View.VISIBLE
            binding.questionNumberTextView.text = "Finished"
            return
        }

        if (state.currentQuestion == null) {
            // Handle loading or error state
            binding.wordToDefineTextView.text = state.feedback ?: "Loading quiz..."
            binding.optionsRadioGroup.visibility = View.GONE
            binding.submitButton.isEnabled = false
            binding.questionNumberTextView.text = ""
            return
        }

        // Quiz in progress
        binding.optionsRadioGroup.visibility = View.VISIBLE
        binding.submitButton.isEnabled = true
        binding.submitButton.text = "Submit Answer"
        binding.submitButton.setOnClickListener { submitAnswerLogic() } // Re-assign listener

        binding.questionNumberTextView.text = "Question ${state.questionNumber}/${state.totalQuestions}"
        binding.wordToDefineTextView.text = state.currentQuestion.wordToDefine.wordText

        // Set options text
        radioButtons.forEachIndexed { index, radioButton ->
            if (index < state.currentQuestion.options.size) {
                radioButton.text = state.currentQuestion.options[index]
                radioButton.visibility = View.VISIBLE
            } else {
                radioButton.visibility = View.GONE // Hide unused radio buttons
            }
        }

        if (state.isAnswerSubmitted) {
            binding.feedbackTextView.text = state.feedback
            binding.feedbackTextView.visibility = View.VISIBLE
            binding.nextQuestionButton.visibility = View.VISIBLE
            binding.submitButton.visibility = View.GONE
            enableRadioButtons(false)
            highlightAnswers(state)
        }
    }

    private fun submitAnswerLogic() {
         val selectedRadioButtonId = binding.optionsRadioGroup.checkedRadioButtonId
            if (selectedRadioButtonId != -1) {
                val selectedRadioButton = view?.findViewById<RadioButton>(selectedRadioButtonId)
                val selectedAnswer = selectedRadioButton?.text?.toString()
                if (selectedAnswer != null) {
                    viewModel.submitAnswer(selectedAnswer)
                }
            } else {
                Toast.makeText(context, "Please select an answer", Toast.LENGTH_SHORT).show()
            }
    }

    private fun enableRadioButtons(enabled: Boolean) {
        binding.optionsRadioGroup.children.forEach { it.isEnabled = enabled }
    }

    private fun highlightAnswers(state: QuizUiState) {
        val correctAnswer = state.currentQuestion?.correctAnswer
        val selectedAnswer = state.selectedAnswer

        radioButtons.forEach { radioButton ->
            val optionText = radioButton.text.toString()
            when (optionText) {
                correctAnswer -> {
                    // Highlight correct answer (e.g., green background or text color)
                    // Example: radioButton.setBackgroundColor(requireContext().getColor(R.color.correct_green))
                     radioButton.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                }
                selectedAnswer -> {
                    // Highlight incorrect selected answer (e.g., red)
                     radioButton.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                }
                else -> {
                    // Reset others
                     radioButton.setTextColor(requireContext().getColor(android.R.color.tab_indicator_text)) // Default color
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
