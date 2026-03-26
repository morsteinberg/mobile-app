package com.even.map.terraexplorer.constants

import com.even.map.types.Element.LinePattern

enum class LinePatternCode(val code: Int) {
    SOLID(0xFFFFFFFF.toInt()),
    XLARGE_DASH(0xFFF00FFF.toInt()),
    LARGE_DASH(0xFF0000FF.toInt()),
    MEDIUM_DASH(0xF00FF00F.toInt()),
    SMALL_DASH(0xC3C3C3C3.toInt()),
    TINY_DASH(0x99999999.toInt()),
    DOTS(0xAAAAAAAA.toInt()),
    DASH_DOT_DASH(0xFF0180FF.toInt()),
    DASH_DOT_DOT_DASH(0xFF0C30FF.toInt()),
}

fun LinePattern.toLinePatternCode(): Int =
    when (this) {
        LinePattern.FULL -> LinePatternCode.SOLID.code
        LinePattern.DOTTED -> LinePatternCode.LARGE_DASH.code
        LinePattern.EXTRA_DOTTED -> LinePatternCode.SMALL_DASH.code
    }
