/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */

package vendor.oplus.hardware.popupcamera.controller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.ServiceManager
import android.os.SystemClock
import android.util.Log

import vendor.oplus.hardware.popupcamera.IPopupCamera
import vendor.oplus.hardware.popupcamera.MotorPosition

class PopupCameraService : Service() {

    private var popupCamera: IPopupCamera? = null
    private val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            MSG_OPEN -> {
                Log.d(TAG, "Raising camera")
                try { popupCamera?.raise() }
                catch (e: Exception) { Log.e(TAG, "Failed to raise camera", e) }
            }
            MSG_CLOSE -> {
                Log.d(TAG, "Lowering camera")
                try { popupCamera?.lower() }
                catch (e: Exception) { Log.e(TAG, "Failed to lower camera", e) }
            }
        }
        true
    }
    private var fallSensor: Sensor? = null
    private lateinit var sensorManager: SensorManager

    private var openEvent = 0L
    private var closedEvent = 0L

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            Log.d(TAG, "Camera $cameraId available")
            if (cameraId != FRONT_CAMERA_ID) return
            closedEvent = SystemClock.elapsedRealtime()
            if (SystemClock.elapsedRealtime() - openEvent < EVENT_DELAY_MS
                && handler.hasMessages(MSG_OPEN)) {
                handler.removeMessages(MSG_OPEN)
            }
            handler.sendEmptyMessageDelayed(MSG_CLOSE, EVENT_DELAY_MS)
        }

        override fun onCameraUnavailable(cameraId: String) {
            Log.d(TAG, "Camera $cameraId unavailable")
            if (cameraId != FRONT_CAMERA_ID) return
            openEvent = SystemClock.elapsedRealtime()
            if (SystemClock.elapsedRealtime() - closedEvent < EVENT_DELAY_MS
                && handler.hasMessages(MSG_CLOSE)) {
                handler.removeMessages(MSG_CLOSE)
            }
            handler.sendEmptyMessageDelayed(MSG_OPEN, EVENT_DELAY_MS)
        }
    }

    private val fallListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values[0] <= 0) return

            Log.d(TAG, "Fall detected, retracting camera")

            try {
                if (popupCamera?.position == MotorPosition.DOWN) return
                popupCamera?.lower()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retract camera on fall", e)
                return
            }

            handler.post {
                android.widget.Toast.makeText(
                    this@PopupCameraService,
                    "Free fall detected. Camera retracted for safety.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

            // Re-raise after a short delay if the camera is still in use
            handler.sendEmptyMessageDelayed(MSG_OPEN, FALL_RERAISE_DELAY_MS)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    override fun onCreate() {
        super.onCreate()

        val binder = ServiceManager.waitForService("$DESCRIPTOR/default")
        if (binder == null) {
            Log.e(TAG, "Popup camera HAL not found")
            stopSelf()
            return
        }
        popupCamera = IPopupCamera.Stub.asInterface(binder)

        val cameraThread = HandlerThread("popupcamera-cam").apply { start() }
        val cameraHandler = Handler(cameraThread.looper)

        val cameraManager = getSystemService(CameraManager::class.java)
        cameraManager.registerAvailabilityCallback(cameraCallback, cameraHandler)

        sensorManager = getSystemService(SensorManager::class.java)
        fallSensor = sensorManager.getSensorList(Sensor.TYPE_ALL)
            .firstOrNull { it.stringType == FALL_SENSOR_TYPE }

        if (fallSensor != null) {
            sensorManager.registerListener(
                fallListener, fallSensor, SensorManager.SENSOR_DELAY_FASTEST
            )
            Log.d(TAG, "Fall sensor registered")
        } else {
            Log.w(TAG, "Fall sensor not found")
        }

        Log.d(TAG, "Service started")
    }

    override fun onDestroy() {
        fallSensor?.let { sensorManager.unregisterListener(fallListener, it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val TAG = "PopupCameraService"
        private const val DESCRIPTOR = "vendor.oplus.hardware.popupcamera.IPopupCamera"
        private const val FRONT_CAMERA_ID = "1"
        private const val FALL_SENSOR_TYPE = "camera_protect"
        private const val EVENT_DELAY_MS = 100L
        private const val FALL_RERAISE_DELAY_MS = 1500L
        private const val MSG_OPEN = 1001
        private const val MSG_CLOSE = 1000

        fun start(context: Context) {
            context.startService(Intent(context, PopupCameraService::class.java))
        }
    }
}
