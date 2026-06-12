package com.omnkey.keyboard.core.database

import androidx.room.*
import com.omnkey.keyboard.core.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes")
    fun getAllThemes(): Flow<List<KeyboardTheme>>

    @Query("SELECT * FROM themes WHERE id = :id")
    suspend fun getThemeById(id: Long): KeyboardTheme?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: KeyboardTheme): Long

    @Update
    suspend fun updateTheme(theme: KeyboardTheme)

    @Delete
    suspend fun deleteTheme(theme: KeyboardTheme)
}

@Dao
interface TextReplacementDao {
    @Query("SELECT * FROM text_replacements WHERE enabled = 1 ORDER BY shortcut")
    fun getAllEnabled(): Flow<List<TextReplacement>>

    @Query("SELECT * FROM text_replacements ORDER BY useCount DESC")
    fun getAllByUsage(): Flow<List<TextReplacement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(replacement: TextReplacement): Long

    @Update
    suspend fun update(replacement: TextReplacement)

    @Delete
    suspend fun delete(replacement: TextReplacement)

    @Query("UPDATE text_replacements SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: Long)
}

@Dao
interface LearnedWordDao {
    @Query("SELECT * FROM learned_words WHERE language = :language ORDER BY frequency DESC LIMIT 1000")
    fun getTopWords(language: String): Flow<List<LearnedWord>>

    @Query("SELECT * FROM learned_words WHERE word LIKE :prefix || '%' AND language = :language ORDER BY frequency DESC LIMIT 10")
    suspend fun getWordsSuggestionsStartingWith(prefix: String, language: String): List<LearnedWord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: LearnedWord)

    @Query("UPDATE learned_words SET frequency = frequency + 1, lastUsed = :timestamp WHERE word = :word")
    suspend fun incrementWordFrequency(word: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM learned_words WHERE lastUsed < :timestamp")
    suspend fun deleteOldWords(timestamp: Long)
}

@Dao
interface WordPredictionDao {
    @Query("SELECT * FROM word_predictions WHERE context = :context ORDER BY frequency DESC LIMIT 5")
    suspend fun getPredictions(context: String): List<WordPrediction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: WordPrediction)

    @Query("UPDATE word_predictions SET frequency = frequency + 1, lastUsed = :timestamp WHERE context = :context AND nextWord = :nextWord")
    suspend fun incrementPredictionFrequency(context: String, nextWord: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM word_predictions WHERE lastUsed < :timestamp")
    suspend fun deleteOldPredictions(timestamp: Long)
}

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, copiedAt DESC LIMIT 100")
    fun getAllClipboardItems(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE isPinned = 1 ORDER BY copiedAt DESC")
    fun getPinnedItems(): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipboardItem(item: ClipboardItem): Long

    @Update
    suspend fun updateClipboardItem(item: ClipboardItem)

    @Delete
    suspend fun deleteClipboardItem(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0 AND expiresAt < :timestamp")
    suspend fun deleteExpiredItems(timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0 AND id NOT IN (SELECT id FROM clipboard_items ORDER BY copiedAt DESC LIMIT 100)")
    suspend fun deleteOldItems()

    @Query("UPDATE clipboard_items SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: Long)
}

@Dao
interface EmojiDao {
    @Query("SELECT * FROM emoji_frequency ORDER BY useCount DESC LIMIT 50")
    fun getFrequentEmojis(): Flow<List<EmojiFrequency>>

    @Query("SELECT * FROM emoji_frequency WHERE lastUsed > :timestamp ORDER BY useCount DESC")
    fun getRecentEmojis(timestamp: Long): Flow<List<EmojiFrequency>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmoji(emoji: EmojiFrequency)

    @Query("UPDATE emoji_frequency SET useCount = useCount + 1, lastUsed = :timestamp WHERE emoji = :emoji")
    suspend fun incrementEmojiCount(emoji: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY useCount DESC")
    fun getAllTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE category = :category ORDER BY useCount DESC")
    fun getTemplatesByCategory(category: String): Flow<List<Template>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("UPDATE templates SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: Long)
}

@Dao
interface GestureDao {
    @Query("SELECT * FROM gesture_actions WHERE enabled = 1")
    fun getAllEnabled(): Flow<List<GestureAction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gesture: GestureAction): Long

    @Update
    suspend fun update(gesture: GestureAction)

    @Delete
    suspend fun delete(gesture: GestureAction)
}

@Dao
interface MacroDao {
    @Query("SELECT * FROM macro_definitions WHERE enabled = 1")
    fun getAllEnabled(): Flow<List<MacroDefinition>>

    @Query("SELECT * FROM macro_definitions WHERE category = :category AND enabled = 1")
    fun getByCategory(category: String): Flow<List<MacroDefinition>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(macro: MacroDefinition): Long

    @Update
    suspend fun update(macro: MacroDefinition)

    @Delete
    suspend fun delete(macro: MacroDefinition)
}
