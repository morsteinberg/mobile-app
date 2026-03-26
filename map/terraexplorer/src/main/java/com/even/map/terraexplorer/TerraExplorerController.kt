package com.even.map.terraexplorer

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.even.core.coroutines.DispatchersProvider
import com.even.core.file.DeviceStorageAssetManager
import com.even.core.logger.Logger
import com.even.map.MapController
import com.even.map.orientation.Orientation
import com.even.map.providers.ElementsMapper
import com.even.map.terraexplorer.wrappers.CommandWrapper
import com.even.map.terraexplorer.wrappers.CreatorWrapper
import com.even.map.terraexplorer.wrappers.ListenersWrapper
import com.even.map.terraexplorer.wrappers.NavigateWrapper
import com.even.map.terraexplorer.wrappers.ProjectTreeWrapper
import com.even.map.terraexplorer.wrappers.WindowWrapper
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
import com.skyline.teapi81.ISGWorld
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt
import com.even.map.terraexplorer.functions.addSelfLocationIndicator as internalAddSelfLocationIndicator
import com.even.map.terraexplorer.functions.addViewPointController as internalAddViewPointController
import com.even.map.terraexplorer.functions.initMapView as internalInitMapView
import com.even.map.terraexplorer.functions.trackElements as internalTrackElements
import com.even.map.terraexplorer.functions.trackElevationLayer as internalTrackElevationLayer
import com.even.map.terraexplorer.functions.trackFeatureLayers as internalTrackFeatureLayers
import com.even.map.terraexplorer.functions.trackRasterLayers as internalTrackRasterLayers
import com.even.map.terraexplorer.functions.trackTempElement as internalTrackTempElement

internal const val predefinedLayersGroupName = "predefinedLayers"

internal val sgWorldInstance
    get() = ISGWorld.getInstance()

