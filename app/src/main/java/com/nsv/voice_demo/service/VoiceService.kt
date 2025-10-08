package com.nsv.voice_demo.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.nsv.voice_demo.ContinuousSpeechManager
import org.greenrobot.eventbus.EventBus
/**
 * A persistent background [Service] for continuous speech recognition.
 *
 * This service runs in the foreground to maintain a continuous listening state
 * using a [ContinuousSpeechManager] and handles the recognized speech.
 *
 * It implements [ContinuousSpeechManager.SpeechResultListener] to receive
 * speech recognition results and errors.
 */
class VoiceService : Service(), ContinuousSpeechManager.SpeechResultListener {

    /**
     * The manager responsible for handling continuous speech recognition.
     */
    private lateinit var speechManager: ContinuousSpeechManager

    /**
     * Called when the service is first created.
     *
     * Initializes the [speechManager], sets up the speech recognition components,
     * and promotes the service to the foreground with a persistent notification.
     */
    override fun onCreate() {
        super.onCreate()

        speechManager = ContinuousSpeechManager(this, this)
        speechManager.setup()

        startForeground(
            1001,
            NotificationHelper.createVoiceServiceNotification(this)
        )
    }

    /**
     * Called when the service is started via [Context.startService].
     *
     * Initiates the continuous speech recognition process by calling
     * [ContinuousSpeechManager.startListening].
     *
     * @param intent The [Intent] supplied to [Context.startService], may be null.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what to do if the service's process is killed;
     * [START_STICKY] is returned to request the system to recreate the service.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        speechManager.startListening()
        return START_STICKY
    }

    /**
     * Called when the service is no longer used and is being destroyed.
     *
     * Cleans up and releases resources held by the [speechManager].
     */
    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
    }

    /**
     * Binds the service to an activity. This service is not designed to be bound,
     * so it returns null.
     *
     * @param intent The [Intent] used to bind to this service.
     * @return An [IBinder] through which clients can communicate with the service, or null if binding is not supported.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    // 🎙 Speech Callbacks

    /**
     * Callback method invoked when the speech recognition engine successfully recognizes a phrase.
     *
     * Logs the recognized text and delegates the command handling to [handleVoiceCommand].
     *
     * @param text The fully recognized speech result as a [String].
     */
    override fun onSpeechResult(text: String) {
        Log.d("VoiceService", "Heard: $text")
        handleVoiceCommand(text)
    }

    /**
     * Callback method invoked when a speech recognition error occurs.
     *
     * Logs the error message.
     *
     * @param message A descriptive [String] detailing the error.
     */
    override fun onSpeechError(message: String) {
        Log.e("VoiceService", "Speech error: $message")
    }

    /**
     * Processes the recognized voice command.
     *
     * Posts a [VoiceCommandEvent] containing the recognized text to the default [EventBus]
     * for other application components to act upon.
     *
     * @param text The recognized voice command [String].
     */
    private fun handleVoiceCommand(text: String) {
        EventBus.getDefault().post(VoiceCommandEvent(text))
    }
}