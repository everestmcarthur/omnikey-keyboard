package com.omnkey.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.lifecycle.lifecycleScope
import com.omnkey.keyboard.OmniKeyApplication
import com.omnkey.keyboard.core.engine.*
import com.omnkey.keyboard.ui.keyboard.KeyboardView
import kotlinx.coroutines.launch

class OmniKeyService : InputMethodService() {

    private lateinit var app: OmniKeyApplication
    private lateinit var keyboardView: KeyboardView

    // Core engines
    private lateinit var predictionEngine: PredictionEngine
    private lateinit var swipeEngine: SwipeEngine
    private lateinit var autocorrectEngine: AutocorrectEngine
    private lateinit var textReplacementEngine: TextReplacementEngine
    private lateinit var gestureEngine: GestureEngine
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var themeEngine: ThemeEngine
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var mlEngine: MLEngine
    private lateinit var codeEngine: CodeEngine
    private lateinit var writerEngine: WriterEngine
    private lateinit var smartDetector: SmartContextDetector

    // State
    private var currentInputConnection: InputConnection? = null
    private var currentEditorInfo: EditorInfo? = null
    private var currentContext: SmartContextDetector.Context? = null
    private var isIncognitoMode = false

    override fun onCreate() {
        super.onCreate()
        app = application as OmniKeyApplication
        initializeEngines()
    }

