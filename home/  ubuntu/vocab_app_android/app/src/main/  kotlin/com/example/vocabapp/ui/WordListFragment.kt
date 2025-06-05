package com.example.vocabapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vocabapp.adapter.WordListAdapter
import com.example.vocabapp.databinding.FragmentWordListBinding // Use ViewBinding
import com.example.vocabapp.viewmodel.WordListViewModel
import com.example.vocabapp.viewmodel.WordListViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WordListFragment : Fragment() {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordListViewModel by viewModels {
        WordListViewModelFactory(requireActivity().application)
    }

    private lateinit var adapter: WordListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        setupFab()
        observeWordList()
    }

    private fun setupRecyclerView() {
        adapter = WordListAdapter { word ->
            // Navigate to WordEntryFragment for editing, passing the word ID
            val action = WordListFragmentDirections.actionWordListFragmentToWordEntryFragment(word.id)
            findNavController().navigate(action)
        }
        binding.wordRecyclerView.adapter = adapter
        binding.wordRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Optional: Add ItemDecoration for dividers
        // binding.wordRecyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))

        // Optional: Add swipe-to-delete functionality here if desired
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Can perform search on submit if needed, but typically handled by onQueryTextChange
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
    }

    private fun setupFab() {
        binding.fabAddWord.setOnClickListener {
            // Navigate to WordEntryFragment for adding a new word (pass ID 0 or -1)
             val action = WordListFragmentDirections.actionWordListFragmentToWordEntryFragment(0)
            findNavController().navigate(action)
        }
    }

    private fun observeWordList() {
        // Use repeatOnLifecycle to collect the flow safely
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.words.collectLatest { words ->
                    adapter.submitList(words)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
