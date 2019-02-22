/*
 * Copyright (C) 2019 Hideki Shiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.shiro.qrtheta

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.theta4j.plugin.PresetSound
import org.theta4j.plugin.ThetaPluginActivity

class MainActivity : ThetaPluginActivity(), Camera.PreviewCallback {
    companion object {
        private const val TAG = "QR_THETA"
        private const val WIDTH = 1920
        private const val HEIGHT = 960
    }

    private val reader: Reader = MultiFormatReader()

    // Keep reference to avoid GC.
    // Camera#setPreviewTexture does not keep reference, and cause error.
    private var texture = SurfaceTexture(10)

    private var mCamera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        start()
    }

    override fun onPause() {
        super.onPause()

        stop()
    }

    private fun start() {
        closeMainCamera()
        mCamera = Camera.open().apply {
            parameters = parameters.apply {
                setPreviewSize(WIDTH, HEIGHT)
                set("RIC_SHOOTING_MODE", "RicMoviePreview1920")
                set("RIC_PROC_STITCHING", "RicStaticStitching")
                previewFrameRate = 5
                previewFormat = ImageFormat.YV12
            }
            setPreviewCallback(this@MainActivity)
            setPreviewTexture(texture)
            startPreview()
        }
    }

    private fun stop() {
        mCamera?.apply {
            stopPreview()
            setPreviewCallback(null)
            release()
        }
        mCamera = null
        openMainCamera()
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        val width = WIDTH / 5
        val height = HEIGHT / 3
        val left = width * 2
        val top = height * 1
        val src = PlanarYUVLuminanceSource(data, WIDTH, HEIGHT, left, top, width, height, false)
        val bmp = BinaryBitmap(HybridBinarizer(src))

        try {
            val result = reader.decode(bmp)
            Log.d(TAG, "Found : ${result.text}")
            ring(PresetSound.SHUTTER_CLOSE)
            stop()
        } catch (e: NotFoundException) {
            Log.d(TAG, "Not Found")
        }
    }
}
