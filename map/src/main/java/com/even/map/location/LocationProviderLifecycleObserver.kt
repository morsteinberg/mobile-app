package com.even.map.location

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class LocationProviderLifecycleObserver(
    private val locationProvider: LocationProvider,
) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) = locationProvider.startTracking()
    override fun onDestroy(owner: LifecycleOwner) = locationProvider.stopTracking()
}
