package com.omnkey.keyboard.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.omnkey.keyboard.OmniKeyApplication
import com.omnkey.keyboard.R
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var app: OmniKeyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = application as OmniKeyApplication

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Title
        layout.addView(TextView(this).apply {
            text = "OmniKey Keyboard"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        })

        // Enable keyboard button
        layout.addView(Button(this).apply {
            text = "Enable Keyboard"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        })

        // Select keyboard button
        layout.addView(Button(this).apply {
            text = "Select Keyboard"
            setOnClickListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        })

        // Appearance section
        layout.addView(TextView(this).apply {
            text = "\nAppearance"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        })

        // Haptic feedback
        layout.addView(createToggle("Haptic Feedback", app.preferenceManager.hapticFeedback))

        // Sound effects
        layout.addView(createToggle("Sound Effects", app.preferenceManager.soundEnabled))

        // Typing section
        layout.addView(TextView(this).apply {
            text = "\nTyping"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        })

        // Auto-correction
        layout.addView(createToggle("Auto-correction", app.preferenceManager.autoCorrection))

        // Swipe typing
        layout.addView(createToggle("Swipe Typing", app.preferenceManager.swipeTypingEnabled))

        // Suggestions
        layout.addView(createToggle("Word Suggestions", app.preferenceManager.suggestionStripEnabled))

        // Text replacement
        layout.addView(createToggle("Text Replacement", app.preferenceManager.textReplacementEnabled))

        // Developer section
        layout.addView(TextView(this).apply {
            text = "\nDeveloper"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        })

        // Code auto-complete
        layout.addView(createToggle("Code Auto-complete", app.preferenceManager.codeAutoComplete))

        // Terminal mode
        layout.addView(createToggle("Terminal Mode", app.preferenceManager.terminalMode))

        // Writer section
        layout.addView(TextView(this).apply {
            text = "\nWriter"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        })

        // Grammar check
        layout.addView(createToggle("Grammar Check", app.preferenceManager.grammarCheck))

        // Markdown shortcuts
        layout.addView(createToggle("Markdown Shortcuts", app.preferenceManager.markdownShortcuts))

        // Privacy section
        layout.addView(TextView(this).apply {
            text = "\nPrivacy"
            textSize = 18f
            setPadding(0, 32, 0, 16)
        })

        // Incognito mode
        layout.addView(createToggle("Incognito Mode", app.preferenceManager.incognitoMode))

        // Clipboard history
        layout.addView(createToggle("Clipboard History", app.preferenceManager.clipboardHistory))

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        setContentView(scrollView)
    }

    private fun createToggle(label: String, preference: com.omnkey.keyboard.core.preferences.PreferenceProperty<Boolean>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)

            addView(TextView(this@MainActivity).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })

            addView(Switch(this@MainActivity).apply {
                lifecycleScope.launch {
                    isChecked = preference.get()
                }

                setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch {
                        preference.set(isChecked)
                    }
                }
            })
        }
    }
}
