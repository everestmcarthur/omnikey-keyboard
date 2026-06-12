package com.omnkey.keyboard.core.engine

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class SmartContextDetector {

    data class Context(
        val mode: InputMode,
        val sensitivity: SensitivityLevel,
        val suggestionsType: SuggestionsType,
        val layoutOptimization: LayoutOptimization
    )

    enum class InputMode {
        NORMAL,
        CODE,
        TERMINAL,
        PASSWORD,
        EMAIL,
        URL,
        NUMBER,
        PHONE,
        CREDIT_CARD,
        ADDRESS,
        NAME,
        SEARCH,
        MESSAGE,
        FORMAL_WRITING,
        MARKDOWN,
        JSON,
        SQL
    }

    enum class SensitivityLevel {
        PUBLIC,      // Normal typing
        PRIVATE,     // Passwords, credit cards
        SENSITIVE    // Banking, medical
    }

    enum class SuggestionsType {
        FULL,           // Full predictions
        CONSERVATIVE,   // Basic only
        NONE            // Disabled
    }

    enum class LayoutOptimization {
        STANDARD,
        NUMERIC,
        SYMBOLS,
        CODE_SYMBOLS,
        EMAIL_URL
    }

    fun detectContext(
        editorInfo: EditorInfo?,
        inputConnection: InputConnection?,
        packageName: String
    ): Context {
        // Detect from input type
        val inputType = editorInfo?.inputType ?: 0

        // Password detection
        if (isPasswordField(inputType)) {
            return Context(
                mode = InputMode.PASSWORD,
                sensitivity = SensitivityLevel.PRIVATE,
                suggestionsType = SuggestionsType.NONE,
                layoutOptimization = LayoutOptimization.STANDARD
            )
        }

        // Email detection
        if (isEmailField(inputType)) {
            return Context(
                mode = InputMode.EMAIL,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.CONSERVATIVE,
                layoutOptimization = LayoutOptimization.EMAIL_URL
            )
        }

        // URL detection
        if (isUrlField(inputType)) {
            return Context(
                mode = InputMode.URL,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.CONSERVATIVE,
                layoutOptimization = LayoutOptimization.EMAIL_URL
            )
        }

        // Number detection
        if (isNumberField(inputType)) {
            return Context(
                mode = InputMode.NUMBER,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.NONE,
                layoutOptimization = LayoutOptimization.NUMERIC
            )
        }

        // Phone detection
        if (isPhoneField(inputType)) {
            return Context(
                mode = InputMode.PHONE,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.NONE,
                layoutOptimization = LayoutOptimization.NUMERIC
            )
        }

        // App-based detection
        when {
            isTerminalApp(packageName) -> return Context(
                mode = InputMode.TERMINAL,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.CONSERVATIVE,
                layoutOptimization = LayoutOptimization.CODE_SYMBOLS
            )

            isCodeEditor(packageName) -> return Context(
                mode = InputMode.CODE,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.CONSERVATIVE,
                layoutOptimization = LayoutOptimization.CODE_SYMBOLS
            )

            isBankingApp(packageName) -> return Context(
                mode = InputMode.NORMAL,
                sensitivity = SensitivityLevel.SENSITIVE,
                suggestionsType = SuggestionsType.NONE,
                layoutOptimization = LayoutOptimization.STANDARD
            )

            isMedicalApp(packageName) -> return Context(
                mode = InputMode.NORMAL,
                sensitivity = SensitivityLevel.SENSITIVE,
                suggestionsType = SuggestionsType.NONE,
                layoutOptimization = LayoutOptimization.STANDARD
            )

            isMessagingApp(packageName) -> return Context(
                mode = InputMode.MESSAGE,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.FULL,
                layoutOptimization = LayoutOptimization.STANDARD
            )

            isWritingApp(packageName) -> return Context(
                mode = InputMode.FORMAL_WRITING,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.FULL,
                layoutOptimization = LayoutOptimization.STANDARD
            )
        }

        // Content-based detection
        val textBefore = inputConnection?.getTextBeforeCursor(200, 0)?.toString() ?: ""
        val textAfter = inputConnection?.getTextAfterCursor(100, 0)?.toString() ?: ""
        val fullContext = textBefore + textAfter

        when {
            detectsCreditCard(textBefore) -> return Context(
                mode = InputMode.CREDIT_CARD,
                sensitivity = SensitivityLevel.PRIVATE,
                suggestionsType = SuggestionsType.NONE,
                layoutOptimization = LayoutOptimization.NUMERIC
            )

            detectsMarkdown(fullContext) -> return Context(
                mode = InputMode.MARKDOWN,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.FULL,
                layoutOptimization = LayoutOptimization.STANDARD
            )

            detectsJson(fullContext) -> return Context(
                mode = InputMode.JSON,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.CONSERVATIVE,
                layoutOptimization = LayoutOptimization.CODE_SYMBOLS
            )

            detectsSql(fullContext) -> return Context(
                mode = InputMode.SQL,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.CONSERVATIVE,
                layoutOptimization = LayoutOptimization.CODE_SYMBOLS
            )

            detectsCode(fullContext) -> return Context(
                mode = InputMode.CODE,
                sensitivity = SensitivityLevel.PUBLIC,
                suggestionsType = SuggestionsType.CONSERVATIVE,
                layoutOptimization = LayoutOptimization.CODE_SYMBOLS
            )
        }

        // Default context
        return Context(
            mode = InputMode.NORMAL,
            sensitivity = SensitivityLevel.PUBLIC,
            suggestionsType = SuggestionsType.FULL,
            layoutOptimization = LayoutOptimization.STANDARD
        )
    }

    private fun isPasswordField(inputType: Int): Boolean {
        return (inputType and EditorInfo.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
               (inputType and EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0 ||
               (inputType and EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
               (inputType and EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD) != 0
    }

    private fun isEmailField(inputType: Int): Boolean {
        return (inputType and EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 ||
               (inputType and EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) != 0
    }

    private fun isUrlField(inputType: Int): Boolean {
        return (inputType and EditorInfo.TYPE_TEXT_VARIATION_URI) != 0
    }

    private fun isNumberField(inputType: Int): Boolean {
        val numberType = inputType and EditorInfo.TYPE_MASK_CLASS
        return numberType == EditorInfo.TYPE_CLASS_NUMBER
    }

    private fun isPhoneField(inputType: Int): Boolean {
        return (inputType and EditorInfo.TYPE_CLASS_PHONE) != 0
    }

    private fun isTerminalApp(packageName: String): Boolean {
        return packageName.contains("terminal", ignoreCase = true) ||
               packageName.contains("termux", ignoreCase = true) ||
               packageName.contains("shell", ignoreCase = true) ||
               packageName.contains("ssh", ignoreCase = true)
    }

    private fun isCodeEditor(packageName: String): Boolean {
        return packageName.contains("code", ignoreCase = true) ||
               packageName.contains("editor", ignoreCase = true) ||
               packageName.contains("ide", ignoreCase = true) ||
               packageName.contains("spck", ignoreCase = true) ||
               packageName.contains("acode", ignoreCase = true)
    }

    private fun isBankingApp(packageName: String): Boolean {
        return packageName.contains("bank", ignoreCase = true) ||
               packageName.contains("paypal", ignoreCase = true) ||
               packageName.contains("venmo", ignoreCase = true) ||
               packageName.contains("chase", ignoreCase = true) ||
               packageName.contains("wellsfargo", ignoreCase = true) ||
               packageName.contains("finance", ignoreCase = true)
    }

    private fun isMedicalApp(packageName: String): Boolean {
        return packageName.contains("health", ignoreCase = true) ||
               packageName.contains("medical", ignoreCase = true) ||
               packageName.contains("doctor", ignoreCase = true) ||
               packageName.contains("pharmacy", ignoreCase = true)
    }

    private fun isMessagingApp(packageName: String): Boolean {
        return packageName.contains("message", ignoreCase = true) ||
               packageName.contains("whatsapp", ignoreCase = true) ||
               packageName.contains("telegram", ignoreCase = true) ||
               packageName.contains("discord", ignoreCase = true) ||
               packageName.contains("slack", ignoreCase = true) ||
               packageName.contains("signal", ignoreCase = true)
    }

    private fun isWritingApp(packageName: String): Boolean {
        return packageName.contains("docs", ignoreCase = true) ||
               packageName.contains("word", ignoreCase = true) ||
               packageName.contains("writer", ignoreCase = true) ||
               packageName.contains("note", ignoreCase = true) ||
               packageName.contains("notion", ignoreCase = true)
    }

    private fun detectsCreditCard(text: String): Boolean {
        // Look for patterns like: 1234 5678 9012 3456
        val ccPattern = Regex("\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{0,4}")
        return ccPattern.containsMatchIn(text)
    }

    private fun detectsMarkdown(text: String): Boolean {
        val mdPatterns = listOf(
            Regex("^#{1,6}\\s"),     // Headers
            Regex("\\[.*]\\(.*\\)"),  // Links
            Regex("```"),             // Code blocks
            Regex("^[-*+]\\s"),      // Lists
            Regex("\\*\\*.*\\*\\*"), // Bold
            Regex("__.*__")          // Bold alt
        )
        return mdPatterns.any { it.containsMatchIn(text) }
    }

    private fun detectsJson(text: String): Boolean {
        val jsonIndicators = listOf("{", "}", "[", "]", ":", "\"")
        val count = jsonIndicators.count { text.contains(it) }
        return count >= 3 && (text.contains("\"key\"") || text.contains("\"value\""))
    }

    private fun detectsSql(text: String): Boolean {
        val sqlKeywords = listOf(
            "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE",
            "CREATE", "TABLE", "JOIN", "GROUP BY", "ORDER BY"
        )
        return sqlKeywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun detectsCode(text: String): Boolean {
        val codeIndicators = listOf(
            Regex("function\\s+\\w+\\("), // JS/TS functions
            Regex("def\\s+\\w+\\("),      // Python functions
            Regex("class\\s+\\w+"),       // Classes
            Regex("import\\s+"),          // Imports
            Regex("const\\s+\\w+\\s*="),  // JS consts
            Regex("let\\s+\\w+\\s*="),    // JS lets
            Regex("var\\s+\\w+\\s*="),    // JS vars
            Regex("=>"),                   // Arrow functions
            Regex("\\w+\\.\\w+\\(")       // Method calls
        )
        return codeIndicators.any { it.containsMatchIn(text) }
    }

    fun shouldDisableLearning(context: Context): Boolean {
        return context.sensitivity != SensitivityLevel.PUBLIC
    }

    fun shouldDisableClipboard(context: Context): Boolean {
        return context.mode == InputMode.PASSWORD ||
               context.sensitivity == SensitivityLevel.SENSITIVE
    }

    fun shouldUseMonospaceFont(context: Context): Boolean {
        return context.mode in listOf(
            InputMode.CODE,
            InputMode.TERMINAL,
            InputMode.JSON,
            InputMode.SQL
        )
    }

    fun getSuggestedLanguage(context: Context): String? {
        return when (context.mode) {
            InputMode.CODE -> detectProgrammingLanguage(context)
            else -> null
        }
    }

    private fun detectProgrammingLanguage(context: Context): String {
        // Would analyze syntax patterns to determine language
        return "javascript" // Placeholder
    }
}
