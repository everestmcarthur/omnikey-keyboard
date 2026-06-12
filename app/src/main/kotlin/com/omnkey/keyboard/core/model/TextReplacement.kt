package com.omnkey.keyboard.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "text_replacements")
data class TextReplacement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shortcut: String,
    val replacement: String,
    val caseSensitive: Boolean = false,
    val enabled: Boolean = true,
    val category: String = "general",
    val useCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learned_words")
data class LearnedWord(
    @PrimaryKey
    val word: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val language: String = "en"
)

@Entity(tableName = "word_predictions")
data class WordPrediction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val context: String,
    val word: String,
    val nextWord: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val contentType: ClipboardType = ClipboardType.TEXT,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val category: String? = null,
    val copiedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 0,
    val expiresAt: Long? = null
)

enum class ClipboardType {
    TEXT,
    URL,
    EMAIL,
    PHONE,
    IMAGE,
    RICH_TEXT
}

@Entity(tableName = "emoji_frequency")
data class EmojiFrequency(
    @PrimaryKey
    val emoji: String,
    val useCount: Int = 1,
    val lastUsed: Long = System.currentTimeMillis(),
    val category: String? = null
)

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val category: String = "general",
    val variables: List<String> = emptyList(),
    val useCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
