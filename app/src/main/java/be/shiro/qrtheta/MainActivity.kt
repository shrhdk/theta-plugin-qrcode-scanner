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
import android.util.Log
import android.view.KeyEvent
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.theta4j.plugin.*
import org.theta4j.plugin.ThetaIntent.KEY_CODE_SHUTTER

class MainActivity : ThetaPluginActivity(), Camera.PreviewCallback {
    companion object {
        private const val TAG = "QR_THETA"
        private const val WIDTH = 1920
        private const val HEIGHT = 960
    }

    private val reader: Reader = MultiFormatReader()

    // Keep reference to avoid GC.
    // Camera#setPreviewTexture does not keep reference, and cause error.
    private var mTexture: SurfaceTexture? = null

    private var mCamera: Camera? = null

    override fun onResume() {
        super.onResume()
        start()
    }

    override fun onPause() {
        super.onPause()
        stop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEY_CODE_SHUTTER && mCamera == null) {
            start()
        }
        return true
    }

    private fun start() {
        setWLanMode(WLanMode.OFF)
        hideLED(LEDTarget.LED3)
        hideLED(LEDTarget.LED4)
        hideLED(LEDTarget.LED5)
        hideLED(LEDTarget.LED6)
        hideLED(LEDTarget.LED7)
        hideLED(LEDTarget.LED8)
        blinkLED(LEDTarget.LED7)
        ring(PresetSound.MOVIE_START)

        startScan()
    }

    private fun stop() {
        hideLED(LEDTarget.LED7)

        stopScan()
    }

    private fun startScan() {
        closeMainCamera()

        mTexture = SurfaceTexture(10)

        mCamera = Camera.open().apply {
            parameters = parameters.apply {
                setPreviewSize(WIDTH, HEIGHT)
                set("RIC_SHOOTING_MODE", "RicMoviePreview1920")
                set("RIC_PROC_STITCHING", "RicStaticStitching")
                previewFrameRate = 5
                previewFormat = ImageFormat.YV12
            }
            setPreviewTexture(mTexture)
            setPreviewCallback(this@MainActivity)
            startPreview()
        }
    }

    private fun stopScan() {
        mCamera?.apply {
            stopPreview()
            setPreviewTexture(null)
            setPreviewCallback(null)
            release()
        }
        mCamera = null

        mTexture?.release()
        mTexture = null

        openMainCamera()
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        val width = WIDTH / 5
        val height = HEIGHT / 3
        val left = width * 2
        val top = height * 1
        val src = PlanarYUVLuminanceSource(data, WIDTH, HEIGHT, left, top, width, height, false)
        val bmp = BinaryBitmap(HybridBinarizer(src))

        val result: Result
        try {
            result = reader.decode(bmp)
            Log.d(TAG, "Found : ${result.text}")
        } catch (e: NotFoundException) {
            //Log.d(TAG, "Not Found")
            return
        }

        val ledColor: LEDColor
        try {
            ledColor = LEDColor.valueOf(result.text)
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Undefined LED Color : ${result.text}")
            return
        }

        runOnUiThread {
            showLED(LEDTarget.LED3, ledColor)
            ring(PresetSound.SHUTTER_CLOSE)
            stop()
        }
    }
}
