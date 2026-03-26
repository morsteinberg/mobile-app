package com.even.map.providers

import androidx.appcompat.app.AppCompatActivity
import com.even.map.location.FusedLocationProvider
import com.even.map.location.LocationProvider
import com.even.map.location.LocationProviderLifecycleObserver
import com.even.map.orientation.AndroidOrientationProvider
import com.even.map.orientation.OrientationProvider
import com.even.map.orientation.OrientationProviderLifecycleObserver

class SelfLocationProvider(activity: AppCompatActivity) {
    private val locationProvider: LocationProvider = FusedLocationProvider(activity)
    private val orientationProvider: OrientationProvider = AndroidOrientationProvider(activity)

    val location = locationProvider.location
    val orientation = orientationProvider.orientation
    val availability = locationProvider.availability

    init {
        activity.lifecycle.addObserver(LocationProviderLifecycleObserver(locationProvider))
        activity.lifecycle.addObserver(OrientationProviderLifecycleObserver(orientationProvider))
    }
}
