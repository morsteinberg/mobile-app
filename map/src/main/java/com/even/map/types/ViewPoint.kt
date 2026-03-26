package com.even.map.types

sealed interface ViewPoint {
    data object FreeRoam : ViewPoint
    data object Heading : ViewPoint
    data object CenteredOnUser : ViewPoint
    data class FlyingToObjectId(val objectId: String) : ViewPoint
    data class CenteredOnElement(val element: Element) : ViewPoint
    data class FlyingToLocation(
        val locations: List<Location>,
        val animation: FlyingAnimation = FlyingAnimation.DEFAULT,
    ) : ViewPoint {
        constructor(
            vararg locations: Location, animation: FlyingAnimation = FlyingAnimation.DEFAULT,
        ) : this(locations.toList(), animation)
    }

    data class FlyingToCameraPosition(
        val cameraPosition: CameraPosition,
        val animation: FlyingAnimation = FlyingAnimation.DEFAULT,
    ) : ViewPoint
}
