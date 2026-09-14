package com.bzucker4.slate.lockout

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Mutes blackout hum while the proximity sensor is covered (pocket). */
class ProximityMonitor(
    context: Context,
    private val onCoveredChanged: (Boolean) -> Unit,
) : SensorEventListener {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = manager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var lastCovered: Boolean? = null

    val hasSensor: Boolean get() = sensor != null

    fun start() {
        val proximity = sensor ?: return
        lastCovered = null
        manager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        manager.unregisterListener(this)
        lastCovered = null
        onCoveredChanged(false)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val proximity = sensor ?: return
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val covered = event.values[0] < proximity.maximumRange
        if (covered != lastCovered) {
            lastCovered = covered
            onCoveredChanged(covered)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
