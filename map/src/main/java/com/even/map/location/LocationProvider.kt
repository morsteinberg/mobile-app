package com.even.map.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.even.map.types.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

const val LOCATION_SAMPLE_INTERVAL = 1000L

interface LocationProvider {
    val location: StateFlow<Location?>
    val availability: StateFlow<Boolean>
    fun startTracking()
    fun stopTracking()
}

@SuppressLint("MissingPermission")
class FusedLocationProvider(context: Context) : LocationProvider {
    private val mutableLocationFlow = MutableStateFlow<Location?>(null)
    override val location: StateFlow<Location?> = mutableLocationFlow

    private val mutableAvailability = MutableStateFlow(true)
    override val availability: StateFlow<Boolean> = mutableAvailability

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_SAMPLE_INTERVAL).apply {
            setMinUpdateIntervalMillis(0)
        }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            mutableLocationFlow.update { Location(locationResult.locations.last()) }
        }


        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            mutableAvailability.update { locationAvailability.isLocationAvailable }
        }
    }

    override fun startTracking() {
        Log.i("FusedLocationProvider", "Started tracking device location!")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { lastLocation ->
                lastLocation?.let { mutableLocationFlow.update { Location(lastLocation) } }
            }
            .addOnCompleteListener {
                fusedLocationClient.locationAvailability
                    .addOnSuccessListener { initialAvailability ->
                        initialAvailability?.let { mutableAvailability.update { initialAvailability.isLocationAvailable } }
                    }
                    .addOnCompleteListener {
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper(),
                        )
                    }
            }
    }

    override fun stopTracking() {
        Log.i("FusedLocationProvider", "Stopped tracking device location!")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
