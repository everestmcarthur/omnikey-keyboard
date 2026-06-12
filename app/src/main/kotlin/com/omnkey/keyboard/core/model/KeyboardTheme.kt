package com.omnkey.keyboard.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "themes")
data class KeyboardTheme(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean = false,

    // Colors
    val backgroundColor: Int,
    val keyBackgroundColor: Int,
    val keyPressedColor: Int,
    val keyTextColor: Int,
    val keySecondaryTextColor: Int,
    val accentColor: Int,
    val borderColor: Int,

    // Key appearance
    val keyCornerRadius: Float = 8f,
    val keyElevation: Float = 2f,
    val keyBorderWidth: Float = 0f,
    val keySpacing: Float = 4f,

    // Background
    val backgroundImageUri: String? = null,
    val backgroundBlur: Float = 0f,
    val backgroundDim: Float = 0f,

    // Effects
    val rippleEffect: Boolean = true,
    val shadowEffect: Boolean = true,
    val gradientKeys: Boolean = false,
    val gradientStartColor: Int? = null,
    val gradientEndColor: Int? = null,

    // Typography
    val fontFamily: String = "roboto",
    val fontSize: Float = 18f,
    val fontWeight: Int = 400,

    // RGB/Animation
    val rgbEnabled: Boolean = false,
    val rgbSpeed: Float = 1f,
    val rgbBrightness: Float = 1f,

    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class KeyboardLayout(
    val id: String,
    val name: String,
    val layoutType: LayoutType,
    val keys: List<List<Key>>,
    val supportsSwipe: Boolean = true
)

enum class LayoutType {
    QWERTY,
    AZERTY,
    QWERTZ,
    DVORAK,
    COLEMAK,
    NUMERIC,
    SYMBOLS,
    EMOJI,
    CUSTOM
}

data class Key(
    val primary: String,
    val secondary: String? = null,
    val keyType: KeyType = KeyType.CHARACTER,
    val width: Float = 1f,
    val longPressAlternates: List<String> = emptyList(),
    val swipeUp: String? = null,
    val swipeDown: String? = null,
    val swipeLeft: String? = null,
    val swipeRight: String? = null
)

enum class KeyType {
    CHARACTER,
    SPACE,
    BACKSPACE,
    ENTER,
    SHIFT,
    SYMBOL_SWITCH,
    NUMBER_SWITCH,
    EMOJI_SWITCH,
    LANGUAGE_SWITCH,
    SETTINGS,
    VOICE,
    CLIPBOARD,
    CURSOR_LEFT,
    CURSOR_RIGHT
}
