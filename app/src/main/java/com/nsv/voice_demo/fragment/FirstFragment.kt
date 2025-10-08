package com.nsv.voice_demo.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.nsv.voice_demo.ContinuousSpeechManager
import com.nsv.voice_demo.R
import com.nsv.voice_demo.service.VoiceCommandEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class FirstFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onVoiceCommandReceived(event: VoiceCommandEvent) {
        handleVoiceCommand(event.command)
    }


    private fun handleVoiceCommand(command: String) {
        when {
            command.contains("home") -> {
                // Replace with actual fragment you want to open
                val homeFragment = HomeFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
                    .addToBackStack(null)
                    .commit()
            }

            command.contains("settings") -> {
                val settingsFragment = SettingsFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, settingsFragment)
                    .addToBackStack(null)
                    .commit()
            }

            command.contains("back") || command.contains("go back") -> {
                parentFragmentManager.popBackStack()
            }

            // Add more voice command handling here
        }
    }
}
