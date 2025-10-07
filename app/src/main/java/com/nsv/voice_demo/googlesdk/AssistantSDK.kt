package com.nsv.voice_demo.googlesdk


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log

class AssistantSDK(private val context: Context) {

    interface AssistantCommandListener {
        fun onCommandReceived(action: String, payload: Map<String, String>)
        fun onError(message: String)
    }

    var listener: AssistantCommandListener? = null

    companion object {
        const val ASSISTANT_REQUEST_CODE = 9999
    }

    /**
     * Start Google Assistant voice command.
     * The calling activity must override onActivityResult and pass results to this SDK.
     */
    fun startAssistantCommand() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            setPackage("com.google.android.googlequicksearchbox")
        }
        try {
            (context as? Activity)?.startActivityForResult(intent, ASSISTANT_REQUEST_CODE)
                ?: listener?.onError("Context is not an Activity")
        } catch (e: Exception) {
            listener?.onError("Assistant not available: ${e.message}")
            Log.e("AssistantSDK", "Failed to start Assistant: ${e.message}")
        }
    }

    /**
     * Call this from your Activity's onActivityResult
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != ASSISTANT_REQUEST_CODE) return

        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                val action = data.getStringExtra("assistant_action") ?: "UNKNOWN"
                val payload = data.getSerializableExtra("assistant_payload") as? Map<String, String>
                    ?: emptyMap()
                listener?.onCommandReceived(action, payload)
            } catch (e: Exception) {
                listener?.onError("Failed to parse command: ${e.message}")
            }
        } else {
            listener?.onError("No command received or user canceled")
        }
    }
}

