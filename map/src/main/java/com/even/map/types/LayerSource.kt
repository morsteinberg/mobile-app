package com.even.map.types

import kotlinx.serialization.Serializable

@Serializable
sealed interface LayerSource {
    data class Local(val filePath: String) : LayerSource {
        override fun toString(): String = "local"
    }

    data class Remote(val wmtsHostName: String, val fileName: String) : LayerSource {
        override fun toString(): String = "remote"
    }
}
