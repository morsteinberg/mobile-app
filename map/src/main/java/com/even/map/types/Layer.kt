package com.even.map.types

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.even.core.serialization.DpSerializer
import com.even.core.serialization.FileSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.reflect.KClass

@Serializable
sealed interface Layer {
    val id: String
    val isVisible: Boolean

    @Serializable
    data class ElevationLayer(
        override val id: String,
        val layerSource: LayerSource,
        override val isVisible: Boolean = true,
    ) : Layer

    @Serializable
    data class RasterLayer(
        override val id: String,
        val layerSource: LayerSource,
        override val isVisible: Boolean = false,
    ) : Layer

    @Serializable
    data class FeatureLayer<T : Element>(
        override val id: String,
        override val isVisible: Boolean = true,
        val elements: List<T> = emptyList(),
        @Contextual
        val elementType: KClass<T>,
        val layerProperties: LayerProperties = LayerProperties(),
    ) : Layer {
        @Serializable
        data class LayerProperties(
            @Serializable(with = DpSerializer::class)
            val maxSize: Dp = 36.dp,
            val anchorPosition: AnchorPosition = AnchorPosition.BOTTOM_CENTER,
            val hasAnnotation: Boolean = true,
            val smallestVisibleSize: Int = 7,
        )
    }

    @Serializable
    data class PredefinedFeatureLayer(
        override val id: String,
        override val isVisible: Boolean = true,
        @Serializable(with = FileSerializer::class)
        val filePath: File,
    ) : Layer
}
