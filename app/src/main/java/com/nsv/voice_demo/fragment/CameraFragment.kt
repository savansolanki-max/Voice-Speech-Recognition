package com.nsv.voice_demo.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.FocusMeteringAction.FLAG_AE
import androidx.camera.core.FocusMeteringAction.FLAG_AF
import androidx.camera.core.FocusMeteringAction.FLAG_AWB
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nsv.voice_demo.R
import com.nsv.voice_demo.service.VoiceCommandEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var camera: Camera

    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imagePreview: ImageView

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        view.findViewById<ImageButton>(R.id.captureImageBtn).setOnClickListener {

            captureImage()
        }

        view.findViewById<ImageButton>(R.id.recordVideoBtn).setOnClickListener {
            toggleRecording()
        }

        view.findViewById<ImageButton>(R.id.zoomInBtn).setOnClickListener {
            zoomCamera(zoomIn = true)
        }

        view.findViewById<ImageButton>(R.id.zoomOutBtn).setOnClickListener {
            zoomCamera(zoomIn = false)
        }
        imagePreview = view.findViewById(R.id.imagePreview)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
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

    private var lastCommand = ""
    private var lastCommandTime = 0L
    private fun handleVoiceCommand(command: String) {
        val currentTime = System.currentTimeMillis()

        // Ignore repeated command within 1000ms (1 second)
        if (command == lastCommand && currentTime - lastCommandTime < 1000) {
            Log.d("CameraFragment", "Duplicate command ignored: $command")
            return
        }

        lastCommand = command
        lastCommandTime = currentTime

        when {
            command.contains("zoom in") -> {
                zoomCamera(true)
            }

            command.contains("zoom out") -> {
                zoomCamera(zoomIn = false)
            }

            command.contains("capture") -> {
                Log.i("COUNT","captureImage")
                captureImage()

            }

           /* command.contains("video",ignoreCase = true) || command.contains("video record",ignoreCase = true) || command.contains("start video",ignoreCase = true) -> {
                toggleRecording()
            }

            command.contains("pause video", ignoreCase = true) -> pauseRecording()

            command.contains("resume video", ignoreCase = true) -> resumeRecording()

            command.contains("stop video", ignoreCase = true) -> stopRecording()*/
            command.contains("close camera", ignoreCase = true) || command.contains("close", ignoreCase = true) || command.contains("back") -> {
                stopCamera()
                closeCameraScreen()
            }
            command.contains("front",ignoreCase = true) || command.contains("front camera",ignoreCase = true)->{
              /*  lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }*/
                lensFacing =  CameraSelector.LENS_FACING_FRONT
                startCamera() // Restart camera with new lensFacing
            }

            command.contains("back camera",ignoreCase = true) || command.contains("back lens",ignoreCase = true) || command.contains("default",ignoreCase = true)->{
                /*  lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                      CameraSelector.LENS_FACING_FRONT
                  } else {
                      CameraSelector.LENS_FACING_BACK
                  }*/
                lensFacing =  CameraSelector.LENS_FACING_BACK
                startCamera() // Restart camera with new lensFacing
            }

            command.contains("focus",ignoreCase = true)->{
                focusCenter(camera.cameraControl, previewView)
            }
        }

    }
    fun focusCenter(cameraControl: CameraControl, previewView: PreviewView) {
        // Get the center point of the PreviewView
        val factory: MeteringPointFactory = previewView.meteringPointFactory
        val centerPoint: MeteringPoint = factory.createPoint(0.5f, 0.5f)

        // Build focus metering action with autofocus (AF), auto-exposure (AE), and auto-white-balance (AWB)
        val action = FocusMeteringAction.Builder(
            centerPoint,
            FLAG_AF or FLAG_AE or FLAG_AWB
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS) // Optional: cancel after 3 seconds
            .build()

        // Trigger the focus
        cameraControl.startFocusAndMetering(action)
    }

     var lensFacing:Int = CameraSelector.LENS_FACING_BACK
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
                val cameraControl = camera.cameraControl
            } catch (e: Exception) {
                Log.e("CameraFragment", "Failed to bind camera use cases", e)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImage() {


        val photoFile = File(
            requireContext().externalMediaDirs.first(),
            "IMG_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    // Load thumbnail into preview ImageView

                    requireActivity().runOnUiThread {
                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        imagePreview.setImageBitmap(bitmap)
                        imagePreview.visibility = View.VISIBLE

                        Toast.makeText(
                            requireContext(),
                            "Image saved: ${photoFile.absolutePath}",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraFragment", "Image capture failed", exception)
                    Toast.makeText(
                        requireContext(),
                        "Image capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            Log.d("CameraFragment", "Camera stopped.")
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun closeCameraScreen() {
        parentFragmentManager.popBackStack()
    }

    private fun pauseRecording() {
        recording?.pause()
        Toast.makeText(requireContext(), "Recording paused", Toast.LENGTH_SHORT).show()
    }

    private fun resumeRecording() {
        recording?.resume()
        Toast.makeText(requireContext(), "Recording resumed", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        view?.findViewById<ImageButton>(R.id.recordVideoBtn)
            ?.setImageResource(android.R.drawable.ic_btn_speak_now)
        Toast.makeText(requireContext(), "Recording stopped", Toast.LENGTH_SHORT).show()
    }

    private fun toggleRecording() {
        val recordButton = view?.findViewById<ImageButton>(R.id.recordVideoBtn)

        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            recordButton?.setImageResource(android.R.drawable.ic_btn_speak_now)
            return
        }

        val videoFile = File(
            requireContext().externalMediaDirs.first(),
            "VID_${System.currentTimeMillis()}.mp4"
        )

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .apply {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.RECORD_AUDIO
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        recordButton?.setImageResource(android.R.drawable.ic_media_pause)
                        Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT)
                            .show()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (recordEvent.hasError()) {
                            Toast.makeText(
                                requireContext(),
                                "Recording error: ${recordEvent.error}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Video saved: ${videoFile.absolutePath}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        recordButton?.setImageResource(android.R.drawable.ic_btn_speak_now)
                        recording = null
                    }
                }
            }
    }

    private fun zoomCamera(zoomIn: Boolean) {
        val zoomState = camera.cameraInfo.zoomState.value
        val currentZoomRatio = zoomState?.zoomRatio ?: 1f
        val maxZoomRatio = zoomState?.maxZoomRatio ?: 1f
        val minZoomRatio = zoomState?.minZoomRatio ?: 1f

        val newZoomRatio = if (zoomIn) {
            (currentZoomRatio + 0.1f).coerceAtMost(maxZoomRatio)
        } else {
            (currentZoomRatio - 0.1f).coerceAtLeast(minZoomRatio)
        }

        camera.cameraControl.setZoomRatio(newZoomRatio)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
