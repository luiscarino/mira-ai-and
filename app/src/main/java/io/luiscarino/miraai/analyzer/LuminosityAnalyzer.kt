package io.luiscarino.miraai.analyzer

import androidx.camera.core.ImageProxy
import java.util.*
import java.util.concurrent.TimeUnit

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

/**
 * Our custom image analysis class.
 *
 * <p>All we need to do is override the function `analyze` with our desired operations. Here,
 * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
 */
private class LuminosityAnalyzer(listener: LumaListener? = null) : BaseAnalyzer() {

    private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }

    /**
     * Used to add listeners that will be called with each luma computed
     */
    fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

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
            //  plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            listeners.forEach { it(luma) }
        }
    }
}
