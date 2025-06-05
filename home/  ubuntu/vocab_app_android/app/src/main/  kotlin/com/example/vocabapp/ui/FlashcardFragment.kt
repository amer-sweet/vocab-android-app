package com.example.vocabapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.vocabapp.databinding.FragmentFlashcardBinding // Use ViewBinding
import com.example.vocabapp.viewmodel.FlashcardUiState
import com.example.vocabapp.viewmodel.FlashcardViewModel
import com.example.vocabapp.viewmodel.FlashcardViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FlashcardFragment : Fragment() {

    private var _binding: FragmentFlashcardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FlashcardViewModel by viewModels {
        FlashcardViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlashcardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtonClickListeners()
        observeUiState()
    }

    private fun setupButtonClickListeners() {
        binding.flipButton.setOnClickListener {
            viewModel.flipCard()
        }
        binding.nextButton.setOnClickListener {
            viewModel.nextCard()
        }
        binding.previousButton.setOnClickListener {
            viewModel.previousCard()
        }
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

    private fun updateUi(state: FlashcardUiState) {
        binding.previousButton.isEnabled = state.canGoPrevious
        binding.nextButton.isEnabled = state.canGoNext
        binding.flipButton.isEnabled = state.currentWord != null // Enable flip only if there's a word

        if (state.currentWord == null) {
            // Handle empty state - maybe show a message
            binding.wordTextViewFront.text = "No words saved yet."
            binding.cardBackLayout.visibility = View.GONE
            binding.wordTextViewFront.visibility = View.VISIBLE
            return
        }

        // Update card content based on visibility state
        if (state.isFrontVisible) {
            binding.wordTextViewFront.visibility = View.VISIBLE
            binding.cardBackLayout.visibility = View.GONE
            binding.wordTextViewFront.text = state.currentWord.wordText
        } else {
            binding.wordTextViewFront.visibility = View.GONE
            binding.cardBackLayout.visibility = View.VISIBLE

            binding.definitionTextViewBack.text = state.currentWord.definition
            binding.exampleTextViewBack.text = state.currentWord.exampleSentence ?: ""
            binding.exampleTextViewBack.visibility = if (state.currentWord.exampleSentence.isNullOrBlank()) View.GONE else View.VISIBLE

            if (state.currentBitmap != null) {
                binding.photoImageViewBack.setImageBitmap(state.currentBitmap)
                binding.photoImageViewBack.visibility = View.VISIBLE
            } else {
                binding.photoImageViewBack.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
