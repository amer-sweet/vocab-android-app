package com.example.vocabapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word): Long

    @Update
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)

    @Query("SELECT * FROM words ORDER BY wordText ASC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE id = :id")
    fun getWordById(id: Int): Flow<Word?>

    @Query("SELECT * FROM words WHERE wordText LIKE :query || '%' ORDER BY wordText ASC")
    fun searchWords(query: String): Flow<List<Word>>

    // Add queries for sync logic if needed, e.g., finding words needing sync
    @Query("SELECT * FROM words WHERE syncStatus != 'synced'")
    suspend fun getWordsNeedingSync(): List<Word>
}