    private fun initializeEngines() {
        predictionEngine = PredictionEngine(this, app.database)
        swipeEngine = SwipeEngine(this)
        autocorrectEngine = AutocorrectEngine(this, app.database)
        textReplacementEngine = TextReplacementEngine(app.database)
        gestureEngine = GestureEngine(this, app.database)
        clipboardManager = ClipboardManager(this, app.database)
        themeEngine = ThemeEngine(this, app.database)
        voiceEngine = VoiceEngine(this)
        mlEngine = MLEngine(this)
        codeEngine = CodeEngine(this, app.database)
        writerEngine = WriterEngine(this, app.database)
        smartDetector = SmartContextDetector()
    }

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this).apply {
            setOnKeyListener { key, keyType ->
                handleKeyPress(key, keyType)
            }
            setOnSwipeListener { path ->
                lifecycleScope.launch {
                    handleSwipe(path)
                }
            }
            setOnGestureListener { gesture ->
                handleGesture(gesture)
            }
        }

        lifecycleScope.launch {
            loadCurrentTheme()
            loadGestures()
        }

        return keyboardView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentEditorInfo = attribute
        currentInputConnection = currentInputConnection

        lifecycleScope.launch {
            detectInputMode(attribute)
            updateKeyboardLayout(attribute)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        lifecycleScope.launch {
            // Detect smart context
            val packageName = info?.packageName ?: ""
            currentContext = smartDetector.detectContext(
                info,
                currentInputConnection,
                packageName
            )

            // Apply context-based optimizations
            applyContextOptimizations(currentContext!!)

            // Load app-specific settings
            loadAppSpecificSettings(packageName)

            // Check for incognito mode
            isIncognitoMode = app.preferenceManager.incognitoMode.get() ||
                             isPrivateField(info) ||
                             smartDetector.shouldDisableLearning(currentContext!!)

            // Update keyboard based on context
            updateKeyboardForContext(currentContext!!)
        }
    }

    private suspend fun detectInputMode(info: EditorInfo?) {
        val inputType = info?.inputType ?: return
        val packageName = info.packageName ?: ""

        when {
            // Terminal detection
            isTerminalApp(packageName) -> {
                if (app.preferenceManager.terminalMode.get()) {
                    codeEngine.enableTerminalMode()
                }
            }
            // Code editor detection
            isCodeEditor(packageName) || isCodeField(info) -> {
                if (app.preferenceManager.codeAutoComplete.get()) {
                    codeEngine.enableCodeMode()
                }
            }
            // Writing app detection
            isWritingApp(packageName) -> {
                if (app.preferenceManager.grammarCheck.get()) {
                    writerEngine.enableWritingMode()
                }
            }
            // Email/formal writing
            isEmailField(inputType) -> {
                writerEngine.enableFormalMode()
            }
        }
    }

    private fun handleKeyPress(key: String, keyType: KeyType) {
        val ic = currentInputConnection ?: return

        when (keyType) {
            KeyType.CHARACTER -> {
                lifecycleScope.launch {
                    val processedText = processText(key)
                    ic.commitText(processedText, 1)

                    if (!isIncognitoMode) {
                        learnFromInput(processedText)
                    }

                    updatePredictions()
                }
            }
            KeyType.BACKSPACE -> handleBackspace(ic)
            KeyType.ENTER -> handleEnter(ic)
            KeyType.SPACE -> handleSpace(ic)
            KeyType.SHIFT -> handleShift()
            else -> handleSpecialKey(keyType, ic)
        }

        playHaptic()
        playSound()
    }

    private suspend fun processText(text: String): String {
        var processed = text

        // Text replacement
        if (app.preferenceManager.textReplacementEnabled.get()) {
            processed = textReplacementEngine.process(processed, getContextBefore())
        }

        // Auto-capitalization
        if (app.preferenceManager.autoCapitalization.get()) {
            processed = autocorrectEngine.capitalizeIfNeeded(processed, getContextBefore())
        }

        // Smart quotes
        if (writerEngine.isActive && app.preferenceManager.markdownShortcuts.get()) {
            processed = writerEngine.processSmartQuotes(processed, getContextBefore())
        }

        // Code auto-pairing
        if (codeEngine.isActive && app.preferenceManager.bracketMatching.get()) {
            val paired = codeEngine.autoPairBrackets(processed)
            if (paired != null) {
                currentInputConnection?.commitText(paired, 0)
                return ""
            }
        }

        return processed
    }

    private suspend fun handleSwipe(path: SwipePath) {
        if (!app.preferenceManager.swipeTypingEnabled.get()) return

        val word = swipeEngine.recognizeGesture(path)
        if (word != null) {
            currentInputConnection?.commitText(word, 1)

            if (!isIncognitoMode) {
                learnFromInput(word)
            }

            updatePredictions()
        }
    }

    private fun handleGesture(gesture: GestureType) {
        lifecycleScope.launch {
            val action = gestureEngine.getAction(gesture)
            executeAction(action)
        }
    }

    private fun handleBackspace(ic: InputConnection) {
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""

        // Smart delete for code (remove paired brackets)
        if (codeEngine.isActive) {
            val deleted = codeEngine.smartDelete(textBefore, ic)
            if (deleted) return
        }

        // Delete by word on long press (handled by gesture)
        ic.deleteSurroundingText(1, 0)
    }

    private suspend fun handleSpace(ic: InputConnection) {
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""

        // Double space for period
        if (app.preferenceManager.doubleSpaceForPeriod.get() && textBefore.endsWith(" ")) {
            ic.deleteSurroundingText(1, 0)
            ic.commitText(". ", 1)
            return
        }

        // Auto-correction on space
        if (app.preferenceManager.autoCorrection.get()) {
            val lastWord = getLastWord(textBefore)
            val correction = autocorrectEngine.getCorrection(lastWord)
            if (correction != null && correction != lastWord) {
                ic.deleteSurroundingText(lastWord.length, 0)
                ic.commitText("$correction ", 1)
                return
            }
        }

        ic.commitText(" ", 1)
        updatePredictions()
    }

    private fun handleEnter(ic: InputConnection) {
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""

        // Auto-indent for code
        if (codeEngine.isActive && app.preferenceManager.autoIndent.get()) {
            val indented = codeEngine.autoIndent(textBefore)
            ic.commitText(indented, 1)
            return
        }

        ic.commitText("\n", 1)
    }

    private suspend fun updatePredictions() {
        if (!app.preferenceManager.suggestionStripEnabled.get()) return

        val context = getContextBefore()
        val predictions = predictionEngine.getPredictions(context)

        keyboardView.updateSuggestions(predictions)
    }

    private suspend fun learnFromInput(text: String) {
        predictionEngine.learn(text, getContextBefore())
        autocorrectEngine.learn(text)
    }

    private suspend fun executeAction(action: ActionType?) {
        action ?: return
        val ic = currentInputConnection ?: return

        when (action) {
            ActionType.DELETE_WORD -> deleteWord(ic)
            ActionType.DELETE_LINE -> deleteLine(ic)
            ActionType.MOVE_CURSOR_LEFT -> moveCursor(ic, -1)
            ActionType.MOVE_CURSOR_RIGHT -> moveCursor(ic, 1)
            ActionType.MOVE_CURSOR_WORD_LEFT -> moveCursorByWord(ic, -1)
            ActionType.MOVE_CURSOR_WORD_RIGHT -> moveCursorByWord(ic, 1)
            ActionType.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)
            ActionType.COPY -> ic.performContextMenuAction(android.R.id.copy)
            ActionType.CUT -> ic.performContextMenuAction(android.R.id.cut)
            ActionType.PASTE -> ic.performContextMenuAction(android.R.id.paste)
            ActionType.UNDO -> codeEngine.undo(ic)
            ActionType.REDO -> codeEngine.redo(ic)
            else -> {}
        }
    }

    private fun getContextBefore(): String {
        return currentInputConnection?.getTextBeforeCursor(100, 0)?.toString() ?: ""
    }

    private fun getLastWord(text: String): String {
        return text.trim().split(Regex("\\s+")).lastOrNull() ?: ""
    }

    private fun playHaptic() {
        lifecycleScope.launch {
            if (app.preferenceManager.hapticFeedback.get()) {
                val strength = app.preferenceManager.hapticStrength.get()
                HapticHelper.vibrate(this@OmniKeyService, strength)
            }
        }
    }

    private fun playSound() {
        lifecycleScope.launch {
            if (app.preferenceManager.soundEnabled.get()) {
                val volume = app.preferenceManager.soundVolume.get()
                val mechanical = app.preferenceManager.mechanicalKeyboardSound.get()
                SoundHelper.playKeySound(this@OmniKeyService, volume, mechanical)
            }
        }
    }

    private suspend fun loadCurrentTheme() {
        val themeId = app.preferenceManager.currentThemeId.get()
        val theme = app.database.themeDao().getThemeById(themeId)
        theme?.let { keyboardView.applyTheme(it) }
    }

    private suspend fun loadGestures() {
        gestureEngine.loadGestures()
    }

    private suspend fun loadAppSpecificSettings(packageName: String) {
        // TODO: Implement per-app settings
    }

    private fun isPrivateField(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        return (inputType and EditorInfo.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
               (inputType and EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0
    }

    private fun isTerminalApp(packageName: String): Boolean {
        return packageName.contains("terminal", ignoreCase = true) ||
               packageName.contains("termux", ignoreCase = true) ||
               packageName.contains("shell", ignoreCase = true)
    }

    private fun isCodeEditor(packageName: String): Boolean {
        return packageName.contains("code", ignoreCase = true) ||
               packageName.contains("editor", ignoreCase = true) ||
               packageName.contains("ide", ignoreCase = true)
    }

    private fun isCodeField(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        return (inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
    }

    private fun isWritingApp(packageName: String): Boolean {
        return packageName.contains("docs", ignoreCase = true) ||
               packageName.contains("word", ignoreCase = true) ||
               packageName.contains("writer", ignoreCase = true) ||
               packageName.contains("note", ignoreCase = true)
    }

    private fun isEmailField(inputType: Int): Boolean {
        return (inputType and EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0
    }

    private fun deleteWord(ic: InputConnection) {
        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: return
        val lastWord = getLastWord(textBefore)
        if (lastWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastWord.length, 0)
        }
    }

    private fun deleteLine(ic: InputConnection) {
        val textBefore = ic.getTextBeforeCursor(10000, 0)?.toString() ?: return
        val lastLineBreak = textBefore.lastIndexOf('\n')
        val deleteCount = if (lastLineBreak >= 0) {
            textBefore.length - lastLineBreak - 1
        } else {
            textBefore.length
        }
        ic.deleteSurroundingText(deleteCount, 0)
    }

    private fun moveCursor(ic: InputConnection, offset: Int) {
        val selection = ic.getSelectedText(0)
        if (selection != null && selection.isNotEmpty()) {
            ic.setSelection(0, 0)
        } else {
            ic.commitText("", offset)
        }
    }

    private fun moveCursorByWord(ic: InputConnection, direction: Int) {
        val text = if (direction < 0) {
            ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        } else {
            ic.getTextAfterCursor(100, 0)?.toString() ?: ""
        }

        val words = text.split(Regex("\\s+"))
        val wordLength = words.firstOrNull()?.length ?: 0
        moveCursor(ic, if (direction < 0) -wordLength else wordLength)
    }

    private fun handleShift() {
        keyboardView.toggleShift()
    }

    private fun handleSpecialKey(keyType: KeyType, ic: InputConnection) {
        when (keyType) {
            KeyType.EMOJI_SWITCH -> keyboardView.showEmojiPanel()
            KeyType.SYMBOL_SWITCH -> keyboardView.showSymbolLayout()
            KeyType.NUMBER_SWITCH -> keyboardView.showNumberLayout()
            KeyType.LANGUAGE_SWITCH -> showLanguageSwitcher()
            KeyType.VOICE -> voiceEngine.startVoiceInput()
            KeyType.CLIPBOARD -> clipboardManager.showClipboardPanel()
            KeyType.SETTINGS -> launchSettings()
            else -> {}
        }
    }

    private fun showLanguageSwitcher() {
        // TODO: Implement language switcher
    }

    private fun launchSettings() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}

data class SwipePath(
    val points: List<android.graphics.PointF>,
    val startTime: Long,
    val endTime: Long
)

    private suspend fun applyContextOptimizations(context: SmartContextDetector.Context) {
        when (context.mode) {
            SmartContextDetector.InputMode.CODE,
            SmartContextDetector.InputMode.TERMINAL -> {
                if (app.preferenceManager.codeAutoComplete.get()) {
                    codeEngine.enableCodeMode()
                }
            }
            SmartContextDetector.InputMode.FORMAL_WRITING,
            SmartContextDetector.InputMode.MARKDOWN -> {
                if (app.preferenceManager.grammarCheck.get()) {
                    writerEngine.enableWritingMode()
                }
            }
            SmartContextDetector.InputMode.PASSWORD -> {
                // Disable all learning and suggestions
            }
            else -> {
                // Normal mode
            }
        }
    }

    private fun updateKeyboardForContext(context: SmartContextDetector.Context) {
        when (context.layoutOptimization) {
            SmartContextDetector.LayoutOptimization.NUMERIC -> {
                keyboardView.showNumberLayout()
            }
            SmartContextDetector.LayoutOptimization.CODE_SYMBOLS -> {
                keyboardView.showSymbolLayout()
            }
            else -> {
                keyboardView.showQwertyLayout()
            }
        }
    }
}
