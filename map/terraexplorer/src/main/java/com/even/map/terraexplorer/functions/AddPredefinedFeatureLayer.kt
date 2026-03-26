package com.even.map.terraexplorer.functions

import com.even.core.logger.Logger
import com.even.map.terraexplorer.predefinedLayersGroupName
import com.even.map.terraexplorer.wrappers.ProjectTreeWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.skyline.teapi81.IFeatureLayer
import java.io.File

internal suspend fun addPredefinedFeatureLayer(
    id: String,
    isVisible: Boolean,
    fileName: File,
    logger: Logger,
): IFeatureLayer? {
    val layer = ProjectTreeWrapper.getLayer("$predefinedLayersGroupName\\$id")

    if (layer == null) {
        logger.e(
            "Failed to load predefined feature layer from local fly file",
            mapOf("layerId" to id),
        )
        return null
    }

    val layerName = fileName.nameWithoutExtension
    val connectionString = "FileName=$fileName;TEPlugName=OGR;LayerName=$layerName"
    ThreadWrapper.launch {
        layer.apply {
            dataSourceInfo.connectionString = connectionString
            Refresh()
            visibility.show = isVisible
        }
    }

    logger.i(
        "Layer added successfully",
        mapOf("id" to id, "type" to "predefined feature", "source" to "local"),
    )
    return layer
}
