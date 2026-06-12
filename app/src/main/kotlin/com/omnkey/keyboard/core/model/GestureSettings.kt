package com.omnkey.keyboard.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_actions")
data class GestureAction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gestureType: GestureType,
    val actionType: ActionType,
    val parameter: String? = null,
    val enabled: Boolean = true
)

enum class GestureType {
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_SPACEBAR_LEFT,
    SWIPE_SPACEBAR_RIGHT,
    SWIPE_BACKSPACE_LEFT,
    TWO_FINGER_SWIPE_LEFT,
    TWO_FINGER_SWIPE_RIGHT,
    LONG_PRESS_SPACE,
    DOUBLE_TAP_SPACE,
    TRIPLE_TAP_SHIFT,
    PINCH_IN,
    PINCH_OUT
}

enum class ActionType {
    DELETE_WORD,
    DELETE_LINE,
    MOVE_CURSOR_LEFT,
    MOVE_CURSOR_RIGHT,
    MOVE_CURSOR_WORD_LEFT,
    MOVE_CURSOR_WORD_RIGHT,
    MOVE_CURSOR_START,
    MOVE_CURSOR_END,
    SELECT_WORD,
    SELECT_ALL,
    COPY,
    CUT,
    PASTE,
    UNDO,
    REDO,
    SWITCH_LANGUAGE,
    SWITCH_LAYOUT,
    OPEN_EMOJI,
    OPEN_CLIPBOARD,
    OPEN_SETTINGS,
    INSERT_PERIOD,
    INSERT_SPACE,
    TOGGLE_VOICE_INPUT,
    TOGGLE_ONE_HANDED_MODE,
    RESIZE_KEYBOARD,
    CUSTOM_TEXT
}

@Entity(tableName = "macro_definitions")
data class MacroDefinition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val shortcut: String,
    val actions: List<MacroAction>,
    val category: String = "general",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class MacroAction(
    val type: MacroActionType,
    val parameter: String? = null,
    val delayMs: Long = 0
)

enum class MacroActionType {
    TYPE_TEXT,
    INSERT_TIMESTAMP,
    INSERT_DATE,
    INSERT_TIME,
    INSERT_CLIPBOARD,
    PRESS_KEY,
    WAIT,
    SELECT_TEXT,
    MOVE_CURSOR
}
