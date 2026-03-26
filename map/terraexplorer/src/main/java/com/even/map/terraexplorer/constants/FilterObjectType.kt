package com.even.map.terraexplorer.constants

import com.even.map.types.FilterObjectType
import com.skyline.teapi81.WorldPointType

fun FilterObjectType.toWPT() = when (this) {
    FilterObjectType.LABEL -> WorldPointType.WPT_LABEL
    FilterObjectType.DRAWABLES -> WorldPointType.WPT_PRIMITIVE + WorldPointType.WPT_LABEL
}
