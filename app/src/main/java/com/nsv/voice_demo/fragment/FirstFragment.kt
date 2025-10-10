package com.nsv.voice_demo.fragment

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.nsv.voice_demo.ContinuousSpeechManager
import com.nsv.voice_demo.R
import com.nsv.voice_demo.service.VoiceCommandEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class FirstFragment : Fragment() {
    private var minButtonSizePx: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize the minimum size (e.g., 50dp) once the view is available
        // You'll need to define <dimen name="button_min_size">50dp</dimen> in dimens.xml
        minButtonSizePx = resources.getDimensionPixelSize(R.dimen.button_min_size)
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
        val stepSizePx = resources.getDimensionPixelSize(R.dimen.button_size_step)
        view?.let {
            val txtFinal = it.findViewById<TextView>(R.id.txtFinal)
            txtFinal.text = command
        }

        when {
            command.contains("home") || command.contains("go home") -> {
                // Replace with actual fragment you want to open
                val homeFragment = HomeFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
                    .addToBackStack(null)
                    .commit()
            }

            command.contains("settings") || command.contains("go settings") -> {
                val settingsFragment = SettingsFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, settingsFragment)
                    .addToBackStack(null)
                    .commit()
            }

            command.contains("back") || command.contains("go back") -> {
                parentFragmentManager.popBackStack()
            }

            // 👇 This is the updated voice command handler
            command.contains("increase") || command.contains("increase button size") ||  command.contains("plus") -> {
                view?.let { fragmentView ->
                    // 1. Find the target button view
                    val buttonToResize = fragmentView.findViewById<Button>(R.id.btnFirst)

                    // 2. Safely get its current layout parameters
                    val layoutParams = buttonToResize?.layoutParams as? ViewGroup.LayoutParams

                    if (layoutParams != null) {
                        // 3. Increase the width and height
                        layoutParams.width += stepSizePx
                        layoutParams.height += stepSizePx

                        // 4. Apply the new parameters to update the button size
                        buttonToResize.layoutParams = layoutParams

                        // Optional: Log the change for debugging
                        Log.d("VoiceCommand", "Button size increased to: W=${layoutParams.width}, H=${layoutParams.height}")
                    } else {
                        Log.e("VoiceCommand", "Could not find 'myButton' or its LayoutParams.")
                    }
                }
            }

            // 👇 DECREASE COMMAND
            command.contains("decrease") || command.contains("decrease button size") || command.contains("minus")-> {

                view?.let {fragmentView->
                    // 1. Find the target button view
                    val buttonToResize = fragmentView.findViewById<Button>(R.id.btnFirst)

                    // 2. Safely get its current layout parameters
                    val layoutParams = buttonToResize?.layoutParams as? ViewGroup.LayoutParams
                    // Calculate the new size after decrease
                    val newWidth = layoutParams?.width?.minus(stepSizePx)
                    val newHeight = layoutParams?.height?.minus(stepSizePx)

                    // Ensure the new size is not smaller than the predefined minimum
                    layoutParams?.width = newWidth?.coerceAtLeast(minButtonSizePx)
                    layoutParams?.height = newHeight?.coerceAtLeast(minButtonSizePx)

                    // Apply the new parameters
                    buttonToResize.layoutParams = layoutParams
                    Log.d("VoiceCommand", "Button size decreased to: W=${layoutParams?.width}, H=${layoutParams?.height}")
                }

            }

            command.contains("camera") || command.contains("open camera") -> {
                // Replace with actual fragment you want to open
                val cameraFragment = CameraFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, cameraFragment)
                    .addToBackStack(null)
                    .commit()
            }


            // Add more voice command handling here
        }
    }
}
