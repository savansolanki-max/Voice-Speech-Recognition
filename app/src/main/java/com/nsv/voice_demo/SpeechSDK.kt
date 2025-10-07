package com.nsv.voice_demo


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nsv.voice_demo.callback.SimpleRecognitionListener
import java.util.*
import kotlin.math.pow

/**
 * Simple Speech SDK
 * Usage:
 *   SpeechSDK.init(context)
 *   SpeechSDK.start()
 *   SpeechSDK.stop()
 *
 * Observe:
 *   SpeechSDK.partialText.observe(this) { ... }
 *   SpeechSDK.finalText.observe(this) { ... }
 *   SpeechSDK.rmsLevel.observe(this) { ... }
 */
/*object SpeechSDK {

    private var recognizer: SpeechRecognizer? = null
    private lateinit var intentRecognizer: Intent
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private var isInitialized = false

    // LiveData outputs for developer to observe
    private val _partialText = MutableLiveData<String>()
    val partialText: LiveData<String> = _partialText

    private val _finalText = MutableLiveData<String>()
    val finalText: LiveData<String> = _finalText

    private val _rmsLevel = MutableLiveData<Float>()
    val rmsLevel: LiveData<Float> = _rmsLevel

    // transcript builder for merging partials
    private var ongoingTranscript = StringBuilder()

    fun init(context: Context, preferOffline: Boolean = false) {
        if (isInitialized) return
        isInitialized = true

        // 🎯 Intent setup
        intentRecognizer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 250)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 300)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 800)


            if (preferOffline) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        // 🎯 SpeechRecognizer setup
        recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        recognizer?.setRecognitionListener(object : SimpleRecognitionListener() {

            override fun onPartialResults(partialResults: Bundle?) {
                val partialText = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                partialText?.let {
                    ongoingTranscript.append(" $it")
                    _partialText.postValue(ongoingTranscript.toString().trim())
                }
            }

            override fun onResults(results: Bundle?) {
                retryCount = 0
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                text?.let {
                    _finalText.postValue(it)
                    ongoingTranscript.clear()
                }
                restartListening()
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsLevel.postValue(rmsdB)
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        retryCount = 0
                        restartListening()
                    }
                    else -> {
                        retryCount++
                        val delay = minOf(3000L, (500L * 1.5.pow(retryCount)).toLong())
                        handler.postDelayed({
                            recognizer?.cancel()
                            recognizer?.startListening(intentRecognizer)
                        }, delay)
                    }
                }
            }
        })
    }

    *//** Start continuous listening *//*
    fun start() {
        retryCount = 0
        ongoingTranscript.clear()
        recognizer?.cancel()
        recognizer?.startListening(intentRecognizer)
    }

    *//** Stop listening *//*
    fun stop() {
        recognizer?.stopListening()
        handler.removeCallbacksAndMessages(null)
    }

    *//** Destroy recognizer (call in onDestroy of app) *//*
    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        isInitialized = false
    }

    // Auto restart on result/error
    private fun restartListening() {
        handler.post {
            recognizer?.cancel()
            recognizer?.startListening(intentRecognizer)
        }
    }
}*/


object SpeechSDK {

    private var recognizer: SpeechRecognizer? = null
    private lateinit var intentRecognizer: Intent
    private var isInitialized = false

    // LiveData outputs
    private val _partialText = MutableLiveData<String>()
    val partialText: LiveData<String> = _partialText

    private val _finalText = MutableLiveData<String>()
    val finalText: LiveData<String> = _finalText

    private val _rmsLevel = MutableLiveData<Float>()
    val rmsLevel: LiveData<Float> = _rmsLevel

    fun init(context: Context, preferOffline: Boolean = false) {
        if (isInitialized) return
        isInitialized = true
        println("🎤 SpeechSDK: Initializing...")

        // 🎯 Fast intent setup
        intentRecognizer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // ⚡ realtime tuning
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 150)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 200)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 500)

            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                println("🎤 SpeechSDK: Running in OFFLINE mode if supported.")
            }
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        recognizer?.setRecognitionListener(object : SimpleRecognitionListener() {

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                partial?.let {
                    println("📝 Partial: $it")
                    _partialText.postValue(it)
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                text?.let {
                    println("✅ Final: $it")
                    _finalText.postValue(it)
                }
                restartListening() // 🔄 immediately continue
            }

            override fun onRmsChanged(rmsdB: Float) {
//                println("📶 RMS Level: $rmsdB")
                _rmsLevel.postValue(rmsdB)
            }

            override fun onEndOfSpeech() {
                println("⏹️ End of Speech → Restarting...")
                restartListening()
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }

                println("❌ SpeechSDK Error ($error): $message")

                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        println("🔄 Recoverable error → Restarting...")
                        restartListening()
                    }

                    else -> {
                        println("⏹️ Fatal error → Stopping. Need manual restart.")
                        stop()
                    }
                }
            }
        })

        println("🎤 SpeechSDK: Initialized successfully.")
    }

    /** Start continuous realtime recognition */
    fun start() {
        println("▶️ SpeechSDK: START listening...")
        recognizer?.cancel()
        recognizer?.startListening(intentRecognizer)
    }

    /** Stop listening */
    fun stop() {
        println("⏹️ SpeechSDK: STOP listening.")
        recognizer?.stopListening()
    }

    /** Destroy recognizer (call in Application.onTerminate / Activity.onDestroy) */
    fun destroy() {
        println("🗑️ SpeechSDK: Destroying...")
        recognizer?.destroy()
        recognizer = null
        isInitialized = false
    }

    private fun restartListening() {
        if (isInitialized) {
            println("🔄 SpeechSDK: Restarting listening...")
            recognizer?.cancel()
            recognizer?.startListening(intentRecognizer)
        }
    }
}


