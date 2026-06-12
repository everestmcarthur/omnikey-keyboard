package com.omnkey.keyboard.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omnikey_preferences")

class PreferenceManager(private val context: Context) {

    // Appearance
    val currentThemeId = longPreference("current_theme_id", 1L)
    val keyboardHeight = floatPreference("keyboard_height", 0.3f)
    val keyboardWidth = floatPreference("keyboard_width", 1.0f)
    val showNumberRow = booleanPreference("show_number_row", false)
    val keyPreviewEnabled = booleanPreference("key_preview_enabled", true)

    // Typing
    val autoCapitalization = booleanPreference("auto_capitalization", true)
    val autoPunctuation = booleanPreference("auto_punctuation", true)
    val doubleSpaceForPeriod = booleanPreference("double_space_period", true)
    val autoCorrection = booleanPreference("auto_correction", true)
    val suggestionStripEnabled = booleanPreference("suggestion_strip_enabled", true)
    val aggressiveAutoCorrect = booleanPreference("aggressive_auto_correct", false)

    // Haptics & Sound
    val hapticFeedback = booleanPreference("haptic_feedback", true)
    val hapticStrength = intPreference("haptic_strength", 50)
    val soundEnabled = booleanPreference("sound_enabled", false)
    val soundVolume = intPreference("sound_volume", 50)
    val mechanicalKeyboardSound = booleanPreference("mechanical_sound", false)

    // Swipe/Gesture
    val swipeTypingEnabled = booleanPreference("swipe_typing_enabled", true)
    val swipeSpeed = floatPreference("swipe_speed", 1.0f)
    val swipeSensitivity = floatPreference("swipe_sensitivity", 1.0f)
    val cursorSwipeEnabled = booleanPreference("cursor_swipe_enabled", true)

    // Smart Features
    val nextWordPrediction = booleanPreference("next_word_prediction", true)
    val contextualSuggestions = booleanPreference("contextual_suggestions", true)
    val emojiPrediction = booleanPreference("emoji_prediction", true)
    val textReplacementEnabled = booleanPreference("text_replacement_enabled", true)
    val clipboardSuggestions = booleanPreference("clipboard_suggestions", true)

    // Multi-language
    val activeLanguages = stringSetPreference("active_languages", setOf("en"))
    val autoLanguageDetection = booleanPreference("auto_language_detection", true)
    val bilingualSuggestions = booleanPreference("bilingual_suggestions", false)

    // Privacy
    val incognitoMode = booleanPreference("incognito_mode", false)
    val saveTypingHistory = booleanPreference("save_typing_history", true)
    val cloudSync = booleanPreference("cloud_sync", false)
    val clipboardHistory = booleanPreference("clipboard_history", true)
    val clipboardAutoExpire = booleanPreference("clipboard_auto_expire", true)
    val clipboardExpireDays = intPreference("clipboard_expire_days", 7)

    // Accessibility
    val highContrastMode = booleanPreference("high_contrast_mode", false)
    val largeKeysMode = booleanPreference("large_keys_mode", false)
    val voiceFeedback = booleanPreference("voice_feedback", false)
    val oneHandedMode = booleanPreference("one_handed_mode", false)
    val oneHandedSide = stringPreference("one_handed_side", "right")

    // Advanced Features
    val floatingKeyboard = booleanPreference("floating_keyboard", false)
    val splitKeyboard = booleanPreference("split_keyboard", false)
    val numberPadMode = booleanPreference("number_pad_mode", false)

    // Developer Features
    val showDebugInfo = booleanPreference("show_debug_info", false)
    val verboseLogging = booleanPreference("verbose_logging", false)

    // Coder Features
    val codeAutoComplete = booleanPreference("code_auto_complete", false)
    val syntaxHighlighting = booleanPreference("syntax_highlighting", false)
    val bracketMatching = booleanPreference("bracket_matching", true)
    val autoIndent = booleanPreference("auto_indent", true)
    val tabSize = intPreference("tab_size", 4)
    val insertSpacesForTabs = booleanPreference("insert_spaces_for_tabs", true)
    val showLineNumbers = booleanPreference("show_line_numbers", false)
    val codeSnippets = booleanPreference("code_snippets", true)

    // Writer Features
    val grammarCheck = booleanPreference("grammar_check", false)
    val thesaurusSuggestions = booleanPreference("thesaurus_suggestions", false)
    val wordCount = booleanPreference("word_count", false)
    val readingTime = booleanPreference("reading_time", false)
    val markdownShortcuts = booleanPreference("markdown_shortcuts", false)

    // Terminal Features
    val terminalMode = booleanPreference("terminal_mode", false)
    val quickSymbolAccess = booleanPreference("quick_symbol_access", false)
    val escapeSequences = booleanPreference("escape_sequences", false)

    private fun booleanPreference(key: String, default: Boolean): PreferenceProperty<Boolean> {
        return PreferenceProperty(context.dataStore, booleanPreferencesKey(key), default)
    }

    private fun intPreference(key: String, default: Int): PreferenceProperty<Int> {
        return PreferenceProperty(context.dataStore, intPreferencesKey(key), default)
    }

    private fun longPreference(key: String, default: Long): PreferenceProperty<Long> {
        return PreferenceProperty(context.dataStore, longPreferencesKey(key), default)
    }

    private fun floatPreference(key: String, default: Float): PreferenceProperty<Float> {
        return PreferenceProperty(context.dataStore, floatPreferencesKey(key), default)
    }

    private fun stringPreference(key: String, default: String): PreferenceProperty<String> {
        return PreferenceProperty(context.dataStore, stringPreferencesKey(key), default)
    }

    private fun stringSetPreference(key: String, default: Set<String>): PreferenceProperty<Set<String>> {
        return PreferenceProperty(context.dataStore, stringSetPreferencesKey(key), default)
    }
}

class PreferenceProperty<T>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val defaultValue: T
) {
    val flow: Flow<T> = dataStore.data.map { preferences ->
        preferences[key] ?: defaultValue
    }

    suspend fun get(): T {
        return dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.first()
    }

    suspend fun set(value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun first(): T {
        var result: T = defaultValue
        flow.collect {
            result = it
            return@collect
        }
        return result
    }
}
