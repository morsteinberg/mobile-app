package com.even.map.terraexplorer.functions

import com.even.core.logger.Logger
import com.even.map.providers.ElementsMapper
import com.even.map.terraexplorer.IconProvider
import com.even.map.terraexplorer.constants.ObjectType
import com.even.map.terraexplorer.constants.toLinePatternCode
import com.even.map.terraexplorer.wrappers.CreatorWrapper
import com.even.map.terraexplorer.wrappers.ProjectTreeWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.even.map.types.Element
import com.even.map.types.Element.Arrow
import com.even.map.types.Element.ElementType.Arrow
import com.even.map.types.Element.ElementType.Polygon
import com.even.map.types.Element.ElementType.Polyline
import com.even.map.types.Element.Polygon
import com.even.map.types.Element.Polyline
import com.even.map.types.Location
import com.even.map.types.PositionAltitudeType
import com.skyline.teapi81.ITerrainArrow
import com.skyline.teapi81.ITerrainImageLabel
import com.skyline.teapi81.ITerrainPolygon
import com.skyline.teapi81.ITerrainPolyline
import com.skyline.teapi81.TEIUnknownHandle
import kotlinx.coroutines.flow.Flow

private const val DEFAULT_NODE_MAX_SIZE = 50
private const val MIDPOINT_NODE_MAX_SIZE = 35

var previousState: Element.Temp? = null
var previousMidNodes: List<Element.Point.BasePoint>? = null

internal suspend fun trackTempElement(
    state: Flow<Element.Temp?>,
    creatorWrapper: CreatorWrapper,
    elementsMapper: ElementsMapper,
    iconProvider: IconProvider,
    logger: Logger,
) {
    state.collect { currentState ->
        val prev = previousState
        ThreadWrapper.launchSync {
            if (currentState == null) {
                prev?.let { removeTempElement(it, previousMidNodes, elementsMapper, creatorWrapper, logger) }

            } else if (prev == null) {
                drawTempElement(currentState, creatorWrapper, elementsMapper, logger)
            } else if (prev.id != currentState.id) {
                removeTempElement(prev, previousMidNodes, elementsMapper, creatorWrapper, logger)
                drawTempElement(currentState, creatorWrapper, elementsMapper, logger)
            } else {
                val externalIds = elementsMapper.getExternalIdsByElementId(prev.id)
                val terraElement = externalIds.mapNotNull { ProjectTreeWrapper.getAndResolveObject(it) }
                if (prev.vertices != currentState.vertices) {
                    val mainElement = terraElement.first()

                    if (currentState.type == Polyline && mainElement is ITerrainPolyline) {
                        updatePolylineShape(mainElement, currentState.vertices, creatorWrapper)
                        updateBasePointsLocation(currentState, prev, elementsMapper, creatorWrapper)
                    } else if (currentState.type == Polygon && mainElement is ITerrainPolygon) {
                        updatePolygonShape(mainElement, currentState.vertices, creatorWrapper)
                        updateBasePointsLocation(currentState, prev, elementsMapper, creatorWrapper)
                    } else if (currentState.type == Arrow && mainElement is ITerrainPolyline && terraElement.last() is ITerrainArrow) {
                        updatePolylineShape(mainElement, currentState.vertices, creatorWrapper)
                        updateArrowShape(terraElement.last() as ITerrainArrow, currentState.vertices)
                        updateBasePointsLocation(currentState, prev, elementsMapper, creatorWrapper)
                    } else {
                        removeTempElement(prev, previousMidNodes, elementsMapper, creatorWrapper, logger)
                        drawTempElement(currentState, creatorWrapper, elementsMapper, logger)
                    }
                }
                if (prev.color != currentState.color) {
                    changeShapeColor(currentState, terraElement, creatorWrapper)
                    changeBasePointsColor(currentState, iconProvider, elementsMapper)
                }
                if (prev.pattern != currentState.pattern) {
                    changeLinePattern(currentState, terraElement)
                }
            }

            previousState = currentState
        }
    }
}

private fun updatePolylineShape(
    polyline: ITerrainPolyline,
    vertices: List<Element.Point.BasePoint>,
    creatorWrapper: CreatorWrapper,
) {
    polyline.geometry = creatorWrapper.createPolylineGeometry(vertices)
}

private fun updatePolygonShape(
    polygon: ITerrainPolygon,
    vertices: List<Element.Point.BasePoint>,
    creatorWrapper: CreatorWrapper,
) {
    polygon.geometry = creatorWrapper.createPolygonGeometry(vertices)
}

