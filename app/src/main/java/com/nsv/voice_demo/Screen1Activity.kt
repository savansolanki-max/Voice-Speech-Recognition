package com.nsv.voice_demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nsv.voice_demo.databinding.ActivityScreen1Binding

class Screen1Activity : AppCompatActivity(), ContinuousSpeechManager.SpeechResultListener  {
    private lateinit var binding: ActivityScreen1Binding
    private lateinit var speechManager: ContinuousSpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreen1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        SpeechManagerInit()


    }
    private fun SpeechManagerInit() {
        speechManager = ContinuousSpeechManager(this, this)
        speechManager.setup()
    }
    override fun onResume() {
        super.onResume()
        speechManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        speechManager.stopListening()
    }

    override fun onSpeechResult(command: String) {
        binding.txtWrite.text = command
        when(command.lowercase()) {
            "red"  -> binding.screen1Root.setBackgroundColor(Color.RED)
            "blue" -> binding.screen1Root.setBackgroundColor(Color.BLUE)
            "green"  -> binding.screen1Root.setBackgroundColor(Color.GREEN)
            "white"  -> binding.screen1Root.setBackgroundColor(Color.WHITE)
            "camera"  -> startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            "browser" -> startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://google.com")
                )
            )

            "back","exit","close" -> onBackPressed()
        }
    }

    override fun onSpeechError(message: String) {
        Log.i("MainActivity", "Speech Error: $message")
    }


    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
    }
}
