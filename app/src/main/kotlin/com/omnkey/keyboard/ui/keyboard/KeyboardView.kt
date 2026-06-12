package com.omnkey.keyboard.ui.keyboard

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.omnkey.keyboard.core.model.*
import com.omnkey.keyboard.service.SwipePath
import kotlin.math.abs

class KeyboardView(context: Context) : View(context) {

    private var keyListener: ((String, KeyType) -> Unit)? = null
    private var swipeListener: ((SwipePath) -> Unit)? = null
    private var gestureListener: ((GestureType) -> Unit)? = null

    private var currentTheme: KeyboardTheme? = null
    private var currentLayout: KeyboardLayout = createQwertyLayout()
    private var isShifted = false
    private var isCapsLock = false
    private var suggestions = listOf<String>()

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val suggestionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint()

    private val keyRects = mutableListOf<KeyRect>()
    private var pressedKey: KeyRect? = null
    private var swipePoints = mutableListOf<PointF>()
    private var swipeStartTime = 0L

    init {
        textPaint.textAlign = Paint.Align.CENTER
        suggestionPaint.textAlign = Paint.Align.CENTER
        setWillNotDraw(false)
    }

    fun setOnKeyListener(listener: (String, KeyType) -> Unit) {
        keyListener = listener
    }

    fun setOnSwipeListener(listener: (SwipePath) -> Unit) {
        swipeListener = listener
    }

    fun setOnGestureListener(listener: (GestureType) -> Unit) {
        gestureListener = listener
    }

    fun applyTheme(theme: KeyboardTheme) {
        currentTheme = theme
        backgroundPaint.color = theme.backgroundColor
        keyPaint.color = theme.keyBackgroundColor
        textPaint.color = theme.keyTextColor
        textPaint.textSize = theme.fontSize * resources.displayMetrics.density
        invalidate()
    }

    fun updateSuggestions(newSuggestions: List<String>) {
        suggestions = newSuggestions
        invalidate()
    }

    fun toggleShift() {
        if (isShifted) {
            isCapsLock = !isCapsLock
            isShifted = isCapsLock
        } else {
            isShifted = true
        }
        invalidate()
    }

    fun showEmojiPanel() {
        currentLayout = createEmojiLayout()
        invalidate()
    }

    fun showSymbolLayout() {
        currentLayout = createSymbolLayout()
        invalidate()
    }

    fun showNumberLayout() {
        currentLayout = createNumberLayout()
        invalidate()
    }

    fun showQwertyLayout() {
        currentLayout = createQwertyLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val theme = currentTheme ?: return

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw suggestion strip
        drawSuggestionStrip(canvas)

        // Calculate key positions
        val suggestionHeight = height * 0.15f
        val keyboardHeight = height - suggestionHeight
        val rowHeight = keyboardHeight / currentLayout.keys.size

        keyRects.clear()

        currentLayout.keys.forEachIndexed { rowIndex, row ->
            val y = suggestionHeight + rowIndex * rowHeight
            val totalWidth = row.sumOf { it.width.toDouble() }.toFloat()
            var x = (width - totalWidth * (width / (totalWidth + row.size - 1))) / 2

            row.forEach { key ->
                val keyWidth = (width / totalWidth) * key.width
                val rect = RectF(x, y + 4, x + keyWidth - 8, y + rowHeight - 4)

                // Draw key background
                keyPaint.color = if (pressedKey?.key == key) {
                    theme.keyPressedColor
                } else {
                    theme.keyBackgroundColor
                }

                val cornerRadius = theme.keyCornerRadius * resources.displayMetrics.density
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyPaint)

                // Draw key text
                val displayText = when {
                    isCapsLock -> key.primary.uppercase()
                    isShifted -> key.primary.uppercase()
                    else -> key.primary
                }

                textPaint.color = theme.keyTextColor
                val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(displayText, rect.centerX(), textY, textPaint)

                // Draw secondary text (long-press hint)
                if (key.secondary != null) {
                    textPaint.textSize = theme.fontSize * 0.6f * resources.displayMetrics.density
                    textPaint.color = theme.keySecondaryTextColor
                    canvas.drawText(
                        key.secondary,
                        rect.right - 10,
                        rect.top + 15,
                        textPaint
                    )
                    textPaint.textSize = theme.fontSize * resources.displayMetrics.density
                }

                keyRects.add(KeyRect(key, rect))
                x += keyWidth
            }
        }

