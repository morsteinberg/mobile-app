package com.even.map

import android.app.Activity
import android.view.View
import com.even.map.orientation.Orientation
import com.even.map.providers.ElementsMapper
import com.even.map.types.CameraPosition
import com.even.map.types.Element
import com.even.map.types.FilterObjectType
import com.even.map.types.Layer
import com.even.map.types.Layer.ElevationLayer
import com.even.map.types.Layer.RasterLayer
import com.even.map.types.Listener
import com.even.map.types.Location
import com.even.map.types.MeshModel
import com.even.map.types.SelfLocationController
import com.even.map.types.ViewPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface MapController {
    fun initMapView(activity: Activity, onCreated: () -> Unit = {}): View

    fun addSelfLocationIndicator(
        scope: CoroutineScope,
        location: Flow<Location>,
        availability: Flow<Boolean>,
        orientation: Flow<Orientation>,
    ): SelfLocationController

    fun addViewPointController(
        scope: CoroutineScope,
        elementsMapper: ElementsMapper,
        viewPointState: Flow<ViewPoint>,
        selfLocation: SelfLocationController,
        onFinishedFlying: () -> Unit,
        onPositionInvalid: () -> Unit,
    )

    fun resetRotation()

    suspend fun trackElevationLayer(state: Flow<ElevationLayer>)
    suspend fun trackRasterLayers(state: Flow<List<RasterLayer>>)
    suspend fun trackFeatureLayers(state: Flow<List<Layer>>, elementsMapper: ElementsMapper)
    suspend fun trackElements(state: Flow<List<Element>>, elementsMapper: ElementsMapper)
    suspend fun trackTempElement(state: Flow<Element.Temp?>, elementsMapper: ElementsMapper)

    suspend fun addMeshModel(id: String, serverPath: String): MeshModel?
    suspend fun removeMeshModel(meshModel: MeshModel)

    fun addOnMapClickedListener(callback: () -> Unit): Listener
    fun addOnMapClickedListener(
        elementsMapper: ElementsMapper,
        callback: (location: Location?, selectedElementId: String?) -> Unit,
    ): Listener

    fun addOnMapClickedDownListener(callback: () -> Unit): Listener
    fun addOnMapHoldDownListener(
        elementsMapper: ElementsMapper? = null,
        filterObjectsType: FilterObjectType = FilterObjectType.LABEL,
        callback: (location: Location, elementId: String?, fingerRelease: CompletableDeferred<Unit>) -> Unit,
    ): Listener

    fun addRenderQualityListener(callback: (Int) -> Unit): Listener
    fun addRotationDegreesListener(callback: (Int) -> Unit): Listener
    fun addOnMapClickedAndDragListener(callback: () -> Unit): Listener
    fun getCenterLocation(): Location?
    fun distanceToCenter(): Double?
    fun pixelFromCenterToDistance(px: Int): Double?
    fun addOnMapDragListener(
        elementsMapper: ElementsMapper,
        filterObjectsType: FilterObjectType = FilterObjectType.LABEL,
        callback: (location: Location?, selectedElementId: String?) -> Unit,
    ): Listener

    val yaw: Double

    fun getCameraPosition(): CameraPosition

    companion object {
        const val TAG = "MapController"
    }
}
