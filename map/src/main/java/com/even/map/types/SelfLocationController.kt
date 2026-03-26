package com.even.map.types

interface SelfLocationController {
    suspend fun flyToLocationIndicator(animation: FlyingAnimation = FlyingAnimation.DEFAULT)
    suspend fun rotateToHeadingBeam(animation: FlyingAnimation = FlyingAnimation.DEFAULT)
}