class TerraExplorerController(
    activity: AppCompatActivity,
    private val logger: Logger,
    private val deviceStorageAssetManager: DeviceStorageAssetManager,
    private val dispatchers: DispatchersProvider,
) : MapController {
    private val iconProvider = IconProvider(activity)
    private val gesturesProvider = GesturesProvider(activity)

    private val creatorWrapper = CreatorWrapper(iconProvider)

    override fun initMapView(activity: Activity, onCreated: () -> Unit): View =
        internalInitMapView(logger, deviceStorageAssetManager, dispatchers, activity, onCreated)

    override fun addSelfLocationIndicator(
        scope: CoroutineScope,
        location: Flow<Location>,
        availability: Flow<Boolean>,
        orientation: Flow<Orientation>,
    ) = internalAddSelfLocationIndicator(scope, creatorWrapper, location, availability, orientation)

    override fun addViewPointController(
        scope: CoroutineScope,
        elementsMapper: ElementsMapper,
        viewPointState: Flow<ViewPoint>,
        selfLocation: SelfLocationController,
        onFinishedFlying: () -> Unit,
        onPositionInvalid: () -> Unit,
    ) = internalAddViewPointController(
        scope,
        elementsMapper,
        viewPointState,
        selfLocation,
        creatorWrapper,
        onFinishedFlying,
        onPositionInvalid,
    )

    override fun resetRotation() = CommandWrapper.north()

    override suspend fun trackElevationLayer(state: Flow<ElevationLayer>) =
        internalTrackElevationLayer(state, creatorWrapper, logger)

    override suspend fun trackRasterLayers(state: Flow<List<RasterLayer>>) =
        internalTrackRasterLayers(state, creatorWrapper, logger)

    override suspend fun trackElements(
        state: Flow<List<Element>>,
        elementsMapper: ElementsMapper,
    ) =
        internalTrackElements(state, creatorWrapper, elementsMapper, logger)

    override suspend fun trackTempElement(
        state: Flow<Element.Temp?>,
        elementsMapper: ElementsMapper,
    ) = internalTrackTempElement(state, creatorWrapper, elementsMapper, iconProvider, logger)

    override suspend fun addMeshModel(id: String, serverPath: String): MeshModel? {
        logger.i("Loading 3D model", mapOf("id" to id))
        return try {
            creatorWrapper.createMeshModel(id, serverPath)
        } catch (err: Exception) {
            logger.e("Error ${err.message}")
            null
        }
    }

    override suspend fun removeMeshModel(meshModel: MeshModel) {
        logger.i("Deleting model ${meshModel.id}!")
        ProjectTreeWrapper.deleteItems(meshModel.externalReferenceIds + meshModel.flyToId)
    }

    override suspend fun trackFeatureLayers(
        state: Flow<List<Layer>>,
        elementsMapper: ElementsMapper,
    ) = internalTrackFeatureLayers(state, elementsMapper, creatorWrapper, iconProvider, logger)

    override fun addOnMapClickedDownListener(callback: () -> Unit) =
        ListenersWrapper.addOnLButtonDownListener(callback)

    override fun addOnMapHoldDownListener(
        elementsMapper: ElementsMapper?,
        filterObjectsType: FilterObjectType,
        callback: (location: Location, selectedElementId: String?, fingerRelease: CompletableDeferred<Unit>) -> Unit,
    ): Listener =
        ListenersWrapper.addOnRButtonDownListener(filterObjectsType, elementsMapper) { location, elementId ->
            location?.let {
                gesturesProvider.vibrate(GesturesProvider.Duration.DEFAULT)

                val deferred = CompletableDeferred<Unit>()
                ListenersWrapper.addOnLButtonUpListenerOnce { deferred.complete(Unit) }

                callback(location, elementId.firstOrNull(), deferred)
            }
            false
        }

    override fun addRotationDegreesListener(callback: (Int) -> Unit): Listener =
        ListenersWrapper.addOnFrameListener {
            callback(NavigateWrapper.getPosition().yaw.toRotation())
        }

    override fun addOnMapClickedListener(
        elementsMapper: ElementsMapper,
        callback: (location: Location?, selectedElementId: String?) -> Unit,
    ): Listener =
        ListenersWrapper.addOnLButtonClickedListener(elementsMapper) { location, elementIds ->
            callback(location, elementIds.firstOrNull())
            false
        }

    override fun addOnMapClickedListener(
        callback: () -> Unit,
    ): Listener =
        ListenersWrapper.addOnLButtonClickedListener(callback)

    override fun addRenderQualityListener(callback: (Int) -> Unit): Listener =
        ListenersWrapper.addOnRenderQualityChangedListener(callback)

    override fun addOnMapClickedAndDragListener(callback: () -> Unit): Listener =
        ListenersWrapper.addOnLButtonDownListenerOnce(callback)

    override fun getCameraPosition() = NavigateWrapper.getCameraPosition()

    override fun getCenterLocation(): Location? =
        WindowWrapper.centerPixelToLocation()

    override fun distanceToCenter(): Double? {
        val centerPosition = WindowWrapper.centerPixelPosition()

        return centerPosition?.let { NavigateWrapper.getDistance(NavigateWrapper.getPosition(), it) }
    }

    override fun pixelFromCenterToDistance(px: Int): Double? = WindowWrapper.pixelFromCenterToDistance(px)


    override fun addOnMapDragListener(
        elementsMapper: ElementsMapper,
        filterObjectsType: FilterObjectType,
        callback: (location: Location?, selectedElementId: String?) -> Unit,
    ): Listener {
        var isEventHandled: Boolean
        var fingerLocationListener: Listener? = null
        var leaveListener: Listener? = null
        val dragListener = ListenersWrapper.addOnLButtonDownListener(elementsMapper) { _, elementId ->
            if (elementId.isNotEmpty()) {
                var draggedElement = elementId.first()
                fingerLocationListener =
                    ListenersWrapper.addFingerLocationListener(elementsMapper) { location, altElementIds ->
                        val isOriginalElementAvailable =
                            elementsMapper.getExternalIdsByElementId(draggedElement).isNotEmpty()
                        if (isOriginalElementAvailable.not()) altElementIds.firstOrNull()?.let { draggedElement = it }

                        callback(location, draggedElement)
                    }
                leaveListener = ListenersWrapper.addOnLButtonUpListenerOnce {
                    fingerLocationListener.remove()
                }
                isEventHandled = true
            } else isEventHandled = false
            isEventHandled
        }
        return Listener { listOf(dragListener, fingerLocationListener, leaveListener).forEach { it?.remove() } }
    }

    override val yaw: Double
        get() = NavigateWrapper.getPositionYaw()
}

private fun Double.toRotation() = this.roundToInt() % 360
