package com.nsv.voice_demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log


class ContinuousSpeechManager(
    private val context: Context,
    private val listener: SpeechResultListener
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())

    private val recognizerIntent: Intent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")

            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 100L)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 100L)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 500L)
        }
    }

    private val TAG = "ContinuousSpeechManager"

    interface SpeechResultListener {
        fun onSpeechResult(text: String)
        fun onSpeechError(message: String)
    }

    // --- Setup ---
    fun setup() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@ContinuousSpeechManager)
            }
        } else {
            listener.onSpeechError("Speech recognition not available on this device.")
        }
    }

    // --- Start Listening ---
    fun startListening() {
        if (speechRecognizer == null) setup()
        if (isListening) return

        isListening = true
        handler.post {
            try {
                speechRecognizer?.startListening(recognizerIntent)
                Log.d(TAG, "Listening started")
            } catch (e: Exception) {
                Log.e(TAG, "startListening failed: ${e.message}")
                isListening = false
            }
        }
    }

    // --- Stop Listening ---
    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "stopListening failed: ${e.message}")
        }
        isListening = false
        Log.d(TAG, "Listening stopped")
    }

    // --- Cleanup ---
    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d(TAG, "Recognizer destroyed")
    }

    // --- RecognitionListener ---
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "User started speaking")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Hook: show mic animation
    }

    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
    }

    override fun onResults(results: Bundle) {
        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
            listener.onSpeechResult(it)
        }
        restartListening()
    }

    override fun onPartialResults(partialResults: Bundle) {
        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
            listener.onSpeechResult(it) // real-time transcription
        }
    }

    override fun onError(errorCode: Int) {
        val errorMessage = getErrorText(errorCode)
        listener.onSpeechError("Error: $errorMessage (Code: $errorCode)")
        Log.e(TAG, "Error: $errorMessage (Code: $errorCode)")

        if (errorCode != SpeechRecognizer.ERROR_CLIENT &&
            errorCode != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
        ) {
            restartListening()
        } else {
            isListening = false
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- Helpers ---
    private fun restartListening() {
        stopListening()
        handler.postDelayed({ startListening() }, 150) // short delay prevents busy errors
    }

    private fun getErrorText(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Unknown error"
    }
}