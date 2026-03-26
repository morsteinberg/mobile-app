package com.even.map.terraexplorer.wrappers

import android.graphics.Color
import androidx.annotation.DrawableRes
import com.even.map.terraexplorer.IconProvider
import com.even.map.terraexplorer.constants.Attribute
import com.even.map.terraexplorer.constants.LayerInitParam
import com.even.map.terraexplorer.constants.LayerPlugName
import com.even.map.terraexplorer.constants.toAltitudeTypeCode
import com.even.map.terraexplorer.constants.toLinePatternCode
import com.even.map.terraexplorer.sgWorldInstance
import com.even.map.types.CameraPosition
import com.even.map.types.Element
import com.even.map.types.Element.LinePattern
import com.even.map.types.LayerSource
import com.even.map.types.LayerSource.Local
import com.even.map.types.LayerSource.Remote
import com.even.map.types.Location
import com.even.map.types.MeshModel
import com.even.map.types.PositionAltitudeType
import com.skyline.teapi81.AltitudeTypeCode
import com.skyline.teapi81.IColor
import com.skyline.teapi81.IFeatureLayer
import com.skyline.teapi81.IGeometry
import com.skyline.teapi81.IMeshLayer
import com.skyline.teapi81.IPosition
import com.skyline.teapi81.ITerrainArrow
import com.skyline.teapi81.ITerrainImageLabel
import com.skyline.teapi81.ITerrainLabel
import com.skyline.teapi81.ITerrainPolygon
import com.skyline.teapi81.ITerrainPolyline
import com.skyline.teapi81.ITerrainRasterLayer
import com.skyline.teapi81.ITerrainRegularPolygon
import com.skyline.teapi81.LabelLockMode
import com.skyline.teapi81.LayerGeometryType
import com.skyline.teapi81.ReplaceTerrainMeshType

private object ElevationLayersConfig {
    const val NULL_VALUE = 7182.75
    const val NULL_TOLERANCE = 6000.0
    const val MAX_VISIBILITY_DISTANCE = 10000000.0
}

private const val MODEL_FLY_TO_DISTANCE = 1000.0
private const val LINE_WIDTH = -5.0
private const val VERTICAL_PITCH = -89.9
private const val TEXT_SCALE_FACTOR = 113

data class ArrowResult(val arrowShape: ITerrainArrow, val lineShape: ITerrainPolyline? = null)

private val creator
    get() = sgWorldInstance.creator

internal class CreatorWrapper(private val iconProvider: IconProvider) {

    fun createPosition(cameraPosition: CameraPosition): IPosition = ThreadWrapper.launchSync {
        creator.CreatePosition(
            cameraPosition.longitude,
            cameraPosition.latitude,
            cameraPosition.altitude,
            AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE,
            cameraPosition.yaw,
            cameraPosition.pitch.toFixedPitch(),
            cameraPosition.roll,
        )
    }

    fun createPosition(
        location: Location,
        type: PositionAltitudeType = PositionAltitudeType.TERRAIN_ABSOLUTE,
        pitch: Double = VERTICAL_PITCH,
    ): IPosition = ThreadWrapper.launchSync {
        creator.CreatePosition(
            location.longitude,
            location.latitude,
            if (type == PositionAltitudeType.ON_TERRAIN) 0.0 else location.altitude,
            type.toAltitudeTypeCode(),
            0.0,
            pitch,
            0.0,
        )
    }

    fun createCircle(
        radius: Double,
        lineColor: Int,
        fillColor: Int,
        location: Location? = null,
        numberOfSegments: Int? = null,
    ): ITerrainRegularPolygon =
        ThreadWrapper.launchSync {
            creator.CreateCircle(
                if (location != null) creator.CreatePosition(
                    location.longitude,
                    location.latitude,
                ) else creator.CreatePosition(),
                radius,
                createColor(lineColor),
                createColor(fillColor),
            ).apply { numberOfSegments?.let { this.numberOfSegments = it } }
        }

    fun createElevationLayer(layerSource: LayerSource): ITerrainRasterLayer =
        ThreadWrapper.launchSync {
            val (filePath, initParam) = layerSource.let {
                when (it) {
                    is Local -> Pair(it.filePath, "")
                    is Remote -> Pair(
                        it.fileName,
                        LayerInitParam.ofMTPElevaitonLayer(it.wmtsHostName, it.fileName),
                    )
                }
            }

            creator.CreateElevationLayer(
                filePath, 0.0, 0.0, 0.0, 0.0, initParam, LayerPlugName.MPT.value,
            ).apply {
                nullValue = ElevationLayersConfig.NULL_VALUE
                nullTolerance = ElevationLayersConfig.NULL_TOLERANCE
                visibility.maxVisibilityDistance = ElevationLayersConfig.MAX_VISIBILITY_DISTANCE
            }
        }

