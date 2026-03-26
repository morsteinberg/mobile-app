package com.even.map.terraexplorer.functions

import android.graphics.Color
import com.even.map.orientation.Orientation
import com.even.map.terraexplorer.R
import com.even.map.terraexplorer.wrappers.CreatorWrapper
import com.even.map.terraexplorer.wrappers.NavigateWrapper
import com.even.map.terraexplorer.wrappers.TerrainWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.even.map.types.FlyingAnimation
import com.even.map.types.Location
import com.even.map.types.SelfLocationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

const val CENTERED_ON_USER_VIEWING_DISTANCE = 1000
const val HEADING_VIEWING_DISTANCE = 250

internal fun addSelfLocationIndicator(
    scope: CoroutineScope,
    creatorWrapper: CreatorWrapper,
    location: Flow<Location>,
    availability: Flow<Boolean>,
    orientation: Flow<Orientation>,
): SelfLocationController {
    val accuracyCircle = creatorWrapper.createCircle(
        radius = 0.0,
        lineColor = Color.argb(255, 41, 107, 194),
        fillColor = Color.argb(70, 144, 184, 235),
        numberOfSegments = 100,
    )
    val headingBeamIcon = creatorWrapper.createLabel(R.drawable.self_location_heading_beam)
    val activeLocationIcon = creatorWrapper.createLabel(R.drawable.self_location_active)
    val inactiveLocationIcon = creatorWrapper.createLabel(R.drawable.self_location_inactive)

    scope.launch {
        launch {
            location.collect {
                ThreadWrapper.launchLazy {
                    val newPosition = TerrainWrapper.getPositionWithHeightMargin(it)
                    val headingBeamPosition = newPosition.Copy().apply {
                        yaw = headingBeamIcon.position.yaw
                        distance = HEADING_VIEWING_DISTANCE.toDouble()
                    }
                    val locationIconPosition = newPosition.Copy().apply {
                        distance = CENTERED_ON_USER_VIEWING_DISTANCE.toDouble()
                    }

                    accuracyCircle.apply {
                        position = newPosition
                        radius = it.accuracy.toDouble()
                    }
                    headingBeamIcon.position = headingBeamPosition
                    activeLocationIcon.position = locationIconPosition
                    inactiveLocationIcon.position = locationIconPosition
                }
            }
        }
        launch {
            availability.collect {
                ThreadWrapper.launchLazy {
                    activeLocationIcon.visibility.show = it
                    inactiveLocationIcon.visibility.show = !it
                    accuracyCircle.visibility.show = it
                }
            }
        }
        launch {
            orientation.collect {
                ThreadWrapper.launchLazy {
                    headingBeamIcon.position.yaw = it.heading.toDouble()
                }
            }
        }
    }

    return object : SelfLocationController {
        override suspend fun flyToLocationIndicator(animation: FlyingAnimation) =
            NavigateWrapper.flyTo(activeLocationIcon, animation)

        override suspend fun rotateToHeadingBeam(animation: FlyingAnimation) =
            NavigateWrapper.flyTo(headingBeamIcon, animation)
    }
}
