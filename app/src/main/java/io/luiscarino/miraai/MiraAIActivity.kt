package io.luiscarino.miraai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.luiscarino.miraai.ext.FLAGS_FULLSCREEN
import kotlinx.android.synthetic.main.activity_ai_camera.*


private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_camera)
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        fragment_container.postDelayed({
            fragment_container.systemUiVisibility = FLAGS_FULLSCREEN

        }, IMMERSIVE_FLAG_TIMEOUT)
    }

}