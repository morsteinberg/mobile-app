package com.even.map.terraexplorer.functions

import com.even.core.logger.Logger
import com.even.map.terraexplorer.updateVisibility
import com.even.map.terraexplorer.wrappers.CreatorWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.even.map.types.Layer
import com.even.map.types.LayerSource
import com.skyline.teapi81.ITerrainRasterLayer
import kotlinx.coroutines.flow.Flow

internal suspend fun trackElevationLayer(
    state: Flow<Layer.ElevationLayer>,
    creatorWrapper: CreatorWrapper,
    logger: Logger,
) {
    var currentLayer: ITerrainRasterLayer? = null

    state.collect { elevationLayer ->
        val isNew = currentLayer == null

        if (isNew) {
            currentLayer = creatorWrapper.createElevationLayer(elevationLayer.layerSource)

            val layerSource = elevationLayer.layerSource
            logger.i(
                "Layer added successfully",
                buildMap {
                    put("id", elevationLayer.id)
                    put("type", "elevation")
                    put("source", layerSource.toString())
                    if (layerSource is LayerSource.Local) put("file Path", layerSource.filePath)
                },
            )
        }

        ThreadWrapper.launchLazy { currentLayer.updateVisibility(elevationLayer.isVisible) }
    }
}
