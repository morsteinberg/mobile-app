package com.even.map.terraexplorer.wrappers

import com.even.map.terraexplorer.sgWorldInstance
import com.even.map.types.CameraPosition
import com.even.map.types.FlyingAnimation
import com.even.map.types.Location
import com.skyline.teapi81.AltitudeTypeCode
import com.skyline.teapi81.IPosition
import com.skyline.teapi81.ISGWorld
import com.skyline.teapi81.ITerrainImageLabel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val navigate
    get() = sgWorldInstance.navigate

internal object NavigateWrapper {
    fun getLocation(): Location = ThreadWrapper.launchSync {
        navigate.GetPosition().run {
            this.ChangeAltitudeType(AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE)
            Location(y, x, altitude)
        }
    }

    fun getPosition(): IPosition = ThreadWrapper.launchSync {
        navigate.GetPosition()
            .apply { ChangeAltitudeType(AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE) }
    }

    fun getCameraPosition() = ThreadWrapper.launchSync {
        with(
            navigate.GetPosition()
                .apply { ChangeAltitudeType(AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE) },
        ) {
            CameraPosition(
                latitude = y,
                longitude = x,
                altitude = altitude,
                yaw = yaw,
                pitch = pitch,
                roll = roll,
            )
        }
    }

    fun getPositionYaw(): Double = ThreadWrapper.launchSync { navigate.GetPosition().yaw }

    fun getDistance(pos1: IPosition, pos2: IPosition): Double = ThreadWrapper.launchSync { pos1.DistanceTo(pos2) }

    suspend fun flyTo(imageLabel: ITerrainImageLabel, animation: FlyingAnimation = FlyingAnimation.DEFAULT) =
        executeFlyTo(imageLabel, animation.toFlyToPattern())

    suspend fun flyTo(objectId: String, animation: FlyingAnimation = FlyingAnimation.DEFAULT) =
        executeFlyTo(objectId, animation.toFlyToPattern())

    suspend fun flyTo(
        location: Location,
        creatorWrapper: CreatorWrapper,
        animation: FlyingAnimation = FlyingAnimation.DEFAULT,
    ) = flyTo(creatorWrapper.createPosition(location), animation)

    suspend fun flyTo(position: IPosition, animation: FlyingAnimation = FlyingAnimation.DEFAULT) =
        executeFlyTo(position, animation.toFlyToPattern())

    suspend fun flyTo(
        cameraPosition: CameraPosition,
        creatorWrapper: CreatorWrapper,
        animation: FlyingAnimation = FlyingAnimation.DEFAULT,
    ) = flyTo(creatorWrapper.createPosition(cameraPosition), animation)

    private suspend fun executeFlyTo(target: Any, pattern: FlyToPattern) = suspendCoroutine { continuation ->
        ThreadWrapper.launchLazy {
            navigate.FlyTo(target, pattern.code)

            var lastCameraPosition = navigate.GetPosition()
            sgWorldInstance.addOnFrameListener(
                object : ISGWorld.OnFrameListener {
                    override fun OnFrame() {
                        if (navigate.GetPosition().IsEqual(lastCameraPosition)) {
                            continuation.resume(Unit)
                            sgWorldInstance.removeOnFrameListener(this)
                        } else {
                            lastCameraPosition = navigate.GetPosition()
                        }
                    }
                },
            )
        }
    }
}

private fun FlyingAnimation.toFlyToPattern() = when (this) {
    FlyingAnimation.DEFAULT -> FlyToPattern.FLY_TO
    FlyingAnimation.ABOVE -> FlyToPattern.FOLLOW_ABOVE
    FlyingAnimation.BEHIND_AND_ABOVE -> FlyToPattern.FOLLOW_BEHIND_AND_ABOVE
    FlyingAnimation.JUMP -> FlyToPattern.JUMP
}

private enum class FlyToPattern(val code: Int) {
    FLY_TO(0),
    CIRCLE_PATTERN(1),
    OVAL_PATTERN(2),
    LINE_PATTERN(3),
    ARC_PATTERN(4),
    FOLLOW_BEHIND(5),
    FOLLOW_ABOVE(6),
    FOLLOW_BELOW(7),
    FOLLOW_RIGHT(8),
    FOLLOW_LEFT(9),
    FOLLOW_BEHIND_AND_ABOVE(10),
    FOLLOW_COCKPIT(11),
    FOLLOW_FROM_GROUND(12),
    STOP(13),
    JUMP(14),
    PLAY(18),
}
