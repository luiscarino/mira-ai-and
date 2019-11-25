package io.luiscarino.miraai.analyzer

import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.util.*
import java.util.concurrent.TimeUnit


/** Helper type alias used for analysis use case callbacks */
typealias FirebaseLabelerListener = (label: String) -> Unit


/**
 *  Custom image analysis class powered by [FirebaseVision] that can be used to label images.
 *
 */
class FirebaseLabelerAnalyzer(listener: FirebaseLabelerListener? = null) : BaseAnalyzer() {
    private val listeners = ArrayList<FirebaseLabelerListener>().apply { listener?.let { add(it) } }

    /**
     * Used to add listeners that will be called with each luma computed
     */
    fun onFrameAnalyzed(listener: FirebaseLabelerListener) = listeners.add(listener)

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

            val labelImage = FirebaseVisionImage.fromByteArray(YUVdata, metadata)

            val labeler = FirebaseVision.getInstance().onDeviceImageLabeler

            // Call all listeners with new value
            labeler.processImage(labelImage).addOnSuccessListener { labels ->
                listeners.forEach {
                    if (labels.size >= 1) {
                        it(labels[0].text + " " + labels[0].confidence)
                    } else {
                        it("Unable to label")
                    }
                }
            }.addOnFailureListener { e ->
                listeners.forEach {
                    it(e.message.toString())
                }
            }
        }
    }

}