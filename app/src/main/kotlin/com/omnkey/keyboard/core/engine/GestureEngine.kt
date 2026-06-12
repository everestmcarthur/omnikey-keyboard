package com.omnkey.keyboard.core.engine

import android.content.Context
import com.omnkey.keyboard.core.database.OmniKeyDatabase
import com.omnkey.keyboard.core.model.ActionType
import com.omnkey.keyboard.core.model.GestureAction
import com.omnkey.keyboard.core.model.GestureType
import kotlinx.coroutines.flow.first

class GestureEngine(
    private val context: Context,
    private val database: OmniKeyDatabase
) {
    private val gestureDao = database.gestureDao()
    private val gestureMap = mutableMapOf<GestureType, ActionType>()

    suspend fun loadGestures() {
        val gestures = gestureDao.getAllEnabled().first()
        gestureMap.clear()
        gestures.forEach { gesture ->
            gestureMap[gesture.gestureType] = gesture.actionType
        }

        // Set defaults if empty
        if (gestureMap.isEmpty()) {
            setDefaultGestures()
        }
    }

    private suspend fun setDefaultGestures() {
        val defaults = listOf(
            GestureAction(gestureType = GestureType.SWIPE_LEFT, actionType = ActionType.DELETE_WORD),
            GestureAction(gestureType = GestureType.SWIPE_RIGHT, actionType = ActionType.REDO),
            GestureAction(gestureType = GestureType.SWIPE_UP, actionType = ActionType.OPEN_EMOJI),
            GestureAction(gestureType = GestureType.SWIPE_SPACEBAR_LEFT, actionType = ActionType.MOVE_CURSOR_LEFT),
            GestureAction(gestureType = GestureType.SWIPE_SPACEBAR_RIGHT, actionType = ActionType.MOVE_CURSOR_RIGHT),
            GestureAction(gestureType = GestureType.SWIPE_BACKSPACE_LEFT, actionType = ActionType.DELETE_WORD),
            GestureAction(gestureType = GestureType.LONG_PRESS_SPACE, actionType = ActionType.SWITCH_LANGUAGE),
            GestureAction(gestureType = GestureType.DOUBLE_TAP_SPACE, actionType = ActionType.INSERT_PERIOD),
            GestureAction(gestureType = GestureType.TRIPLE_TAP_SHIFT, actionType = ActionType.SELECT_ALL),
            GestureAction(gestureType = GestureType.TWO_FINGER_SWIPE_LEFT, actionType = ActionType.UNDO),
            GestureAction(gestureType = GestureType.TWO_FINGER_SWIPE_RIGHT, actionType = ActionType.REDO),
            GestureAction(gestureType = GestureType.PINCH_IN, actionType = ActionType.RESIZE_KEYBOARD),
            GestureAction(gestureType = GestureType.PINCH_OUT, actionType = ActionType.RESIZE_KEYBOARD)
        )

        defaults.forEach { gestureDao.insert(it) }
        loadGestures()
    }

    fun getAction(gestureType: GestureType): ActionType? {
        return gestureMap[gestureType]
    }

    suspend fun setGesture(gestureType: GestureType, actionType: ActionType) {
        val gesture = GestureAction(
            gestureType = gestureType,
            actionType = actionType,
            enabled = true
        )
        gestureDao.insert(gesture)
        gestureMap[gestureType] = actionType
    }

    suspend fun removeGesture(gestureType: GestureType) {
        val gestures = gestureDao.getAllEnabled().first()
        val gesture = gestures.find { it.gestureType == gestureType }
        gesture?.let {
            gestureDao.delete(it)
            gestureMap.remove(gestureType)
        }
    }
}
