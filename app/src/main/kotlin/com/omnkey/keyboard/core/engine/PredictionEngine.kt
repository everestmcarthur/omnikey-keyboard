package com.omnkey.keyboard.core.engine

import android.content.Context
import com.omnkey.keyboard.core.database.OmniKeyDatabase
import com.omnkey.keyboard.core.model.LearnedWord
import com.omnkey.keyboard.core.model.WordPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PredictionEngine(
    private val context: Context,
    private val database: OmniKeyDatabase
) {
    private val learnedWordDao = database.learnedWordDao()
    private val wordPredictionDao = database.wordPredictionDao()

    // N-gram context for predictions
    private val contextCache = mutableMapOf<String, List<String>>()

    suspend fun getPredictions(contextBefore: String, limit: Int = 3): List<String> = withContext(Dispatchers.IO) {
        val predictions = mutableListOf<String>()

        // Get last few words for context
        val words = contextBefore.trim().split(Regex("\\s+"))
        val lastWord = words.lastOrNull()?.lowercase() ?: ""
        val prevWord = if (words.size >= 2) words[words.size - 2].lowercase() else ""

        // 1. Check if we're in the middle of typing a word
        if (lastWord.isNotEmpty() && !contextBefore.endsWith(" ")) {
            val completions = learnedWordDao.getWordsSuggestionsStartingWith(lastWord, "en")
            predictions.addAll(completions.map { it.word }.take(limit))
        }

        // 2. Next word prediction based on previous word
        if (predictions.size < limit && prevWord.isNotEmpty()) {
            val nextWords = wordPredictionDao.getPredictions(prevWord)
            predictions.addAll(nextWords.map { it.nextWord }.take(limit - predictions.size))
        }

        // 3. Most frequent words as fallback
        if (predictions.isEmpty()) {
            predictions.addAll(getCommonWords(limit))
        }

        predictions.distinct().take(limit)
    }

    suspend fun learn(text: String, contextBefore: String) = withContext(Dispatchers.IO) {
        val words = contextBefore.trim().split(Regex("\\s+"))
        val prevWord = words.lastOrNull()?.lowercase()

        // Learn the word itself
        val word = text.trim().lowercase()
        if (word.isNotEmpty() && word.length > 1) {
            val existingWord = learnedWordDao.getWordsSuggestionsStartingWith(word, "en")
                .firstOrNull { it.word == word }

            if (existingWord != null) {
                learnedWordDao.incrementWordFrequency(word)
            } else {
                learnedWordDao.insertWord(LearnedWord(word = word, frequency = 1))
            }
        }

        // Learn word pair for next-word prediction
        if (prevWord != null && word.isNotEmpty()) {
            wordPredictionDao.incrementPredictionFrequency(prevWord, word)
        }
    }

    private fun getCommonWords(limit: Int): List<String> {
        return listOf("the", "be", "to", "of", "and", "a", "in", "that", "have", "I")
            .take(limit)
    }

    suspend fun clearOldData(daysOld: Int = 90) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        learnedWordDao.deleteOldWords(cutoffTime)
        wordPredictionDao.deleteOldPredictions(cutoffTime)
    }
}
