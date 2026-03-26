package com.even.map.types

import com.even.core.serialization.EnumSerializer
import com.even.map.types.Element.Arrow
import com.even.map.types.Element.Polygon
import com.even.map.types.Element.Polyline
import kotlinx.serialization.Serializable

@Serializable
sealed interface Element {
    val id: String

    fun getReferenceLocation(): Location

    enum class ElementType {
        Point, Polyline, Arrow, Polygon
    }

    @Serializable(with = LinePatternSerializer::class)
    enum class LinePattern {
        FULL, DOTTED, EXTRA_DOTTED
    }

    sealed interface Point : Element {
        val location: Location

        override fun getReferenceLocation(): Location {
            return location
        }

        abstract class BasePoint : Point

        abstract class IconPoint : Point {
            abstract val iconId: Int
            abstract val label: String?
        }

        abstract class Label : Point {
            abstract val text: String
            abstract val degree: Double
            abstract val color: Int
            abstract val size: Double
            abstract val underline: Boolean
            abstract val bold: Boolean

            override fun getReferenceLocation(): Location {
                return location
            }
        }
    }

    @Serializable
    data class Polyline(
        override val id: String,
        val locations: List<Location>,
        val color: Int,
        val pattern: LinePattern,
    ) : Element {
        override fun getReferenceLocation(): Location {
            return if (locations.isNotEmpty()) locations[locations.size / 2] else Location(0.0, 0.0)
        }

        companion object {
            const val ELEMENT_TYPE = "Polyline"
        }
    }

    @Serializable
    data class Polygon(
        override val id: String,
        val locations: List<Location>,
        val lineColor: Int,
        val fillColor: Int? = null,
        val linePattern: LinePattern,
    ) : Element {

        // TODO: add a smarter calculation for the location based on the entire shape rather than the middle node
        override fun getReferenceLocation(): Location {
            return if (locations.isNotEmpty()) locations[locations.size / 2] else Location(0.0, 0.0)
        }

        companion object {
            const val ELEMENT_TYPE = "Polygon"
        }
    }

    @Serializable
    data class Arrow(
        override val id: String,
        val locations: List<Location>,
        val color: Int,
        val pattern: LinePattern,
    ) : Element {
        override fun getReferenceLocation(): Location {
            return if (locations.isNotEmpty()) locations[locations.size / 2] else Location(0.0, 0.0)
        }

        companion object {
            const val ELEMENT_TYPE = "Arrow"
        }
    }

    data class Temp(
        override val id: String,
        val vertices: List<Point.BasePoint>,
        val color: Int,
        val type: ElementType,
        val pattern: LinePattern,
        val fillColor: Int? = null,
    ) : Element {

        val value = createElement(id, vertices.map { it.location }, color, type, fillColor, pattern)

        override fun getReferenceLocation(): Location = value.getReferenceLocation()

        fun calcMidNodes(): List<Point.BasePoint> {
            if (value is Point) return emptyList()

            val edges = if (value is Polygon) vertices.zipWithNext() + listOf(vertices.last() to vertices.first())
            else vertices.zipWithNext()

            return edges.map { (start, end) ->
                DefaultPoint(
                    stableMidpointId(start.id, end.id),
                    start.location.middleLocation(end.location),
                )
            }
        }
    }
}

private fun createElement(
    id: String,
    locations: List<Location>,
    color: Int,
    type: Element.ElementType,
    fillColor: Int? = null,
    pattern: Element.LinePattern,
): Element = when (locations.size) {
    1 -> DefaultPoint(id, locations.first())
    2 -> if (type == Element.ElementType.Arrow) {
        Arrow(id, locations, color, pattern)
    } else {
        Polyline(id, locations, color, pattern)
    }
    else -> when (type) {
        Element.ElementType.Polygon -> Polygon(id, locations, color, fillColor, pattern)
        Element.ElementType.Polyline -> Polyline(id, locations, color, pattern)
        Element.ElementType.Arrow -> Arrow(id, locations, color, pattern)
        Element.ElementType.Point -> DefaultPoint(id, locations.first())
    }
}

private fun stableMidpointId(startId: String, endId: String) = "mid_${startId}_${endId}"

data class DefaultPoint(override val id: String, override val location: Location) : Element.Point.BasePoint()

object LinePatternSerializer : EnumSerializer<Element.LinePattern>(Element.LinePattern.entries.toTypedArray())
