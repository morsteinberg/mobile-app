package com.even.map.types


object BBox {
    private const val MAX_ALTITUDE = 1_000_000
    private const val MIN_LAT = 29.464125450826593
    private const val MAX_LAT = 33.362387945693136
    private const val MIN_LONG = 34.219003053242886
    private const val MAX_LONG = 35.92407964250427

    fun isLocationValid(location: Location?) =
        location != null
            && location.altitude <= MAX_ALTITUDE
            && location.longitude in MIN_LONG..MAX_LONG
            && location.latitude in MIN_LAT..MAX_LAT
}