    suspend fun createFeatureLayer(fileName: String): IFeatureLayer = ThreadWrapper.launch {
        creator.CreateNewFeatureLayer(
            fileName,
            LayerGeometryType.LGT_POINT,
            LayerInitParam.ofFeatureLayer(fileName),
        )
    }

    fun createLabel(@DrawableRes icon: Int): ITerrainImageLabel = ThreadWrapper.launchSync {
        creator.CreateImageLabel(
            creator.CreatePosition(),
            iconProvider.getIconPath(icon).orEmpty(),
            creator.CreateLabelStyle().apply {
                maxImageSize = 150
                pivotAlignment = "Center, Center"
                iconColor = iconColor.apply {
                    SetAlpha(0.99)
                }
                lockMode = LabelLockMode.LM_AXIS
            },
        )
    }

    suspend fun createMeshModel(id: String, serverPath: String): MeshModel {
        val layer = ThreadWrapper.launch {
            creator.CreateMeshLayerFromSGS("https://$serverPath/sg/default/streamer.ashx", id)
                .apply {
                    replaceTerrainWithMesh =
                        ReplaceTerrainMeshType.REPLACE_TERRAIN_WITH_SIMPLIFIED_MESH
                }
        }
        val flyToReference = createFlyToReferenceObject(layer)
        return ThreadWrapper.launchSync {
            MeshModel(id, flyToReference.id, listOf(layer.id))
        }
    }

    suspend fun createPointOnLayer(
        layer: IFeatureLayer,
        point: Element.Point,
        attributes: List<Any?>,
    ): String =
        ThreadWrapper.launch {
            layer.featureGroups.point.CreateFeature(
                creator.geometryCreator.CreatePointGeometry(
                    doubleArrayOf(point.location.longitude, point.location.latitude, 0.0),
                ),
                Attribute.stringifyValues(attributes),
            )
        }

    suspend fun createRasterLayer(
        layerSource: LayerSource,
        isInitiallyVisible: Boolean,
    ): ITerrainRasterLayer =
        ThreadWrapper.launch {
            val (filePath, initParam, plugName) = layerSource.let {
                when (it) {
                    is Local -> Triple(it.filePath, "", "")
                    is Remote -> Triple(
                        it.fileName,
                        LayerInitParam.ofRasterLayer(it.wmtsHostName, it.fileName),
                        LayerPlugName.GIS.value,
                    )
                }
            }

            creator.CreateImageryLayer(filePath, 0.0, 0.0, 0.0, 0.0, initParam, plugName).apply {
                visibility.show = isInitiallyVisible
            }
        }

    fun createArrow(locations: List<Location>, color: Int, linePattern: LinePattern): ArrowResult {
        if (locations.size < 2) error("Locations size should be at least 2")

        val (lineShapeLocations, arrowShapeLocations) = locations.let {
            Pair(it.dropLast(1), it.takeLast(2))
        }
        val terraColor = createColor(color)

        var lineShape: ITerrainPolyline? = null

        if (lineShapeLocations.size > 1) {
            lineShape = createPolyline(lineShapeLocations, color, linePattern)
        }

        val (arrowBase, arrowHead) = arrowShapeLocations
        val distance = arrowBase.distance(arrowHead)
        val yaw = arrowBase.bearing(arrowHead)

        val arrowShape = ThreadWrapper.launchSync {
            creator.CreateArrow(
                creator.CreatePosition(
                    arrowHead.longitude,
                    arrowHead.latitude,
                    0.0,
                    AltitudeTypeCode.ATC_ON_TERRAIN,
                    yaw,
                ),
                distance,
                0,
                terraColor,
                "",
            ).apply {
                lineStyle.width = LINE_WIDTH
                lineStyle.pattern = linePattern.toLinePatternCode()
            }
        }

        return ArrowResult(arrowShape, lineShape)
    }

    suspend fun removeFeature(id: String) = ThreadWrapper.launch {
        creator.DeleteObject(id)
    }

    fun removeFeatures(ids: List<String>) = ThreadWrapper.launchSync {
        ids.forEach { creator.DeleteObject(it) }
    }

    suspend fun createText(
        text: String,
        location: Location,
        yaw: Double,
        color: Int,
        size: Double,
        underline: Boolean = false,
        bold: Boolean = false,
    ): ITerrainLabel = ThreadWrapper.launch {
        val position =
            creator.CreatePosition(location.longitude, location.latitude, 0.0, AltitudeTypeCode.ATC_ON_TERRAIN, yaw)
        creator.CreateTextLabel(
            position, text,
            creator.CreateLabelStyle().apply {
                this.textColor = createColor(color)
                this.fontSize = 72
                this.pivotAlignment = "Center; Center"
                this.textAlignment = "CenterCenter"
                this.bold = bold
                this.underline = underline
                this.limitScreenSize = false
                this.scale = size / TEXT_SCALE_FACTOR
                this.lockMode = LabelLockMode.LM_AXIS_TEXTUP
            },
        )
    }

