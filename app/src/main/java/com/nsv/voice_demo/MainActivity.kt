package com.nsv.voice_demo


import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nsv.voice_demo.databinding.ActivityMainBinding
import com.nsv.voice_demo.fragment.FirstFragment
import com.nsv.voice_demo.googlesdk.AssistantSDK
import com.nsv.voice_demo.service.VoiceService
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), ContinuousSpeechManager.SpeechResultListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var speechManager: ContinuousSpeechManager
    private lateinit var assistantSDK: AssistantSDK

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(android.Manifest.permission.RECORD_AUDIO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 1)

// Load FirstFragment if not already loaded
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FirstFragment())
                .commit()
        }

//        SpeechManagerInit()





    }

    private fun startVoiceService() {
        val serviceIntent = Intent(this, VoiceService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }


    private fun SpeechManagerInit() {
        speechManager = ContinuousSpeechManager(this, this)
        speechManager.setup()
    }


    /**
     * Check if all required permissions are granted
     */
    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Handles button clicks safely
     */


    // Handle user’s response to permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {

                Toast.makeText(this, "Permissions granted ✅", Toast.LENGTH_SHORT).show()
                startVoiceService()
            } else {
                Toast.makeText(
                    this,
                    "Permissions required to use app features ❌",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasAllPermissions()) {
//            speechManager.startListening()
        }else{
            Toast.makeText(this,"Please Grant Permission for Use ❌",Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
//        speechManager.stopListening()
    }


    override fun onDestroy() {
        super.onDestroy()
//        speechManager.destroy()
    }
    override fun onSpeechResult(command: String) {
        Log.i("MainActivity", "Recognized: $command")

//        binding.txtPartial.text = command

        when {
            "next" in command -> startActivity(Intent(this, Screen1Activity::class.java))
            "camera" in command -> startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            "Browser" in command -> startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://google.com")
                )
            )

            "close" in command -> exitProcess(0)
        }
    }

    override fun onSpeechError(message: String) {
        Log.i("MainActivity", "Speech Error: $message")
    }

}
