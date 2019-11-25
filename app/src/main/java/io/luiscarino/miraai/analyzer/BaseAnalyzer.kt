package io.luiscarino.miraai.analyzer

import androidx.camera.core.ImageAnalysis
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.nio.ByteBuffer
import java.util.*


/** Helper type alias used for analysis use case callbacks */
typealias BaseAnalyzerListener = (text: String) -> Unit

/**
 * Common code for image analysis
 */
abstract class BaseAnalyzer : ImageAnalysis.Analyzer {

    protected val frameRateWindow = 8
    protected val frameTimestamps = ArrayDeque<Long>(5)

    protected var lastAnalyzedTimestamp = 0L
    protected var framesPerSecond: Double = -1.0

    /**
     * Helper extension function used to extract a byte array from an image plane buffer
     */
    protected fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    protected fun degreesToFirebaseRotation(degrees: Int): Int {
        return when (degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> throw IllegalArgumentException("invalid argument $degrees")
        }
    }
}