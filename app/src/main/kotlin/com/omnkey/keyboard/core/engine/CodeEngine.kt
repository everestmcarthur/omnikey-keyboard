package com.omnkey.keyboard.core.engine

import android.content.Context
import android.view.inputmethod.InputConnection
import com.omnkey.keyboard.core.database.OmniKeyDatabase
import com.omnkey.keyboard.core.model.Template

class CodeEngine(
    private val context: Context,
    private val database: OmniKeyDatabase
) {
    var isActive = false
        private set

    private var isTerminalMode = false
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    // Bracket pairs
    private val bracketPairs = mapOf(
        "(" to ")",
        "[" to "]",
        "{" to "}",
        "<" to ">",
        "\"" to "\"",
        "'" to "'",
        "`" to "`"
    )

    // Common code snippets
    private val snippets = mapOf(
        "for" to "for (let i = 0; i < \${1:length}; i++) {\n    \${2:// code}\n}",
        "if" to "if (\${1:condition}) {\n    \${2:// code}\n}",
        "func" to "function \${1:name}(\${2:params}) {\n    \${3:// code}\n}",
        "class" to "class \${1:ClassName} {\n    constructor(\${2:params}) {\n        \${3:// code}\n    }\n}",
        "try" to "try {\n    \${1:// code}\n} catch (error) {\n    \${2:// handle error}\n}",
        "switch" to "switch (\${1:expression}) {\n    case \${2:value}:\n        \${3:// code}\n        break;\n    default:\n        \${4:// code}\n}",
        "import" to "import \${2:module} from '\${1:path}';",
        "const" to "const \${1:name} = \${2:value};",
        "let" to "let \${1:name} = \${2:value};",
        "arrow" to "(\${1:params}) => \${2:expression}",
        "async" to "async (\${1:params}) => {\n    \${2:// code}\n}",
        "await" to "const \${1:result} = await \${2:promise};",
        "console" to "console.log(\${1:value});",
        "return" to "return \${1:value};",
        "export" to "export \${1:default} \${2:name};",
        "comment" to "/**\n * \${1:description}\n * @param {\${2:type}} \${3:name} - \${4:description}\n * @returns {\${5:type}} \${6:description}\n */",
        "todo" to "// TODO: \${1:description}",
        "fixme" to "// FIXME: \${1:description}"
    )

    // Terminal commands
    private val terminalCommands = listOf(
        "ls -la", "cd ", "pwd", "mkdir ", "rm -rf ", "cp ", "mv ", "cat ",
        "grep -r ", "find . -name ", "chmod ", "chown ", "sudo ", "apt install ",
        "git status", "git add .", "git commit -m \"\"", "git push", "git pull",
        "git log", "git diff", "git branch", "git checkout ",
        "npm install ", "npm run ", "yarn add ", "python ", "pip install ",
        "docker ps", "docker run ", "docker build ", "docker-compose up",
        "curl -X GET ", "wget ", "ssh ", "scp ", "tar -xzf ", "vim ", "nano "
    )

    fun enableCodeMode() {
        isActive = true
        isTerminalMode = false
    }

    fun enableTerminalMode() {
        isActive = true
        isTerminalMode = true
    }

    fun disableCodeMode() {
        isActive = false
        isTerminalMode = false
    }

    fun autoPairBrackets(char: String): String? {
        val closingBracket = bracketPairs[char] ?: return null
        return "$char$closingBracket"
    }

    fun smartDelete(textBefore: String, ic: InputConnection): Boolean {
        if (textBefore.isEmpty()) return false

        val lastChar = textBefore.last().toString()
        val closingBracket = bracketPairs[lastChar]

        if (closingBracket != null) {
            val textAfter = ic.getTextAfterCursor(1, 0)?.toString() ?: ""
            if (textAfter == closingBracket) {
                // Delete both opening and closing bracket
                ic.deleteSurroundingText(1, 1)
                return true
            }
        }

        return false
    }

    fun autoIndent(textBefore: String): String {
        val lastLine = textBefore.lines().lastOrNull() ?: return "\n"

        // Count leading whitespace
        val indent = lastLine.takeWhile { it.isWhitespace() }

        // Add extra indent after opening bracket or colon
        val extraIndent = if (lastLine.trimEnd().endsWith("{") ||
                              lastLine.trimEnd().endsWith(":") ||
                              lastLine.trimEnd().endsWith("(") ||
                              lastLine.trimEnd().endsWith("[")) {
            "    " // 4 spaces or configurable tab
        } else {
            ""
        }

        return "\n$indent$extraIndent"
    }

    fun getSnippet(trigger: String): String? {
        return snippets[trigger.lowercase()]
    }

    fun getTerminalSuggestions(input: String): List<String> {
        if (!isTerminalMode) return emptyList()
        return terminalCommands.filter { it.startsWith(input) }.take(5)
    }

    fun getCodeCompletions(input: String, language: String): List<String> {
        // Language-specific completions
        return when (language.lowercase()) {
            "javascript", "typescript" -> jsCompletions(input)
            "python" -> pythonCompletions(input)
            "java", "kotlin" -> javaKotlinCompletions(input)
            "html" -> htmlCompletions(input)
            "css" -> cssCompletions(input)
            else -> genericCompletions(input)
        }
    }

    private fun jsCompletions(input: String): List<String> {
        val keywords = listOf(
            "const", "let", "var", "function", "return", "if", "else", "for",
            "while", "do", "switch", "case", "break", "continue", "class",
            "extends", "import", "export", "default", "async", "await",
            "try", "catch", "throw", "new", "this", "super", "static"
        )
        return keywords.filter { it.startsWith(input) }
    }

    private fun pythonCompletions(input: String): List<String> {
        val keywords = listOf(
            "def", "class", "if", "elif", "else", "for", "while", "return",
            "import", "from", "as", "try", "except", "finally", "with",
            "lambda", "yield", "async", "await", "pass", "break", "continue"
        )
        return keywords.filter { it.startsWith(input) }
    }

    private fun javaKotlinCompletions(input: String): List<String> {
        val keywords = listOf(
            "public", "private", "protected", "class", "interface", "fun",
            "val", "var", "if", "else", "when", "for", "while", "return",
            "try", "catch", "finally", "throw", "throws", "override", "open"
        )
        return keywords.filter { it.startsWith(input) }
    }

    private fun htmlCompletions(input: String): List<String> {
        val tags = listOf(
            "<div>", "<span>", "<p>", "<a href=\"\">", "<img src=\"\">",
            "<button>", "<input type=\"\">", "<form>", "<table>", "<ul>",
            "<li>", "<h1>", "<h2>", "<h3>", "<section>", "<article>"
        )
        return tags.filter { it.startsWith(input) }
    }

    private fun cssCompletions(input: String): List<String> {
        val properties = listOf(
            "display:", "position:", "width:", "height:", "margin:",
            "padding:", "color:", "background:", "border:", "font-size:",
            "flex:", "grid:", "align-items:", "justify-content:"
        )
        return properties.filter { it.startsWith(input) }
    }

    private fun genericCompletions(input: String): List<String> {
        return snippets.keys.filter { it.startsWith(input) }
    }

    fun undo(ic: InputConnection) {
        if (undoStack.isNotEmpty()) {
            val lastState = undoStack.removeAt(undoStack.lastIndex)
            val currentText = ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
            redoStack.add(currentText)

            // Restore previous state
            ic.deleteSurroundingText(currentText.length, 0)
            ic.commitText(lastState, 1)
        }
    }

    fun redo(ic: InputConnection) {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.lastIndex)
            val currentText = ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
            undoStack.add(currentText)

            // Restore next state
            ic.deleteSurroundingText(currentText.length, 0)
            ic.commitText(nextState, 1)
        }
    }

    fun saveState(ic: InputConnection) {
        val currentText = ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
        undoStack.add(currentText)
        if (undoStack.size > 50) {
            undoStack.removeAt(0) // Keep only last 50 states
        }
        redoStack.clear()
    }

    // Utility functions
    fun convertCase(text: String, caseType: CaseType): String {
        return when (caseType) {
            CaseType.CAMEL_CASE -> toCamelCase(text)
            CaseType.SNAKE_CASE -> toSnakeCase(text)
            CaseType.KEBAB_CASE -> toKebabCase(text)
            CaseType.PASCAL_CASE -> toPascalCase(text)
            CaseType.UPPER_CASE -> text.uppercase()
            CaseType.LOWER_CASE -> text.lowercase()
        }
    }

    private fun toCamelCase(text: String): String {
        return text.split(Regex("[\\s_-]"))
            .mapIndexed { index, word ->
                if (index == 0) word.lowercase()
                else word.replaceFirstChar { it.uppercase() }
            }
            .joinToString("")
    }

    private fun toSnakeCase(text: String): String {
        return text.split(Regex("[\\s-]"))
            .joinToString("_") { it.lowercase() }
    }

    private fun toKebabCase(text: String): String {
        return text.split(Regex("[\\s_]"))
            .joinToString("-") { it.lowercase() }
    }

    private fun toPascalCase(text: String): String {
        return text.split(Regex("[\\s_-]"))
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

enum class CaseType {
    CAMEL_CASE,
    SNAKE_CASE,
    KEBAB_CASE,
    PASCAL_CASE,
    UPPER_CASE,
    LOWER_CASE
}