private fun updateArrowShape(
    arrow: ITerrainArrow,
    vertices: List<Element.Point.BasePoint>,
) {
    val (tailLocation, headLocation) = vertices.takeLast(2).map { it.location }

    arrow.apply {
        headX = headLocation.longitude
        headY = headLocation.latitude
        tailX = tailLocation.longitude
        tailY = tailLocation.latitude
    }

}

private fun updateBasePointsLocation(
    currentState: Element.Temp,
    prevState: Element.Temp,
    elementsMapper: ElementsMapper,
    creatorWrapper: CreatorWrapper,
) {
    val midNodes = currentState.calcMidNodes()
    val verticesDiffs = calcPointsDiffs(prevState.vertices, currentState.vertices)
    val midPointsDiffs = previousMidNodes?.let { calcPointsDiffs(it, midNodes) } ?: PointsDiffs(pointsToAdd = midNodes)

    midPointsDiffs.pointsToDelete.forEach { removeBasePoint(it, elementsMapper, creatorWrapper) }

    val terraNodes = creatorWrapper.createVerticesNodes(
        verticesDiffs.pointsToAdd,
        currentState.color,
        DEFAULT_NODE_MAX_SIZE,
    )
    val terraMidNodes = creatorWrapper.createVerticesNodes(
        midPointsDiffs.pointsToAdd,
        currentState.color,
        MIDPOINT_NODE_MAX_SIZE,
    )

    verticesDiffs.pointsToAdd.forEachIndexed { index, vertex ->
        elementsMapper.put(vertex, terraNodes[index].id)
    }
    midPointsDiffs.pointsToAdd.forEachIndexed { index, vertex ->
        elementsMapper.put(vertex, terraMidNodes[index].id)
    }

    (verticesDiffs.pointsToUpdate + midPointsDiffs.pointsToUpdate).forEach {
        updateBasePointLocation(
            it,
            elementsMapper,
            creatorWrapper,
        )
    }

    previousMidNodes = midNodes
}

private fun changeShapeColor(
    currentState: Element.Temp,
    terraElement: List<TEIUnknownHandle>,
    creatorWrapper: CreatorWrapper,
) {
    terraElement.forEach { terraElement ->
        when (terraElement) {
            is ITerrainPolyline -> terraElement.lineStyle.color = creatorWrapper.createColor(currentState.color)
            is ITerrainPolygon -> {
                terraElement.apply {
                    lineStyle.color = creatorWrapper.createColor(currentState.color)
                    currentState.fillColor?.let {
                        fillStyle.color = creatorWrapper.createColor(it)
                    }
                }
            }
            is ITerrainArrow -> terraElement.lineStyle.color = creatorWrapper.createColor(currentState.color)
        }
    }
}

private fun changeBasePointsColor(
    currentState: Element.Temp,
    iconProvider: IconProvider,
    elementsMapper: ElementsMapper,
) {
    val terraElements = mutableListOf<ITerrainImageLabel>()
    val verticesExternalIds = currentState.vertices.map { elementsMapper.getExternalIdsByElementId(it.id) }
    terraElements.addAll(
        verticesExternalIds.mapNotNull { externalId ->
            ProjectTreeWrapper.getAndResolveObject(
                externalId.first(),
            ) as ITerrainImageLabel?
        },
    )

    previousMidNodes?.let {
        val externalIds = it.map { elementsMapper.getExternalIdsByElementId(it.id) }
        terraElements.addAll(externalIds.mapNotNull { externalId -> ProjectTreeWrapper.getAndResolveObject(externalId.first()) as ITerrainImageLabel? })
    }

    val updatedIcon = iconProvider.getColoredNodePath(currentState.color)

    terraElements.forEach { it.imageFileName = updatedIcon }
}

private fun changeLinePattern(
    currentState: Element.Temp,
    terraElement: List<TEIUnknownHandle>,
) {
    terraElement.forEach { terraElement ->
        when (terraElement) {
            is ITerrainPolyline -> terraElement.lineStyle.pattern = currentState.pattern.toLinePatternCode()
            is ITerrainPolygon -> terraElement.lineStyle.pattern = currentState.pattern.toLinePatternCode()
            is ITerrainArrow -> terraElement.lineStyle.pattern = currentState.pattern.toLinePatternCode()
        }
    }
}

data class PointsDiffs(
    val pointsToAdd: List<Element.Point.BasePoint> = emptyList(),
    val pointsToUpdate: List<Element.Point.BasePoint> = emptyList(),
    val pointsToDelete: List<Element.Point.BasePoint> = emptyList(),
)