        // Draw swipe trail
        if (swipePoints.size > 1) {
            val swipePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accentColor
                strokeWidth = 8f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }

            for (i in 0 until swipePoints.size - 1) {
                canvas.drawLine(
                    swipePoints[i].x,
                    swipePoints[i].y,
                    swipePoints[i + 1].x,
                    swipePoints[i + 1].y,
                    swipePaint
                )
            }
        }
    }

    private fun drawSuggestionStrip(canvas: Canvas) {
        val theme = currentTheme ?: return
        val stripHeight = height * 0.15f

        // Draw suggestion background
        val stripPaint = Paint().apply {
            color = Color.argb(50, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), stripHeight, stripPaint)

        // Draw suggestions
        if (suggestions.isNotEmpty()) {
            val suggestionWidth = width / suggestions.size.toFloat()

            suggestionPaint.color = theme.keyTextColor
            suggestionPaint.textSize = theme.fontSize * 0.9f * resources.displayMetrics.density

            suggestions.forEachIndexed { index, suggestion ->
                val x = (index + 0.5f) * suggestionWidth
                val y = stripHeight / 2 - (suggestionPaint.descent() + suggestionPaint.ascent()) / 2
                canvas.drawText(suggestion, x, y, suggestionPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                swipePoints.clear()
                swipePoints.add(PointF(event.x, event.y))
                swipeStartTime = System.currentTimeMillis()

                val key = findKeyAt(event.x, event.y)
                if (key != null) {
                    pressedKey = key
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                swipePoints.add(PointF(event.x, event.y))
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - swipeStartTime

                // Check if it was a swipe or tap
                if (swipePoints.size > 5 && duration < 1000) {
                    // It's a swipe
                    val normalizedPoints = swipePoints.map {
                        PointF(it.x / width, it.y / height)
                    }
                    swipeListener?.invoke(
                        SwipePath(
                            points = normalizedPoints,
                            startTime = swipeStartTime,
                            endTime = System.currentTimeMillis()
                        )
                    )
                } else {
                    // It's a tap - check if on suggestion
                    if (event.y < height * 0.15f) {
                        handleSuggestionTap(event.x)
                    } else {
                        // Regular key tap
                        pressedKey?.let { keyRect ->
                            handleKeyPress(keyRect.key)

                            // Reset shift after key press (unless caps lock)
                            if (isShifted && !isCapsLock) {
                                isShifted = false
                            }
                        }
                    }
                }

                swipePoints.clear()
                pressedKey = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findKeyAt(x: Float, y: Float): KeyRect? {
        return keyRects.find { it.rect.contains(x, y) }
    }

    private fun handleSuggestionTap(x: Float) {
        if (suggestions.isEmpty()) return

        val suggestionWidth = width / suggestions.size.toFloat()
        val index = (x / suggestionWidth).toInt()

        if (index in suggestions.indices) {
            keyListener?.invoke(suggestions[index], KeyType.CHARACTER)
        }
    }

    private fun handleKeyPress(key: Key) {
        when (key.keyType) {
            KeyType.CHARACTER -> {
                val text = if (isShifted || isCapsLock) {
                    key.primary.uppercase()
                } else {
                    key.primary
                }
                keyListener?.invoke(text, KeyType.CHARACTER)
            }
            KeyType.SHIFT -> {
                toggleShift()
            }
            KeyType.SYMBOL_SWITCH -> showSymbolLayout()
            KeyType.NUMBER_SWITCH -> showNumberLayout()
            KeyType.EMOJI_SWITCH -> showEmojiPanel()
            else -> keyListener?.invoke(key.primary, key.keyType)
        }
    }

    private fun createQwertyLayout(): KeyboardLayout {
        return KeyboardLayout(
            id = "qwerty",
            name = "QWERTY",
            layoutType = LayoutType.QWERTY,
            keys = listOf(
                // Row 1
                listOf(
                    Key("q", "1"), Key("w", "2"), Key("e", "3"), Key("r", "4"),
                    Key("t", "5"), Key("y", "6"), Key("u", "7"), Key("i", "8"),
                    Key("o", "9"), Key("p", "0")
                ),
                // Row 2
                listOf(
                    Key("a", "@"), Key("s", "#"), Key("d", "$"), Key("f", "%"),
                    Key("g", "&"), Key("h", "*"), Key("j", "("), Key("k", ")"),
                    Key("l", "!")
                ),
                // Row 3
                listOf(
                    Key("⇧", keyType = KeyType.SHIFT, width = 1.5f),
                    Key("z"), Key("x"), Key("c"), Key("v"),
                    Key("b"), Key("n"), Key("m"),
                    Key("⌫", keyType = KeyType.BACKSPACE, width = 1.5f)
                ),
                // Row 4
                listOf(
                    Key("123", keyType = KeyType.NUMBER_SWITCH, width = 1.5f),
                    Key("🌐", keyType = KeyType.LANGUAGE_SWITCH),
                    Key("Space", keyType = KeyType.SPACE, width = 4f),
                    Key("😊", keyType = KeyType.EMOJI_SWITCH),
                    Key("↵", keyType = KeyType.ENTER, width = 1.5f)
                )
            )
        )
    }

    private fun createSymbolLayout(): KeyboardLayout {
        return KeyboardLayout(
            id = "symbols",
            name = "Symbols",
            layoutType = LayoutType.SYMBOLS,
            keys = listOf(
                listOf(
                    Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
                    Key("6"), Key("7"), Key("8"), Key("9"), Key("0")
                ),
                listOf(
                    Key("@"), Key("#"), Key("$"), Key("%"), Key("&"),
                    Key("*"), Key("("), Key(")"), Key("!")
                ),
                listOf(
                    Key("+"), Key("-"), Key("="), Key("/"), Key("\\"),
                    Key(":"), Key(";"), Key(","), Key(".")
                ),
                listOf(
                    Key("ABC", keyType = KeyType.SYMBOL_SWITCH, width = 1.5f),
                    Key("🌐", keyType = KeyType.LANGUAGE_SWITCH),
                    Key("Space", keyType = KeyType.SPACE, width = 4f),
                    Key("😊", keyType = KeyType.EMOJI_SWITCH),
                    Key("↵", keyType = KeyType.ENTER, width = 1.5f)
                )
            )
        )
    }

    private fun createNumberLayout(): KeyboardLayout {
        return createSymbolLayout() // Same as symbols for now
    }

    private fun createEmojiLayout(): KeyboardLayout {
        return KeyboardLayout(
            id = "emoji",
            name = "Emoji",
            layoutType = LayoutType.EMOJI,
            keys = listOf(
                listOf(
                    Key("😀"), Key("😃"), Key("😄"), Key("😁"), Key("😆"),
                    Key("😅"), Key("🤣"), Key("😂"), Key("🙂"), Key("😊")
                ),
                listOf(
                    Key("😍"), Key("🥰"), Key("😘"), Key("😗"), Key("😙"),
                    Key("😚"), Key("😋"), Key("😛"), Key("😝"), Key("😜")
                ),
                listOf(
                    Key("🤪"), Key("🤨"), Key("🧐"), Key("🤓"), Key("😎"),
                    Key("🤩"), Key("🥳"), Key("😏"), Key("😒"), Key("😞")
                ),
                listOf(
                    Key("ABC", keyType = KeyType.SYMBOL_SWITCH, width = 1.5f),
                    Key("🌐", keyType = KeyType.LANGUAGE_SWITCH),
                    Key("Space", keyType = KeyType.SPACE, width = 4f),
                    Key("⌫", keyType = KeyType.BACKSPACE),
                    Key("↵", keyType = KeyType.ENTER, width = 1.5f)
                )
            )
        )
    }

    data class KeyRect(val key: Key, val rect: RectF)
}
