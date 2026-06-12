package com.omnkey.keyboard.core.engine

import android.content.Context
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import kotlinx.coroutines.tasks.await

class MLEngine(private val context: Context) {

    private val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()
    private val translators = mutableMapOf<Pair<String, String>, Translator>()

    suspend fun detectLanguage(text: String): String? {
        return try {
            val languageCode = languageIdentifier.identifyLanguage(text).await()
            if (languageCode != "und") languageCode else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun translate(text: String, sourceLang: String, targetLang: String): String? {
        val key = sourceLang to targetLang
        val translator = translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }

        return try {
            // Download model if needed
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSuggestedLanguages(text: String): List<Pair<String, Float>> {
        return try {
            val result = languageIdentifier.identifyPossibleLanguages(text).await()
            result.map { it.languageTag to it.confidence }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSupportedLanguages(): List<String> {
        return TranslateLanguage.getAllLanguages()
    }

    fun cleanup() {
        translators.values.forEach { it.close() }
        translators.clear()
        languageIdentifier.close()
    }

    // Handwriting recognition would use Digital Ink Recognition
    // OCR would use ML Kit Text Recognition
    // These require more complex setup with model downloads
}
