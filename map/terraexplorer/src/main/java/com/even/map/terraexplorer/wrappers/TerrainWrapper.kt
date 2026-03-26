package com.even.map.terraexplorer.wrappers

import com.even.map.terraexplorer.sgWorldInstance
import com.even.map.types.Location
import com.skyline.teapi81.AccuracyLevel
import com.skyline.teapi81.IMeshLayer
import com.skyline.teapi81.IPosition

private val terrain
    get() = sgWorldInstance.terrain

private const val MARGIN_ALTITUDE_FROM_GROUND = 2

internal object TerrainWrapper {
    fun getMeshLayerAltitude(layer: IMeshLayer): Double = ThreadWrapper.launchSync {
        getPosition(layer.position.y, layer.position.x, AccuracyLevel.ACCURACY_NORMAL).altitude
    }

    fun getPositionWithHeightMargin(location: Location): IPosition = ThreadWrapper.launchSync {
        getPosition(location).apply { altitude += MARGIN_ALTITUDE_FROM_GROUND }
    }

    private fun getPosition(
        location: Location,
        accuracyLevel: Int = AccuracyLevel.ACCURACY_FORCE_BEST_RENDERED,
    ) = getPosition(location.latitude, location.longitude, accuracyLevel)

    private fun getPosition(
        lat: Double,
        long: Double,
        accuracyLevel: Int = AccuracyLevel.ACCURACY_FORCE_BEST_RENDERED,
    ) = ThreadWrapper.launchSync {
        terrain.GetGroundHeightInfo(long, lat, accuracyLevel).position
    }
}
