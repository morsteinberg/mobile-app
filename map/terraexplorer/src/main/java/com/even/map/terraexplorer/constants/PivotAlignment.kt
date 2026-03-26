package com.even.map.terraexplorer.constants

import com.even.map.types.AnchorPosition

enum class PivotAlignment(val id: Int) {
    TOP_LEFT(10),
    TOP_CENTER(11),
    TOP_RIGHT(12),
    CENTER_LEFT(13),
    CENTER_CENTER(14),
    CENTER_RIGHT(15),
    BOTTOM_LEFT(16),
    BOTTOM_CENTER(17),
    BOTTOM_RIGHT(18),
}

fun AnchorPosition.toPivotAlignment() = when(this) {
    AnchorPosition.TOP_LEFT -> PivotAlignment.TOP_LEFT
    AnchorPosition.TOP_CENTER -> PivotAlignment.TOP_CENTER
    AnchorPosition.TOP_RIGHT -> PivotAlignment.TOP_RIGHT
    AnchorPosition.CENTER_LEFT -> PivotAlignment.CENTER_LEFT
    AnchorPosition.CENTER_CENTER -> PivotAlignment.CENTER_CENTER
    AnchorPosition.CENTER_RIGHT -> PivotAlignment.CENTER_RIGHT
    AnchorPosition.BOTTOM_LEFT -> PivotAlignment.BOTTOM_LEFT
    AnchorPosition.BOTTOM_CENTER -> PivotAlignment.BOTTOM_CENTER
    AnchorPosition.BOTTOM_RIGHT -> PivotAlignment.BOTTOM_RIGHT
}
