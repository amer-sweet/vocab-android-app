package com.example.vocabapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vocabapp.R // Assuming R is generated correctly
import com.example.vocabapp.data.Word

class WordListAdapter(private val onItemClicked: (Word) -> Unit) :
    ListAdapter<Word, WordListAdapter.WordViewHolder>(WordsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        return WordViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
    }

    class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val wordItemView: TextView = itemView.findViewById(R.id.word_text_view)
        private val photoIndicator: ImageView = itemView.findViewById(R.id.photo_indicator_image_view)

        fun bind(word: Word) {
            wordItemView.text = word.wordText
            // Show indicator if a photo filename exists
            photoIndicator.visibility = if (word.photoFilename != null) View.VISIBLE else View.GONE
        }

        companion object {
            fun create(parent: ViewGroup): WordViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_word, parent, false)
                return WordViewHolder(view)
            }
        }
    }

    class WordsComparator : DiffUtil.ItemCallback<Word>() {
        override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean {
            // Compare relevant fields, including photo indicator status
            return oldItem.wordText == newItem.wordText &&
                   oldItem.definition == newItem.definition &&
                   oldItem.exampleSentence == newItem.exampleSentence &&
                   oldItem.photoFilename == newItem.photoFilename
                   // Note: Don't compare sync status or lastModified for visual diffing
        }
    }
    }
    
