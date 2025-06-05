package com.example.vocabapp.ui

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.vocabapp.databinding.FragmentWordEntryBinding // Use ViewBinding
import com.example.vocabapp.viewmodel.WordEntryViewModel
import com.example.vocabapp.viewmodel.WordEntryViewModelFactory

class WordEntryFragment : Fragment() {

    private var _binding: FragmentWordEntryBinding? = null
    private val binding get() = _binding!!

    // Use the Application context for the ViewModel factory
    private val viewModel: WordEntryViewModel by viewModels {
        WordEntryViewModelFactory(requireActivity().application)
    }

    private val navigationArgs: WordEntryFragmentArgs by navArgs()

    // ActivityResultLauncher for picking image from gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.setPhotoUri(requireContext(), it)
        }
    }

    // ActivityResultLauncher for taking photo with camera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            viewModel.setPhotoBitmap(requireContext(), it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val wordId = navigationArgs.wordId
        if (wordId > 0) {
            // Load existing word for editing
            viewModel.loadWord(wordId)
            // Observe existing word data (if needed for pre-filling, though ViewModel handles photo loading)
            // viewModel.getWordById(wordId).observe(viewLifecycleOwner) { word -> ... }
        }

        setupButtonClickListeners()
        observeViewModel()
    }

    private fun setupButtonClickListeners() {
        binding.attachPhotoButton.setOnClickListener {
            showImageSourceDialog()
        }

        binding.saveButton.setOnClickListener {
            saveWordEntry()
        }

        // Optional: Add a click listener to the image view to allow removal
        binding.photoImageView.setOnClickListener {
             // Maybe show a dialog to confirm removal or change photo
        }
    }

    private fun showImageSourceDialog() {
        // Simple example: Use gallery for now. Could add Camera option later.
        // Consider using MaterialAlertDialogBuilder for a better dialog
        pickImageLauncher.launch("image/*")
        // To add camera:
        // val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        // val builder = AlertDialog.Builder(requireContext())
        // builder.setTitle("Add Photo")
        // builder.setItems(options) { dialog, item -> ... }
        // builder.show()
    }

    private fun observeViewModel() {
        viewModel.currentPhotoBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.photoImageView.setImageBitmap(bitmap)
            } else {
                // Set a placeholder if bitmap is null (e.g., after clearing)
                binding.photoImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // Observe potential errors or navigation events from ViewModel if implemented
        // viewModel.saveResult.observe(viewLifecycleOwner) { success -> ... navigate back ... }
    }

    private fun saveWordEntry() {
        val wordText = binding.wordEditText.text.toString()
        val definition = binding.definitionEditText.text.toString()
        val example = binding.exampleEditText.text.toString()

        if (wordText.isBlank() || definition.isBlank()) {
            Toast.makeText(context, "Word and Definition cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveWord(wordText, definition, example)

        // Navigate back to the list screen after saving
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
