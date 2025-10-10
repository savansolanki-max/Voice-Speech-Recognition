package com.nsv.voice_demo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat


/**
 * Manages continuous speech recognition by automatically restarting the listener.
 * <p>
 * This class wraps Android's {@link SpeechRecognizer} to provide a seamless, continuous
 * listening experience. It handles the lifecycle of the recognizer, including setup,

 * starting, stopping, and restarting on results or errors. Callers must implement the
 * {@link SpeechResultListener} to receive callbacks for recognized text and errors.
 *
 * @param context  The application context, used to access the {@link SpeechRecognizer}.
 * @param listener The callback listener for speech results and errors.
 */
class ContinuousSpeechManager(
    private val context: Context,
    private val listener: SpeechResultListener
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())

    private val recognizerIntent: Intent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
//            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            // Silence detection parameters for more robust continuous listening.
            /*  putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 1000L)
              putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 700L)
              putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 2000L)*/

            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 100)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 100)
            putExtra(
                "android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS",
                100
            )
        }
    }

    private val TAG = "ContinuousSpeechManager"

    /**
     * A callback interface to deliver speech recognition results or errors.
     */
    interface SpeechResultListener {
        /**
         * Called when a partial or final speech recognition result is available.
         *
         * @param text The recognized text.
         */
        fun onSpeechResult(text: String)

        /**
         * Called when an error occurs during the recognition process.
         *
         * @param message A human-readable error message.
         */
        fun onSpeechError(message: String)
    }

    /**
     * Initializes the SpeechRecognizer.
     * <p>
     * This must be called before {@link #startListening()}. It cleans up any previous instances
     * and creates a new {@link SpeechRecognizer}. If recognition is not available on the device,
     * it reports an error via the {@link SpeechResultListener}.
     */
    fun setup() {
        destroy() // Always clean before new setup

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onSpeechError("Speech recognition not available on this device.")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@ContinuousSpeechManager)
        }

        Log.d(TAG, "SpeechRecognizer set up.")
    }

    /**
     * Starts the speech recognition process.
     * <p>
     * If not already listening, this method will check for microphone permissions and begin
     * listening for speech. If permissions are not granted, it reports an error.
     */
    fun startListening() {
        if (speechRecognizer == null) setup()
        if (isListening) return

        if (!hasMicPermission()) {
            listener.onSpeechError("Microphone permission not granted")
            return
        }

        isListening = true
        // Operations must be on the main thread
        handler.post {
            try {
                speechRecognizer?.startListening(recognizerIntent)
                Log.d(TAG, "Listening started")
            } catch (e: Exception) {
                isListening = false
                Log.e(TAG, "startListening failed: ${e.message}")
            }
        }
    }

    /**
     * Manually stops the speech recognition process.
     * <p>
     * This cancels the current listening session but does not release resources.
     * Use {@link #destroy()} for complete cleanup.
     */
    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "stopListening failed: ${e.message}")
        }
        isListening = false
        Log.d(TAG, "Listening stopped")
    }

    /**
     * Releases all resources associated with the SpeechRecognizer.
     * <p>
     * This method should be called when the manager is no longer needed (e.g., in an Activity's
     * onDestroy) to prevent memory leaks and ensure the recognizer is properly shut down.
     */
    fun destroy() {
        try {
            speechRecognizer?.cancel()   // Immediate stop
            speechRecognizer?.destroy()  // Release resources
        } catch (e: Exception) {
            Log.e(TAG, "destroy() error: ${e.message}")
        }
        speechRecognizer = null
        isListening = false
    }

    // --- RecognitionListener Callbacks ---

    /**
     * Called when the endpointer is ready for the user to start speaking.
     * @param params Contains parameters for the recognition engine.
     */
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    /**
     * The user has started to speak.
     */
    override fun onBeginningOfSpeech() {
        Log.d(TAG, "User started speaking")
    }

    /**
     * The sound level in the audio stream has changed.
     * @param rmsdB The new RMS dB value.
     */
    override fun onRmsChanged(rmsdB: Float) {
        // Optional: Can be used for UI feedback (e.g., mic level visualization).
    }

    /**
     * More sound has been received.
     * @param buffer A buffer containing a sequence of audio samples.
     */
    override fun onBufferReceived(buffer: ByteArray?) {}

    /**
     * Called after the user stops speaking.
     */
    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
    }

    /**
     * Called when recognition results are ready.
     * @param results The recognition results. The most likely result is passed to the listener.
     */
    override fun onResults(results: Bundle) {
        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
            listener.onSpeechResult(it)
        }
        restartListening()
    }

    /**
     * Called when partial recognition results are available.
     * @param partialResults The partial recognition results.
     */
    override fun onPartialResults(partialResults: Bundle) {
        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            ?.let {
//            listener.onSpeechResult(it)
            }
    }

    /**
     * A network or recognition error occurred.
     * @param errorCode The integer error code (see {@link SpeechRecognizer} ERROR_* constants).
     */
    override fun onError(errorCode: Int) {
        val errorMessage = getErrorText(errorCode)
        listener.onSpeechError("Error: $errorMessage (Code: $errorCode)")
        Log.e(TAG, "SpeechRecognizer Error: $errorMessage (Code: $errorCode)")

        when (errorCode) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_CLIENT -> {
                // A critical error occurred; reset the recognizer completely.
                isListening = false
                destroy()
                handler.postDelayed({
                    setup()
                    startListening()
                }, 2000)
            }

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                // Permissions error is not recoverable by restarting.
                isListening = false
            }

            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // These are common and recoverable; just restart listening.
                restartListening()
            }

            else -> {
                // Handle all other recoverable errors by restarting.
                restartListening()
            }
        }
    }

    /**
     * Reserved for future events.
     */
    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- Helper Methods ---

    /**
     * Restarts the listening session after a short delay.
     * This is the core of the "continuous" listening logic.
     */
    private fun restartListening() {
        stopListening()
        handler.postDelayed({
            if (hasMicPermission()) {
                startListening()
            }
        }, 1200) // Delay to prevent rapid, erroneous restarts.
    }

    /**
     * Checks if the app has been granted the RECORD_AUDIO permission.
     * @return True if permission is granted, false otherwise.
     */
    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Converts a {@link SpeechRecognizer} error code into a human-readable string.
     * @param errorCode The error code from {@link #onError(int)}.
     * @return A descriptive string for the error.
     */
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