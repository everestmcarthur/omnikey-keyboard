package com.omnkey.keyboard.core.engine

import com.omnkey.keyboard.core.database.OmniKeyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TextReplacementEngine(private val database: OmniKeyDatabase) {

    private val textReplacementDao = database.textReplacementDao()
    private val replacementCache = mutableMapOf<String, String>()

    suspend fun process(text: String, contextBefore: String): String = withContext(Dispatchers.IO) {
        // Load cache if empty
        if (replacementCache.isEmpty()) {
            loadReplacements()
        }

        // Get the last word (including current text)
        val fullText = contextBefore + text
        val lastWord = fullText.trim().split(Regex("\\s+")).lastOrNull() ?: return@withContext text

        // Check for replacement
        val replacement = replacementCache[lastWord.lowercase()]
        if (replacement != null) {
            // Track usage
            val replacementEntry = textReplacementDao.getAllEnabled().first()
                .find { it.shortcut.equals(lastWord, ignoreCase = !it.caseSensitive) }

            replacementEntry?.let {
                textReplacementDao.incrementUseCount(it.id)
            }

            // Process variables in replacement
            return@withContext processVariables(replacement)
        }

        text
    }

    private suspend fun loadReplacements() {
        val replacements = textReplacementDao.getAllEnabled().first()
        replacementCache.clear()
        replacements.forEach { replacement ->
            replacementCache[replacement.shortcut.lowercase()] = replacement.replacement
        }
    }

    private fun processVariables(text: String): String {
        var result = text

        // Date/time variables
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val datetimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = Date()

        result = result.replace("{{date}}", dateFormat.format(now))
        result = result.replace("{{time}}", timeFormat.format(now))
        result = result.replace("{{datetime}}", datetimeFormat.format(now))
        result = result.replace("{{year}}", now.year.toString())

        // Clipboard (would need clipboard manager reference)
        // result = result.replace("{{clipboard}}", getClipboardContent())

        return result
    }

    suspend fun addReplacement(shortcut: String, replacement: String) {
        val entry = com.omnkey.keyboard.core.model.TextReplacement(
            shortcut = shortcut,
            replacement = replacement
        )
        textReplacementDao.insert(entry)
        loadReplacements()
    }

    suspend fun removeReplacement(shortcut: String) {
        val entry = textReplacementDao.getAllEnabled().first()
            .find { it.shortcut.equals(shortcut, ignoreCase = true) }
        entry?.let {
            textReplacementDao.delete(it)
            loadReplacements()
        }
    }
}
