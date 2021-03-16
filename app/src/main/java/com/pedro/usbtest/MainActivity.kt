package com.pedro.usbtest

import android.app.Activity
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.pedro.rtplibrary.view.LightOpenGlView
import com.pedro.usbtest.streamlib.RtmpUSB
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import net.ossrs.rtmp.ConnectCheckerRtmp


class MainActivity : Activity(), SurfaceHolder.Callback {

    private lateinit var usbMonitor: USBMonitor
    private var uvcCamera: UVCCamera? = null
    private val width = 1920
    private val height = 1080
    private lateinit var rtmpUSB: RtmpUSB

    private val URL = "rtmp://192.168.0.30/publish/live"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rtmpUSB = RtmpUSB(findViewById<LightOpenGlView>(R.id.openglview), connectCheckerRtmp)
        usbMonitor = USBMonitor(this, onDeviceConnectListener)
        usbMonitor.register()
        findViewById<EditText>(R.id.et_url).setText(URL)
        findViewById<Button>(R.id.start_stop).setOnClickListener {
            if (uvcCamera != null) {
                if (!rtmpUSB.isStreaming) {
                    startStream()
                } else {
                    stopStream()
                }
            }
        }
    }

    private fun restartStream() {
        if (!rtmpUSB.isStreaming) {
            startStream()
        } else {
            stopStream()
            startStream()
        }
    }

    private fun startStream() {
        val startStop = findViewById<Button>(R.id.start_stop)
        startStream(findViewById<EditText>(R.id.et_url).text.toString())
        runOnUiThread {
            startStop.text = "Stop stream"
        }
    }

    private fun stopStream() {
        val startStop = findViewById<Button>(R.id.start_stop)
        rtmpUSB.stopStream(uvcCamera)
        runOnUiThread {
            startStop.text = "Start stream"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (rtmpUSB.isStreaming && uvcCamera != null) rtmpUSB.stopStream(uvcCamera)
        if (rtmpUSB.isOnPreview && uvcCamera != null) rtmpUSB.stopPreview(uvcCamera)
        uvcCamera?.close()
        usbMonitor.unregister()
    }

    private fun startStream(url: String) {
        if (rtmpUSB.prepareVideo(width, height, 60, 4000 * 1024, false, 0, uvcCamera) && rtmpUSB.prepareAudio()) {
            rtmpUSB.startStream(uvcCamera, url)
        }
    }

    private fun disconnect() {
        uvcCamera?.let {
            rtmpUSB.stopPreview(it)
            stopStream()
            it.close()
            uvcCamera = null
        }
    }

    private val connectCheckerRtmp = object : ConnectCheckerRtmp {

        override fun onAuthSuccessRtmp() {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onAuthSuccessRtmp", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onConnectionSuccessRtmp() {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onConnectionFailedRtmp(reason: String?) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Failed $reason", Toast.LENGTH_SHORT).show()
                rtmpUSB.stopStream(uvcCamera)
            }
        }

        override fun onAuthErrorRtmp() {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onAuthErrorRtmp", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onDisconnectRtmp() {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Disconnect", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val onDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {
            usbMonitor.requestPermission(device)
        }

        override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
            val camera = UVCCamera()
            camera.open(ctrlBlock)
            try {
                camera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_MJPEG)
            } catch (e: IllegalArgumentException) {
                camera.destroy()
                try {
                    camera.setPreviewSize(width, height, UVCCamera.DEFAULT_PREVIEW_MODE)
                } catch (e1: IllegalArgumentException) {
                    return
                }
            }
            uvcCamera = camera
            rtmpUSB.startPreview(uvcCamera, width, height)
            restartStream()
        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            disconnect()
        }

        override fun onCancel(device: UsbDevice?) {
        }

        override fun onDettach(device: UsbDevice?) {
            disconnect()
        }
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
    }
}
