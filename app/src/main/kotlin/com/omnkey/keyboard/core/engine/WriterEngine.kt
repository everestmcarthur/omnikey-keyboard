package com.omnkey.keyboard.core.engine

import android.content.Context
import com.omnkey.keyboard.core.database.OmniKeyDatabase

class WriterEngine(
    private val context: Context,
    private val database: OmniKeyDatabase
) {
    var isActive = false
        private set

    private var isFormalMode = false

    // Smart punctuation
    private val smartQuoteMap = mapOf(
        "\"" to Pair(""", """),
        "'" to Pair("'", "'")
    )

    // Common writing shortcuts
    private val writingShortcuts = mapOf(
        "--" to "—",  // Em dash
        "..." to "…", // Ellipsis
        "(c)" to "©",
        "(r)" to "®",
        "(tm)" to "™",
        "1/2" to "½",
        "1/4" to "¼",
        "3/4" to "¾",
        "<-" to "←",
        "->" to "→",
        "<->" to "↔",
        "deg" to "°"
    )

    // Markdown shortcuts
    private val markdownPairs = mapOf(
        "**" to "**",  // Bold
        "*" to "*",    // Italic
        "__" to "__",  // Bold alt
        "_" to "_",    // Italic alt
        "~~" to "~~",  // Strikethrough
        "`" to "`",    // Inline code
        "```" to "```" // Code block
    )

    // Grammar rules (simplified)
    private val grammarRules = listOf(
        GrammarRule("\\bi\\b", "I", "Capitalize pronoun 'I'"),
        GrammarRule("\\s{2,}", " ", "Multiple spaces"),
        GrammarRule("\\s+([.,!?;:])", "$1", "Space before punctuation"),
        GrammarRule("([.!?])([A-Z])", "$1 $2", "Missing space after sentence")
    )

    // Common homophones and confusables
    private val confusables = mapOf(
        "there" to listOf("their", "they're"),
        "your" to listOf("you're"),
        "its" to listOf("it's"),
        "then" to listOf("than"),
        "affect" to listOf("effect"),
        "accept" to listOf("except")
    )

    // Thesaurus (simplified - would use external API in production)
    private val synonyms = mapOf(
        "good" to listOf("great", "excellent", "fine", "nice", "pleasant"),
        "bad" to listOf("poor", "terrible", "awful", "inferior"),
        "big" to listOf("large", "huge", "massive", "enormous", "gigantic"),
        "small" to listOf("tiny", "little", "mini", "compact", "petite"),
        "said" to listOf("stated", "mentioned", "expressed", "declared", "remarked")
    )

    fun enableWritingMode() {
        isActive = true
        isFormalMode = false
    }

    fun enableFormalMode() {
        isActive = true
        isFormalMode = true
    }

    fun disableWritingMode() {
        isActive = false
        isFormalMode = false
    }

    fun processSmartQuotes(char: String, contextBefore: String): String {
        val pair = smartQuoteMap[char] ?: return char

        // Determine if opening or closing
        val isOpening = contextBefore.isEmpty() ||
                       contextBefore.last().isWhitespace() ||
                       contextBefore.last() in listOf('(', '[', '{')

        return if (isOpening) pair.first else pair.second
    }

    fun processWritingShortcut(text: String, contextBefore: String): String {
        val combined = contextBefore + text
        for ((shortcut, replacement) in writingShortcuts) {
            if (combined.endsWith(shortcut)) {
                return replacement
            }
        }
        return text
    }

    fun processMarkdown(char: String, contextBefore: String): String? {
        for ((opening, closing) in markdownPairs) {
            if (char == opening.first().toString() && contextBefore.endsWith(opening.dropLast(1))) {
                // Complete the markdown pair
                return "$closing"
            }
        }
        return null
    }

    fun checkGrammar(text: String): List<GrammarSuggestion> {
        val suggestions = mutableListOf<GrammarSuggestion>()

        for (rule in grammarRules) {
            val regex = Regex(rule.pattern)
            regex.findAll(text).forEach { match ->
                suggestions.add(
                    GrammarSuggestion(
                        position = match.range.first,
                        length = match.value.length,
                        message = rule.message,
                        suggestion = match.value.replace(regex, rule.replacement)
                    )
                )
            }
        }

        // Check for confusables
        val words = text.split(Regex("\\b"))
        words.forEachIndexed { index, word ->
            confusables[word.lowercase()]?.let { alternatives ->
                suggestions.add(
                    GrammarSuggestion(
                        position = index,
                        length = word.length,
                        message = "Commonly confused with: ${alternatives.joinToString(", ")}",
                        suggestion = word
                    )
                )
            }
        }

        return suggestions
    }

    fun getSynonyms(word: String): List<String> {
        return synonyms[word.lowercase()] ?: emptyList()
    }

    fun getReadabilityScore(text: String): ReadabilityMetrics {
        val sentences = text.split(Regex("[.!?]+")).filter { it.isNotBlank() }
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val syllables = words.sumOf { countSyllables(it) }

        val avgWordsPerSentence = if (sentences.isNotEmpty()) {
            words.size.toFloat() / sentences.size
        } else 0f

        val avgSyllablesPerWord = if (words.isNotEmpty()) {
            syllables.toFloat() / words.size
        } else 0f

        // Flesch Reading Ease
        val fleschScore = 206.835 - 1.015 * avgWordsPerSentence - 84.6 * avgSyllablesPerWord

        // Flesch-Kincaid Grade Level
        val gradeLevel = 0.39 * avgWordsPerSentence + 11.8 * avgSyllablesPerWord - 15.59

        return ReadabilityMetrics(
            wordCount = words.size,
            sentenceCount = sentences.size,
            characterCount = text.length,
            avgWordsPerSentence = avgWordsPerSentence,
            avgSyllablesPerWord = avgSyllablesPerWord,
            fleschReadingEase = fleschScore,
            fleschKincaidGrade = gradeLevel,
            estimatedReadingTime = (words.size / 200.0) // Avg 200 words per minute
        )
    }

    private fun countSyllables(word: String): Int {
        // Simplified syllable counting
        var count = 0
        var previousWasVowel = false

        for (char in word.lowercase()) {
            val isVowel = char in "aeiouy"
            if (isVowel && !previousWasVowel) {
                count++
            }
            previousWasVowel = isVowel
        }

        // Adjust for silent 'e'
        if (word.endsWith("e", ignoreCase = true) && count > 1) {
            count--
        }

        return maxOf(1, count)
    }

    fun formatTitle(text: String): String {
        val lowercaseWords = setOf("a", "an", "the", "and", "but", "or", "for", "nor", "on", "at", "to", "by", "in", "of")

        return text.split(" ").mapIndexed { index, word ->
            if (index == 0 || word.lowercase() !in lowercaseWords) {
                word.replaceFirstChar { it.uppercase() }
            } else {
                word.lowercase()
            }
        }.joinToString(" ")
    }

    fun insertTemplate(type: TemplateType): String {
        return when (type) {
            TemplateType.EMAIL_FORMAL -> "Dear [Name],\n\n[Body]\n\nBest regards,\n[Your Name]"
            TemplateType.EMAIL_CASUAL -> "Hi [Name],\n\n[Body]\n\nThanks,\n[Your Name]"
            TemplateType.MEETING_NOTES -> "# Meeting Notes\n\n**Date:** [Date]\n**Attendees:** [Names]\n\n## Agenda\n- \n\n## Discussion\n\n\n## Action Items\n- [ ] \n"
            TemplateType.BLOG_POST -> "# [Title]\n\n## Introduction\n\n\n## Main Content\n\n\n## Conclusion\n\n"
            TemplateType.STORY_SCENE -> "### Chapter [Number]\n\n[Scene description]\n\n\"Dialogue,\" [Character] said.\n\n"
        }
    }
}

data class GrammarRule(
    val pattern: String,
    val replacement: String,
    val message: String
)

data class GrammarSuggestion(
    val position: Int,
    val length: Int,
    val message: String,
    val suggestion: String
)

data class ReadabilityMetrics(
    val wordCount: Int,
    val sentenceCount: Int,
    val characterCount: Int,
    val avgWordsPerSentence: Float,
    val avgSyllablesPerWord: Float,
    val fleschReadingEase: Float,
    val fleschKincaidGrade: Float,
    val estimatedReadingTime: Double
)

enum class TemplateType {
    EMAIL_FORMAL,
    EMAIL_CASUAL,
    MEETING_NOTES,
    BLOG_POST,
    STORY_SCENE
}
