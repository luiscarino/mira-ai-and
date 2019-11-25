package io.luiscarino.miraai.analyzer

import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.util.*
import java.util.concurrent.TimeUnit


/** Helper type alias used for analysis use case callbacks */
typealias FirebaseTextAnalyzerListener = (text: String) -> Unit


/**
 *  Custom image analysis class powered by [FirebaseVision] that can be used to label images.
 *
 */
class FirebaseTextAnalyzer(listener: FirebaseTextAnalyzerListener? = null) : BaseAnalyzer() {

    private val listeners = ArrayList<FirebaseTextAnalyzerListener>().apply { listener?.let { add(it) } }

    /**
     * Used to add listeners that will be called with each luma computed
     */
    fun onFrameAnalyzed(listener: FirebaseTextAnalyzerListener) = listeners.add(listener)


    /**
     * Analyzes an image to produce a result.
     *
     * <p>The caller is responsible for ensuring this analysis method can be executed quickly
     * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
     * images will not be acquired and analyzed.
     *
     * <p>The image passed to this method becomes invalid after this method returns. The caller
     * should not store external references to this image, as these references will become
     * invalid.
     *
     * @param image image being analyzed VERY IMPORTANT: do not close the image, it will be
     * automatically closed after this method returns
     * @return the image analysis result
     */
    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        // If there are no listeners attached, we don't need to perform analysis
        if (listeners.isEmpty()) return

        // Keep track of frames analyzed
        frameTimestamps.push(System.currentTimeMillis())

        // Compute the FPS using a moving average
        while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
        framesPerSecond = 1.0 / ((frameTimestamps.peekFirst() -
                frameTimestamps.peekLast()) / frameTimestamps.size.toDouble()) * 1000.0

        // Calculate the average luma no more often than every second
        if (frameTimestamps.first - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(1)) {
            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance
            val y = image.planes[0]
            val u = image.planes[1]
            val v = image.planes[2]

            // get number of pixels on each plane
            val Yb = y.buffer.remaining() // light
            val Ub = u.buffer.remaining()
            val Vb = v.buffer.remaining()

            // convert pixels into YUV formatted array
            val YUVdata = ByteArray(Yb + Ub + Vb)

            y.buffer.get(YUVdata, 0, Yb)
            u.buffer.get(YUVdata, Yb, Ub)
            v.buffer.get(YUVdata, Yb + Ub, Vb)

            // use firebase to analyse data
            val metadata = FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
                .setHeight(image.height)
                .setWidth(image.width)
                .setRotation(degreesToFirebaseRotation(rotationDegrees))
                .build()

            val firebaseVisionImage = FirebaseVisionImage.fromByteArray(YUVdata, metadata)

            val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
            textRecognizer.processImage(firebaseVisionImage).addOnSuccessListener { firebaseVisionText ->
                listeners.forEach {
                    it(processTextRecognitionResult(firebaseVisionText))
                }
            }.addOnFailureListener { e ->
                listeners.forEach {
                    it(e.cause.toString())
                }
            }
        }
    }

    private fun processTextRecognitionResult(texts: FirebaseVisionText) : String {
        val blocks = texts.textBlocks
        if (blocks.size == 0) {
            return "No text found"

        }

        var text = ""
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    text += elements[k].text
                }
            }
        }

        return text
    }



}