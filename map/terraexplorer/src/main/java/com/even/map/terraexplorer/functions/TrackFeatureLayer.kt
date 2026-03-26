package com.even.map.terraexplorer.functions

import androidx.compose.ui.unit.Dp
import com.even.core.extensions.ListExtensions.Helpers.ItemsDiff
import com.even.core.extensions.ListExtensions.diffByKey
import com.even.core.logger.Logger
import com.even.map.providers.ElementsMapper
import com.even.map.terraexplorer.IconProvider
import com.even.map.terraexplorer.attributes
import com.even.map.terraexplorer.constants.Attribute
import com.even.map.terraexplorer.constants.PivotAlignment
import com.even.map.terraexplorer.constants.Property
import com.even.map.terraexplorer.constants.toPivotAlignment
import com.even.map.terraexplorer.setProperty
import com.even.map.terraexplorer.setPropertyByFeatureAttribute
import com.even.map.terraexplorer.updateVisibility
import com.even.map.terraexplorer.wrappers.CreatorWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.even.map.types.Element
import com.even.map.types.Element.Point
import com.even.map.types.Layer
import com.even.map.types.Layer.FeatureLayer
import com.even.map.types.Layer.PredefinedFeatureLayer
import com.skyline.teapi81.IFeatureLayer
import com.skyline.teapi81.IPoint
import com.skyline.terraexplorer.models.UI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal suspend fun trackFeatureLayers(
    state: Flow<List<Layer>>,
    elementsMapper: ElementsMapper,
    creatorWrapper: CreatorWrapper,
    iconProvider: IconProvider,
    logger: Logger,
) {
    var prevState: List<Layer> = emptyList()
    val idsToLayers = mutableMapOf<String, IFeatureLayer?>()

    state.collect { state ->
        val changedLayers = state.filter { prevState.contains(it).not() }

        val changedFeatureLayers = changedLayers.filterIsInstance<FeatureLayer<*>>()
        val newFeatureLayers = changedFeatureLayers.filter { idsToLayers.containsKey(it.id).not() }
        newFeatureLayers.forEach { newLayer ->
            idsToLayers[newLayer.id] = setupFeatureLayer(
                creatorWrapper, newLayer.id, newLayer.elementType, newLayer.layerProperties,
            )

            logger.i(
                "Layer added successfully",
                mapOf("id" to newLayer.id, "type" to "feature", "source" to "local"),
            )
        }
        prevState = prevState + newFeatureLayers.map { it.copy(elements = emptyList()) }

        (changedFeatureLayers).forEach { featureLayer ->
            val prevFeatureLayer = prevState.find { it.id == featureLayer.id } as? FeatureLayer<*>
            if (prevFeatureLayer != null && prevFeatureLayer.elements != featureLayer.elements) {
                idsToLayers[featureLayer.id]?.let { TELayer ->
                    val diff = prevFeatureLayer.elements.diffByKey(
                        other = featureLayer.elements,
                        uniqueKeySelector = { it.id },
                    )

                    setElementsOnLayer(
                        TELayer,
                        elementsMapper,
                        diff,
                        creatorWrapper,
                        iconProvider,
                        logger,
                    )
                }
            }
        }

        val changedPredefinedFeatureLayers =
            changedLayers.filterIsInstance<PredefinedFeatureLayer>()
        val newPredefinedFeatureLayers =
            changedPredefinedFeatureLayers.filter { idsToLayers.containsKey(it.id).not() }
        newPredefinedFeatureLayers.forEach { newLayer ->
            idsToLayers[newLayer.id] = addPredefinedFeatureLayer(
                newLayer.id, newLayer.isVisible, newLayer.filePath, logger,
            )
        }

        changedLayers.forEach { featureLayer ->
            ThreadWrapper.launchLazy { idsToLayers[featureLayer.id]?.updateVisibility(featureLayer.isVisible) }
        }

        prevState = state
    }
}

