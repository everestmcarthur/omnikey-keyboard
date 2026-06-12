package com.omnkey.keyboard.core.engine

import android.content.Context
import com.omnkey.keyboard.core.database.OmniKeyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutocorrectEngine(
    private val context: Context,
    private val database: OmniKeyDatabase
) {
    private val learnedWordDao = database.learnedWordDao()

    // Common typo patterns
    private val commonTypos = mapOf(
        "teh" to "the",
        "taht" to "that",
        "waht" to "what",
        "thier" to "their",
        "recieve" to "receive",
        "occured" to "occurred",
        "seperate" to "separate",
        "definately" to "definitely",
        "untill" to "until",
        "basicly" to "basically"
    )

    suspend fun getCorrection(word: String): String? = withContext(Dispatchers.IO) {
        val lowercase = word.lowercase()

        // 1. Check common typos
        commonTypos[lowercase]?.let { return@withContext it }

        // 2. Check if word exists in learned dictionary
        val learned = learnedWordDao.getWordsSuggestionsStartingWith(lowercase, "en")
        if (learned.any { it.word == lowercase }) {
            return@withContext null // Word is correct
        }

        // 3. Find closest match using edit distance
        val suggestions = learnedWordDao.getWordsSuggestionsStartingWith(
            lowercase.take(2), "en"
        )

        val bestMatch = suggestions
            .filter { editDistance(lowercase, it.word) <= 2 }
            .maxByOrNull { it.frequency }

        bestMatch?.word
    }

    suspend fun learn(word: String) = withContext(Dispatchers.IO) {
        val lowercase = word.lowercase()
        if (lowercase.length > 1 && !commonTypos.containsKey(lowercase)) {
            learnedWordDao.incrementWordFrequency(lowercase)
        }
    }

    fun capitalizeIfNeeded(text: String, contextBefore: String): String {
        if (text.isEmpty()) return text

        // Capitalize after sentence end
        val trimmed = contextBefore.trimEnd()
        if (trimmed.isEmpty() || trimmed.matches(Regex(".*[.!?]\\s*$"))) {
            return text.replaceFirstChar { it.uppercase() }
        }

        // Capitalize "I"
        if (text.lowercase() == "i") {
            return "I"
        }

        return text
    }

    private fun editDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }
}
