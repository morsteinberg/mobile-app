package com.even.map.terraexplorer.functions

import android.graphics.Color
import com.even.map.providers.ElementsMapper
import com.even.map.terraexplorer.wrappers.CreatorWrapper
import com.even.map.terraexplorer.wrappers.ListenersWrapper
import com.even.map.terraexplorer.wrappers.NavigateWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.even.map.types.BBox
import com.even.map.types.FlyingAnimation
import com.even.map.types.Location
import com.even.map.types.SelfLocationController
import com.even.map.types.ViewPoint
import com.even.map.types.ViewPoint.CenteredOnElement
import com.even.map.types.ViewPoint.CenteredOnUser
import com.even.map.types.ViewPoint.FlyingToObjectId
import com.even.map.types.ViewPoint.Heading
import com.skyline.teapi81.IPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private val TRANSPARENT_COLOR = Color.argb(0, 0, 0, 0)

internal fun addViewPointController(
    scope: CoroutineScope,
    elementsMapper: ElementsMapper,
    viewPointState: Flow<ViewPoint>,
    selfLocation: SelfLocationController,
    creatorWrapper: CreatorWrapper,
    onFinishedFlying: () -> Unit,
    onPositionInvalid: () -> Unit,
) {
    var lastValidCameraPosition: IPosition? = null
    var bBoxEnforcement = false
    var flyToTrackingFunction: (suspend () -> Unit?)? = null

    suspend fun followSelectedElement(currentViewPoint: CenteredOnElement) =
        elementsMapper.getExternalIdsByElement(currentViewPoint.element).firstOrNull()?.let {
            NavigateWrapper.flyTo(it)
        }

    fun getFlyToTrackingFunction(currentViewPoint: ViewPoint) = when (currentViewPoint) {
        Heading -> suspend { selfLocation.rotateToHeadingBeam(FlyingAnimation.JUMP) }
        CenteredOnUser -> suspend { selfLocation.flyToLocationIndicator(FlyingAnimation.ABOVE) }
        is CenteredOnElement -> suspend { followSelectedElement(currentViewPoint) }
        else -> null
    }

    suspend fun flyToRefObject(locations: List<Location>) {
        val flyToObjectRef = creatorWrapper.createPolygon(
            locations,
            TRANSPARENT_COLOR,
            TRANSPARENT_COLOR,
        )

        val objectId = ThreadWrapper.launch { flyToObjectRef.id }

        NavigateWrapper.flyTo(objectId)
        creatorWrapper.removeFeature(objectId)
    }

    scope.launch {
        viewPointState.collect { newViewPoint ->
            flyToTrackingFunction = null
            bBoxEnforcement = false

            when (newViewPoint) {
                ViewPoint.FreeRoam -> Unit
                Heading -> selfLocation.rotateToHeadingBeam(FlyingAnimation.BEHIND_AND_ABOVE)
                CenteredOnUser -> {
                    selfLocation.flyToLocationIndicator(FlyingAnimation.ABOVE)
                }
                is FlyingToObjectId -> {
                    NavigateWrapper.flyTo(newViewPoint.objectId)
                    onFinishedFlying()
                }

                is CenteredOnElement -> followSelectedElement(newViewPoint)

                is ViewPoint.FlyingToLocation -> {
                    if (newViewPoint.locations.size == 1) {
                        NavigateWrapper.flyTo(newViewPoint.locations[0], creatorWrapper, newViewPoint.animation)
                    } else {
                        flyToRefObject(newViewPoint.locations)
                    }

                    onFinishedFlying()
                }
                is ViewPoint.FlyingToCameraPosition -> {
                    NavigateWrapper.flyTo(newViewPoint.cameraPosition, creatorWrapper, newViewPoint.animation)
                    onFinishedFlying()
                }
            }

            flyToTrackingFunction = getFlyToTrackingFunction(newViewPoint)
            bBoxEnforcement = true
        }
    }

    ListenersWrapper.addOnFrameListener {
        val currentLocation = NavigateWrapper.getLocation()
        val isCurrentLocationValid = BBox.isLocationValid(currentLocation)
        if (isCurrentLocationValid) lastValidCameraPosition = NavigateWrapper.getPosition()

        scope.launch {
            if (isCurrentLocationValid) {
                flyToTrackingFunction?.invoke()
            } else if (bBoxEnforcement) {
                lastValidCameraPosition?.let { NavigateWrapper.flyTo(it, FlyingAnimation.JUMP) }
                onPositionInvalid()
            }
        }
    }
}
