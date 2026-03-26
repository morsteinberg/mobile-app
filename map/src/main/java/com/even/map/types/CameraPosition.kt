package com.even.map.types

import kotlinx.serialization.Serializable

@Serializable
data class CameraPosition(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val yaw: Double = 0.0,
    val pitch: Double = 0.0,
    val roll: Double = 0.0,
)