    fun createPolygon(
        locations: List<Location>,
        lineColor: Int,
        fillColor: Int? = null,
        linePattern: LinePattern = LinePattern.FULL,
    ): ITerrainPolygon =
        ThreadWrapper.launchSync {
            creator.CreatePolygonFromArray(
                locations.flatMap { listOf(it.longitude, it.latitude, it.altitude) }
                    .toDoubleArray(),
                createColor(lineColor),
                createColor(fillColor ?: Color.TRANSPARENT),
                AltitudeTypeCode.ATC_ON_TERRAIN,
            ).apply {
                if (lineColor == Color.TRANSPARENT) lineStyle.color.setabgrColor(Color.TRANSPARENT)

                lineStyle.width = LINE_WIDTH
                lineStyle.pattern = linePattern.toLinePatternCode()
            }
        }

    fun createPolyline(locations: List<Location>, color: Int, linePattern: LinePattern): ITerrainPolyline =
        ThreadWrapper.launchSync {
            creator.CreatePolylineFromArray(
                locations.flatMap { listOf(it.longitude, it.latitude, it.altitude) }.toDoubleArray(),
                createColor(color),
                AltitudeTypeCode.ATC_ON_TERRAIN,
            ).apply {
                lineStyle.width = LINE_WIDTH
                lineStyle.pattern = linePattern.toLinePatternCode()
            }
        }

    fun createPolylineGeometry(vertices: List<Element.Point.BasePoint>): IGeometry =
        ThreadWrapper.launchSync {
            val verticesDoubleArray = vertices.flatMap { it.toTripletList() }.toDoubleArray()
            creator.geometryCreator.CreateLineStringGeometry(verticesDoubleArray).CastTo(IGeometry::class.java)
        }

    fun createPolygonGeometry(vertices: List<Element.Point.BasePoint>): IGeometry =
        ThreadWrapper.launchSync {
            val verticesDoubleArray = vertices.flatMap { it.toTripletList() }.toDoubleArray()
            creator.geometryCreator.CreateLinearRingGeometry(verticesDoubleArray).CastTo(IGeometry::class.java)
        }

    fun createColor(color: Int): IColor? = ThreadWrapper.launchSync {
        creator.CreateColor(
            Color.red(color),
            Color.green(color),
            Color.blue(color),
            Color.alpha(color),
        )
    }

    private fun createNodeIcon(location: Location, color: Int, maxSize: Int): ITerrainImageLabel =
        creator.CreateImageLabel(
            creator.CreatePosition(location.longitude, location.latitude, 0.0, AltitudeTypeCode.ATC_ON_TERRAIN),
            iconProvider.getColoredNodePath(color).orEmpty(),
            creator.CreateLabelStyle().apply {
                maxImageSize = maxSize
                pivotAlignment = "Center, Center"
                iconColor = iconColor.apply {
                    SetAlpha(0.99)
                }
                lockMode = LabelLockMode.LM_AXIS_TEXTUP
                smallestVisibleSize = 22
            },
        )

    fun createVerticesNodes(
        vertices: List<Element.Point.BasePoint>,
        fillColor: Int,
        maxSize: Int,
    ): List<ITerrainImageLabel> = ThreadWrapper.launchSync {
        vertices.map {
            createNodeIcon(it.location, fillColor, maxSize)
        }
    }

    private suspend fun createFlyToReferenceObject(layer: IMeshLayer): ITerrainPolygon =
        ThreadWrapper.launch {
            val altitude = TerrainWrapper.getMeshLayerAltitude(layer)
            val boundingBox = layer.bBox

            creator.CreatePolygonFromArray(
                doubleArrayOf(
                    boundingBox.minX, boundingBox.minY, 0.0,
                    boundingBox.minX, boundingBox.maxY, 0.0,
                    boundingBox.maxX, boundingBox.maxY, 0.0,
                    boundingBox.maxX, boundingBox.minY, 0.0,
                    boundingBox.minX, boundingBox.minY, 0.0,
                ),
                "#FFFFFF",
                "#FFFFFF",
                AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE,
                ProjectTreeWrapper.hiddenGroupId,
                "",
            ).apply {
                visibility.show = false
                position.altitude = altitude
                position.distance = MODEL_FLY_TO_DISTANCE
            }
        }
}

private fun Double.toFixedPitch() = if (this == 90.0) VERTICAL_PITCH else this
private fun Element.Point.BasePoint.toTripletList(): List<Double> =
    listOf(location.longitude, location.latitude, location.altitude)