private fun calcPointsDiffs(
    currentPoints: List<Element.Point.BasePoint>,
    updatedPoints: List<Element.Point.BasePoint>,
): PointsDiffs {
    val updatedIds = updatedPoints.map { it.id }
    val pointsToDelete = currentPoints.filter { !updatedIds.contains(it.id) }

    val currentIds = currentPoints.map { it.id }
    val pointsToAdd = updatedPoints.filter { !currentIds.contains(it.id) }

    val updates = currentPoints.mapNotNull { curr ->
        val updatedPoint = updatedPoints.find { it.id == curr.id }
        if (curr.location.partialEquals(updatedPoint?.location)) null
        else updatedPoint
    }
    return PointsDiffs(pointsToAdd = pointsToAdd, pointsToUpdate = updates, pointsToDelete = pointsToDelete)
}

private fun updateBasePointLocation(
    point: Element.Point.BasePoint,
    elementsMapper: ElementsMapper,
    creatorWrapper: CreatorWrapper,
) {
    ThreadWrapper.launchSync {
        val terraElement = elementsMapper.getExternalIdsByElementId(point.id).let {
            ProjectTreeWrapper.getElement(it.first())
        }

        if (terraElement.objectType == ObjectType.IMAGE_LABEL.value) {
            val imageLabel = terraElement.CastTo(ITerrainImageLabel::class.java)

            val newPosition = creatorWrapper.createPosition(point.location, PositionAltitudeType.ON_TERRAIN, 0.0)
            imageLabel.position = newPosition
        }
    }
}

private fun removeBasePoint(
    point: Element.Point.BasePoint,
    elementsMapper: ElementsMapper,
    creatorWrapper: CreatorWrapper,
) {
    val terraId = elementsMapper.getExternalIdsByElementId(point.id)
    elementsMapper.remove(point.id)
    creatorWrapper.removeFeatures(terraId.toList())
}

private fun removeTempElement(
    element: Element.Temp,
    prevMidNodes: List<Element.Point.BasePoint>?,
    elementsMapper: ElementsMapper,
    creatorWrapper: CreatorWrapper,
    logger: Logger,
) {
    val terraObjectsIds = mutableListOf<String>()

    (element.vertices + prevMidNodes.orEmpty()).forEach {
        terraObjectsIds.addAll(elementsMapper.getExternalIdsByElementId(it.id))
        elementsMapper.remove(it.id)
    }

    terraObjectsIds.addAll(elementsMapper.getExternalIdsByElementId(element.id))
    elementsMapper.remove(element.id)

    creatorWrapper.removeFeatures(terraObjectsIds)
    logger.i("Track Temp - removed prev element", mapOf("element id" to element.id, "type" to element.value::class))

}

private fun drawTempElement(
    element: Element.Temp,
    creatorWrapper: CreatorWrapper,
    elementsMapper: ElementsMapper,
    logger: Logger,
) = ThreadWrapper.launchSync {
    val terraIds: List<String>? = when (element.value) {
        is Polyline -> {
            val polylineResult =
                creatorWrapper.createPolyline(element.vertices.map { it.location }, element.color, element.pattern)
            listOf(polylineResult.id)
        }
        is Arrow -> {
            val arrowResult =
                creatorWrapper.createArrow(element.vertices.map { it.location }, element.color, element.pattern)
            listOfNotNull(arrowResult.lineShape?.id, arrowResult.arrowShape.id)
        }
        is Polygon -> {
            val polygonResult = creatorWrapper.createPolygon(
                element.vertices.map { it.location },
                element.color,
                element.fillColor,
                element.pattern,
            )
            listOf(polygonResult.id)
        }
        is Element.Point.BasePoint -> {
            val terraNodes = creatorWrapper.createVerticesNodes(element.vertices, element.color, DEFAULT_NODE_MAX_SIZE)
            listOf(terraNodes.first().id)
        }
        else -> null
    }

    terraIds?.let {
        elementsMapper.put(element, *terraIds.toTypedArray())
    }

    if (element.value !is Element.Point) {
        val midNodes = element.calcMidNodes()

        val terraNodes = creatorWrapper.createVerticesNodes(element.vertices, element.color, DEFAULT_NODE_MAX_SIZE)
        val terraMidNodes = creatorWrapper.createVerticesNodes(midNodes, element.color, MIDPOINT_NODE_MAX_SIZE)

        element.vertices.mapIndexed { index, vertex ->
            elementsMapper.put(vertex, terraNodes[index].id)
        }
        midNodes.mapIndexed { index, vertex ->
            elementsMapper.put(vertex, terraMidNodes[index].id)
        }

        previousMidNodes = midNodes
    }

    logger.i(
        "Track Temp - draw new element",
        mapOf("element id" to element.id, "type" to element.value::class, "vertices size" to element.vertices.size),
    )
}

private fun Location.partialEquals(other: Location?): Boolean {
    return latitude == other?.latitude && longitude == other.longitude && altitude == other.altitude
}
