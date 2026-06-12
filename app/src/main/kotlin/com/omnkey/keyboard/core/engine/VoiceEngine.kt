package com.omnkey.keyboard.core.engine

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale

class VoiceEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val resultChannel = Channel<VoiceResult>()

    fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            resultChannel.trySend(VoiceResult.Error("Speech recognition not available"))
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopVoiceInput() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun getResults(): Flow<VoiceResult> = resultChannel.receiveAsFlow()

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            resultChannel.trySend(VoiceResult.Ready)
        }

        override fun onBeginningOfSpeech() {
            resultChannel.trySend(VoiceResult.Listening)
        }

        override fun onRmsChanged(rmsdB: Float) {
            resultChannel.trySend(VoiceResult.VolumeChanged(rmsdB))
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            resultChannel.trySend(VoiceResult.Processing)
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error"
            }
            resultChannel.trySend(VoiceResult.Error(message))
        }

        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

            if (matches != null && matches.isNotEmpty()) {
                resultChannel.trySend(VoiceResult.Success(matches, confidence?.toList() ?: emptyList()))
            }
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches != null && matches.isNotEmpty()) {
                resultChannel.trySend(VoiceResult.PartialResult(matches.first()))
            }
        }

        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }
}

sealed class VoiceResult {
    object Ready : VoiceResult()
    object Listening : VoiceResult()
    object Processing : VoiceResult()
    data class VolumeChanged(val volume: Float) : VoiceResult()
    data class PartialResult(val text: String) : VoiceResult()
    data class Success(val matches: List<String>, val confidence: List<Float>) : VoiceResult()
    data class Error(val message: String) : VoiceResult()
}
