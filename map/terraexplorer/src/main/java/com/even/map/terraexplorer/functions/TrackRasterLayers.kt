package com.even.map.terraexplorer.functions

import com.even.core.logger.Logger
import com.even.map.terraexplorer.updateVisibility
import com.even.map.terraexplorer.wrappers.CreatorWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.even.map.types.Layer.RasterLayer
import com.skyline.teapi81.ITerrainRasterLayer
import kotlinx.coroutines.flow.Flow

internal suspend fun trackRasterLayers(
    state: Flow<List<RasterLayer>>,
    creatorWrapper: CreatorWrapper,
    logger: Logger,
) {
    val rasterLayerIdToTerrainMapper: MutableMap<String, ITerrainRasterLayer> = mutableMapOf()

    StateTracker<RasterLayer, String>().collect(
        state = state,
        keySelector = { it.id },
        onAdded = { rasterLayer ->
            rasterLayerIdToTerrainMapper[rasterLayer.id] =
                creatorWrapper.createRasterLayer(rasterLayer.layerSource, rasterLayer.isVisible)
        },
        onUpdated = { rasterLayer ->
            rasterLayerIdToTerrainMapper[rasterLayer.id]?.let { terrain ->
                ThreadWrapper.launchLazy {
                    terrain.updateVisibility(rasterLayer.isVisible)

                    logger.i(
                        "Updated raster layer visibility",
                        mapOf("id" to rasterLayer.id, "visibility" to terrain.visibility.show),
                    )
                }
            }
        },
    )
}
