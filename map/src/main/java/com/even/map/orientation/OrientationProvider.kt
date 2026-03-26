package com.even.map.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.lang.Math.toDegrees
import kotlin.math.absoluteValue

private const val SMOOTHING_ALPHA = 0.02f // Controls the smoothing level
private const val DIFF_THRESHOLD = 2.5f // Balances jittering and precision

data class Orientation(val heading: Float)

interface OrientationProvider : SensorEventListener {
    val orientation: StateFlow<Orientation>
    fun start()
    fun stop()
}

class AndroidOrientationProvider(context: Context) : OrientationProvider {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private var hasGravity = false

    private val geomagnetic = FloatArray(3)
    private var hasGeomagnetic = false

    private val mutableOrientation = MutableStateFlow(Orientation(0f))
    override val orientation: StateFlow<Orientation> = mutableOrientation

    override fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        Log.i("AndroidOrientationProvider", "started")
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
        Log.i("AndroidOrientationProvider", "stopped")
    }

    /** Combine accelerometer and magnetic sensor data to obtain the user heading direction.
     
    DO NOT TOUCH UNLESS YOU'RE ABSOLUTELY SURE YOU KNOW WHAT YOU'RE DOING. */
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(it.values, 0, gravity, 0, it.values.size)
                    hasGravity = true
                }

                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(it.values, 0, geomagnetic, 0, it.values.size)
                    hasGeomagnetic = true
                }
            }
            if (hasGravity && hasGeomagnetic) {
                val rotationMatrix = FloatArray(9)
                val orientationAngles = FloatArray(3)

                if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuth = toDegrees(orientationAngles[0].toDouble()).toFloat()

                    (orientation.value.heading).let { heading ->
                        val smoothHeading = smoothHeading(heading, azimuth)
                        if (shortestDelta(heading, smoothHeading).absoluteValue > DIFF_THRESHOLD * SMOOTHING_ALPHA)
                            mutableOrientation.update { it.copy(heading = smoothHeading) }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required but must be overridden
    }
}

private fun smoothHeading(current: Float, target: Float): Float {
    val shortestDelta = shortestDelta(current, target)
    return (current + SMOOTHING_ALPHA * shortestDelta + 360) % 360
}

private fun shortestDelta(current: Float, target: Float): Float {
    val positiveDiff = (target - current + 360) % 360
    return if (positiveDiff > 180) positiveDiff - 360 else positiveDiff
}
