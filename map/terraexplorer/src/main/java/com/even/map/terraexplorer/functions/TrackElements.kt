package com.even.map.terraexplorer.functions

import com.even.core.logger.Logger
import com.even.map.providers.ElementsMapper
import com.even.map.terraexplorer.wrappers.CreatorWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.even.map.types.Element
import kotlinx.coroutines.flow.Flow

internal suspend fun trackElements(
    state: Flow<List<Element>>,
    creatorWrapper: CreatorWrapper,
    elementsMapper: ElementsMapper,
    logger: Logger,
) {
    StateTracker<Element, String>(
    ).collect(
        state = state,
        keySelector = { it.id },
        onAdded = { element ->
            addElement(creatorWrapper, elementsMapper, logger, element)
        },
        onRemoved = { element ->
            removeElementById(creatorWrapper, elementsMapper, logger, element.id)
        },
        onUpdated = { element ->
            removeElementById(creatorWrapper, elementsMapper, logger, element.id)
            addElement(creatorWrapper, elementsMapper, logger, element)
        },
    )
}

private suspend fun removeElementById(
    creatorWrapper: CreatorWrapper,
    elementsMapper: ElementsMapper,
    logger: Logger,
    elementId: String,
) {
    creatorWrapper.removeFeatures(elementsMapper.getExternalIdsByElementId(elementId).toList())
    elementsMapper.remove(elementId)

    logger.d("Element removed", mapOf("id" to elementId))
}

private suspend fun addElement(
    creatorWrapper: CreatorWrapper,
    elementsMapper: ElementsMapper,
    logger: Logger,
    element: Element,
) {
    val terraIds: List<String>? = when (element) {
        is Element.Point.Label -> {
            val result = creatorWrapper.createText(
                element.text,
                element.location,
                element.degree,
                element.color,
                element.size,
                element.underline,
                element.bold,
            )

            logger.i("Label feature added successfully", mapOf("location" to element.location))

            ThreadWrapper.launch { listOf(result.id) }
        }

        is Element.Polygon -> {
            val result = creatorWrapper.createPolygon(
                element.locations,
                element.lineColor,
                element.fillColor,
                element.linePattern,
            )

            logger.i("Polygon feature added successfully", mapOf("locations" to element.locations))

            ThreadWrapper.launch { listOf(result.id) }
        }

        is Element.Polyline -> {
            val result = creatorWrapper.createPolyline(element.locations, element.color, element.pattern)

            logger.i("Polyline feature added successfully", mapOf("locations" to element.locations))

            ThreadWrapper.launch { listOf(result.id) }
        }

        is Element.Arrow -> {
            val result = creatorWrapper.createArrow(element.locations, element.color, element.pattern)

            logger.i("Arrow feature added successfully", mapOf("locations" to element.locations))

            ThreadWrapper.launch { listOfNotNull(result.arrowShape.id, result.lineShape?.id) }
        }

        is Element.Point.IconPoint, is Element.Point.BasePoint, is Element.Temp -> null
    }

    terraIds?.let {
        elementsMapper.put(element, *terraIds.toTypedArray())
    }
}
