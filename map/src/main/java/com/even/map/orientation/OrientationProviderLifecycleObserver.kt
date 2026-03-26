package com.even.map.orientation

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class OrientationProviderLifecycleObserver(
    private val orientationProvider: OrientationProvider,
) : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) = orientationProvider.start()
    override fun onPause(owner: LifecycleOwner) = orientationProvider.stop()
}
