package com.pedro.usbtest

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.SurfaceHolder
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.pedro.rtplibrary.view.LightOpenGlView
import com.pedro.rtplibrary.view.OpenGlView
import com.pedro.usbtest.streamlib.RtmpUSB
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var usbMonitor: USBMonitor
    private var uvcCamera: UVCCamera? = null
    private val width = 1920
    private val height = 1080
    private lateinit var rtmpUSB: RtmpUSB

    private val URL = "rtmp://192.168.0.30/publish/live"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<EditText>(R.id.et_url).setText(URL)
        findViewById<OpenGlView>(R.id.openglview).holder.addCallback(this)
        findViewById<Button>(R.id.start_stop).setOnClickListener {
            if (isMyServiceRunning(RtpService::class.java)) {
                stopStream()
            } else {
                startStream()
            }
        }
    }

    private fun startStream() {
        findViewById<Button>(R.id.start_stop).setText(R.string.stop_button)
        startService(Intent(applicationContext, RtpService::class.java).apply {
            putExtra("endpoint", findViewById<EditText>(R.id.et_url).text.toString())
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        })
    }

    private fun stopStream() {
        stopService(Intent(applicationContext, RtpService::class.java))
        findViewById<Button>(R.id.start_stop).setText(R.string.start_button)
    }

    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        if (isMyServiceRunning(RtpService::class.java)) {
            findViewById<Button>(R.id.start_stop).setText(R.string.stop_button)
        } else {
            findViewById<Button>(R.id.start_stop).setText(R.string.start_button)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        if (mBound) {
            mService.setView(findViewById<OpenGlView>(R.id.openglview))
            mService.startPreview()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (mBound) {
            mService.setView(applicationContext)
            mService.stopPreview()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (mBound) {
            mService.setView(applicationContext)
            mService.stopPreview()
        }
    }

    private lateinit var mService: RtpService
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as RtpService.LocalBinder
            mService = binder.getService()

//            mService.setView(applicationContext)
//            mService.startPreview()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onStop() {
        super.onStop()
        if (mBound) {
            unbindService(connection)
            mBound = false
        }
    }
}
