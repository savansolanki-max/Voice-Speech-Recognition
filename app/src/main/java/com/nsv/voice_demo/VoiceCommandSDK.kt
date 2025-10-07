package com.nsv.voice_demo


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * VoiceCommandSDK: Global Speech-to-Command SDK
 * - Continuous listening
 * - Fast response (partial results)
 * - Auto-restart on errors/silence
 * - Can be used in any app
 */
class VoiceCommandSDK(
    private val context: Context,
    private val listener: VoiceCommandListener
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())

    private val TAG = "VOICE_SDK"

    /** Initialize the SDK, must call before startListening() */
    fun init() {
        // Check microphone permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (context is Activity) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    101
                )
            } else {
                throw Exception("RECORD_AUDIO permission required")
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 300)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 300)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listener.onReady()
            }

            override fun onBeginningOfSpeech() {
                listener.onSpeechStart()
            }

            override fun onRmsChanged(rmsdB: Float) {
                listener.onVolumeChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { restartListening() }
            override fun onError(error: Int) { listener.onError(error); restartListening() }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, matches!![0].toString())
                if (!matches.isNullOrEmpty()) listener.onCommand(matches[0])
                restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty()) listener.onPartialCommand(partial[0])
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    /** Start listening continuously */
    fun startListening() {
        if (!isListening) {
            isListening = true
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Stop listening */
    fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) { e.printStackTrace() }
    }

    /** Destroy recognizer, call in Activity.onDestroy() */
    fun destroy() {
        isListening = false
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) { e.printStackTrace() }
    }

    /** Restart recognizer safely with small delay */
    private fun restartListening() {
        handler.postDelayed({
            if (isListening) {
                try {
                    speechRecognizer?.cancel()
                    speechRecognizer?.startListening(recognizerIntent)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }, 200) // fast restart
    }

    /** Listener interface for commands */
    interface VoiceCommandListener {
        fun onReady()
        fun onSpeechStart()
        fun onVolumeChanged(rms: Float)
        fun onPartialCommand(command: String)
        fun onCommand(command: String)
        fun onError(error: Int)
    }
}
