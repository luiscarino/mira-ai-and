package io.luiscarino.miraai.camera

import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import io.luiscarino.miraai.R
import io.luiscarino.miraai.analyzer.FirebaseTextAnalyzer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlinx.android.synthetic.main.fragment_camera.view_finder as viewFinder

/**
 * Fragment that handles all camera operations.
 */
class CameraFragment : Fragment() {

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var label: TextView
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        label = view.findViewById(R.id.textView)
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId
            bindCameraUseCases()
        }
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }

    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        // Set up the view finder use case to display camera preview
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        // Build the viewfinder use case
        preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview?.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Setup image analysis pipeline that computes average pixel luminance in real time
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lensFacing)
            // In our analysis, we care more about the latest image than analyzing *every* image
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageAnalyzer = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(UiThreadExecutor(), FirebaseTextAnalyzer { firebaseLabel ->
                // Values returned from our analyzer are passed to the attached listener
                label.text = firebaseLabel
                textToSpeech = TextToSpeech(context) {
                    when (it) {
                        TextToSpeech.SUCCESS -> {
                            textToSpeech.speak(firebaseLabel, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                        TextToSpeech.ERROR -> {
                            Toast.makeText(
                                context,
                                "Text to speech is unavailable.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            })
        }

        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(this, preview, imageAnalyzer)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // apply transformations to TextureView
        viewFinder.setTransform(matrix)
    }

    internal class UiThreadExecutor : Executor {
        private val mHandler: Handler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            mHandler.post(command)
        }
    }

    companion object {
        private const val TAG = "MiraAICameraX"

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }

}