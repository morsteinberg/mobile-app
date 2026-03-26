package com.even.map.types

import com.even.core.extensions.StringExtensions.generateRandomUUID
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import kotlinx.serialization.Serializable
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import android.location.Location as AndroidLocation

private const val ISRAEL_ZONE_NUMBER = 36
private const val IS_ISRAEL_NORTHERN_HEMISPHERE = true
private const val COORDINATE_REFERENCE_SYSTEM = "EPSG:4326"

@Serializable
data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0.0F,
    val speed: Float = 0.0F,
    val bearing: Float = 0.0F,
    val time: Long = 0,
    val provider: String? = null,
) {
    constructor(location: AndroidLocation) : this(
        latitude = location.latitude,
        longitude = location.longitude,
        altitude = location.altitude,
        accuracy = location.accuracy,
        speed = location.speed,
        bearing = location.bearing,
        time = location.time,
        provider = location.provider,
    )

    fun toUtm(): UtmLocation {
        val crsFactory = CRSFactory()
        val transformFactory = CoordinateTransformFactory()

        // WSG84 (lat/long)
        val sourceCRS: CoordinateReferenceSystem = crsFactory.createFromName("EPSG:4326")

        // UTM ZONE 36N (for Israel)
        val targetCRS: CoordinateReferenceSystem = crsFactory.createFromName("EPSG:32636")

        val transform = transformFactory.createTransform(sourceCRS, targetCRS)

        val fromCoordinate = ProjCoordinate(longitude, latitude)
        val toCoordinate = ProjCoordinate()

        transform.transform(fromCoordinate, toCoordinate)

        return UtmLocation(easting = toCoordinate.x.toInt(), northing = toCoordinate.y.toInt())
    }

    fun distance(location: Location): Double {
        val results = FloatArray(1)
        AndroidLocation.distanceBetween(
            this.latitude,
            this.longitude,
            location.latitude,
            location.longitude,
            results,
        )
        return results[0].toDouble()
    }

    fun bearing(to: Location): Double {
        val fromTurfPoint = Point.fromLngLat(this.longitude, this.latitude)
        val toTurfPoint = Point.fromLngLat(to.longitude, to.latitude)

        return TurfMeasurement.bearing(fromTurfPoint, toTurfPoint)
    }

    fun middleLocation(other: Location) = Location(
        latitude = (this.latitude + other.latitude) / 2,
        longitude = (this.longitude + other.longitude) / 2,
        altitude = (this.altitude + other.altitude) / 2,
    )

    fun toBasePoint(id: String? = null): Element.Point.BasePoint = DefaultPoint(id ?: String.generateRandomUUID(), this)
}

data class UtmLocation(val easting: Int, val northing: Int) {
    fun toLocation(): Location {
        val crsFactory = CRSFactory()

        val hemisphereCode = if (IS_ISRAEL_NORTHERN_HEMISPHERE) "326" else "327"
        val utmCRSCode = "EPSG:$hemisphereCode$ISRAEL_ZONE_NUMBER"

        val utmCRS: CoordinateReferenceSystem = crsFactory.createFromName(utmCRSCode)
        val wgs84CRS: CoordinateReferenceSystem =
            crsFactory.createFromName(COORDINATE_REFERENCE_SYSTEM)

        val source = ProjCoordinate(easting.toDouble(), northing.toDouble())
        val target = ProjCoordinate()

        CoordinateTransformFactory().createTransform(utmCRS, wgs84CRS).transform(source, target)
        return Location(latitude = target.y, longitude = target.x)
    }

    fun isValid() = easting in 100_000..900_000 && northing in 0..10_000_000

    // TODO: move to string resource template and use where needed
    override fun toString(): String = "$easting / $northing"
}