private suspend fun <T : Element> setupFeatureLayer(
    creatorWrapper: CreatorWrapper,
    layerId: String,
    elementType: KClass<T>,
    layerProperties: FeatureLayer.LayerProperties,
): IFeatureLayer {
    val layer = creatorWrapper.createFeatureLayer(layerId)

    ThreadWrapper.launch {
        listOf(Attribute.LABEL, Attribute.ICON)
            .forEach { layer.attributes.CreateAttribute(it.name, it.type, Int.MAX_VALUE) }

        if (layerProperties.hasAnnotation) layer.annotation = true

        layer.Save()
        layer.Refresh()

        if (elementType.isSubclassOf(Point::class)) {

            layer.featureGroups.point.apply {
                setPropertyByFeatureAttribute(Property.ImageFile, Attribute.ICON)

                setProperty(Property.ImageMaxSize, layerProperties.maxSize.toTeMaxImageSize())
                setProperty(Property.ImageOpacity, 0.99)
                setProperty(
                    Property.PivotAlignment,
                    layerProperties.anchorPosition.toPivotAlignment().id,
                )
                setProperty(Property.Scale, 3)
                setProperty(Property.SmallestVisibleSize, layerProperties.smallestVisibleSize)
            }

            if (layerProperties.hasAnnotation) {
                layer.featureGroups.annotation.apply {
                    setPropertyByFeatureAttribute(Property.Text, Attribute.LABEL)

                    setProperty(Property.BackgroundColor, "000000")
                    setProperty(Property.Bold, true)
                    setProperty(Property.Font, "Segoe UI")
                    setProperty(Property.PivotAlignment, PivotAlignment.TOP_CENTER.id)
                    setProperty(Property.Scale, 3)
                    setProperty(Property.SmallestVisibleSize, 10)
                    setProperty(Property.TextSize, 12)
                }
            }
        }
    }

    return layer
}

private suspend fun <T : Element> setElementsOnLayer(
    layer: IFeatureLayer,
    elementsMapper: ElementsMapper,
    elementsDiff: ItemsDiff<T>,
    creatorWrapper: CreatorWrapper,
    iconProvider: IconProvider,
    logger: Logger,
) {
    val (elementsToAdd, elementsToUpdate, elementsToRemove) = elementsDiff
    addElementsOnLayer(layer, elementsMapper, elementsToAdd, creatorWrapper, iconProvider)
    updateElementsOnLayer(layer, elementsMapper, elementsToUpdate, iconProvider, logger)
    removeElementsOnLayer(layer, elementsMapper, elementsToRemove.map { it.id }, logger)
}

private suspend fun <T : Element> addElementsOnLayer(
    layer: IFeatureLayer,
    elementsMapper: ElementsMapper,
    elements: List<T>,
    creatorWrapper: CreatorWrapper,
    iconProvider: IconProvider,
) = coroutineScope {
    elements.map { element ->
        async {
            if (element is Point.IconPoint) {
                val createdElementId = creatorWrapper.createPointOnLayer(
                    layer,
                    element,
                    listOf(
                        formatAsAnnotationLabel(element.label),
                        iconProvider.getIconPath(element.iconId),
                    ),
                )

                elementsMapper.put(element, createdElementId)
            }
        }
    }.awaitAll()
}

private suspend fun <T : Element> updateElementsOnLayer(
    layer: IFeatureLayer,
    elementsMapper: ElementsMapper,
    elements: List<T>,
    iconProvider: IconProvider,
    logger: Logger,
) = ThreadWrapper.launch {
    elements.forEach { element ->
        elementsMapper.getExternalIdsByElement(element).firstOrNull()?.let { terraId ->
            if (element is Point.IconPoint) {
                val feature =
                    layer.featureGroups.point.GetCurrentFeatures().GetFeatureByObjectID(terraId)

                feature.geometry.CastTo(IPoint::class.java).let {
                    it.x = element.location.longitude
                    it.y = element.location.latitude
                }

                listOf(
                    Attribute.ICON to iconProvider.getIconPath(element.iconId),
                    Attribute.LABEL to formatAsAnnotationLabel(element.label),
                ).forEach {
                    feature.featureAttributes.GetFeatureAttribute(it.first.name).value =
                        Attribute.stringifyValues(listOf(it.second))
                }

                elementsMapper.put(element, terraId)
            }
        } ?: logger.e("Got update on an non-mapped element", mapOf("element" to element))
    }
}

private suspend fun removeElementsOnLayer(
    layer: IFeatureLayer,
    elementsMapper: ElementsMapper,
    elementIds: List<String>,
    logger: Logger,
) = ThreadWrapper.launch {
    elementIds.forEach { id ->
        elementsMapper.getExternalIdsByElementId(id).firstOrNull()?.let { terraId ->
            layer.featureGroups.point.RemoveFeature(terraId)
            elementsMapper.remove(id)
        } ?: logger.e("Got remove on an non-mapped element", mapOf("elementId" to id))
    }
}

private fun formatAsAnnotationLabel(label: String?) = label?.let { "\r\n$it" }

private const val DP_TO_TE_PIXEL_RATIO = 64
private fun Dp.toTeMaxImageSize() = UI.dp2px(this.value).toDouble() / DP_TO_TE_PIXEL_RATIO
