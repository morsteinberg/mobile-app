package com.even.map.types

import com.even.core.serialization.EnumSerializer
import kotlinx.serialization.Serializable

private object AnchorPositionSerializer : EnumSerializer<AnchorPosition>(AnchorPosition.entries.toTypedArray())

@Serializable(with = AnchorPositionSerializer::class)
enum class AnchorPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER_CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
}
