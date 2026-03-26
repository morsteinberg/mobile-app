package com.even.map.terraexplorer.constants

import com.even.map.types.PositionAltitudeType
import com.skyline.teapi81.AltitudeTypeCode

fun PositionAltitudeType.toAltitudeTypeCode(): Int =
    when (this) {
        PositionAltitudeType.ON_TERRAIN -> AltitudeTypeCode.ATC_ON_TERRAIN
        PositionAltitudeType.TERRAIN_ABSOLUTE -> AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE
        PositionAltitudeType.TERRAIN_RELATIVE -> AltitudeTypeCode.ATC_TERRAIN_RELATIVE
    }
