package com.even.map.terraexplorer.wrappers

import com.even.map.providers.ElementsMapper
import com.even.map.terraexplorer.sgWorldInstance
import com.even.map.types.FilterObjectType
import com.even.map.types.Listener
import com.even.map.types.Location
import com.skyline.teapi81.ISGWorld

internal object ListenersWrapper {
    fun addOnFrameListener(callback: () -> Unit): Listener {
        val listener = ISGWorld.OnFrameListener(callback)
        ThreadWrapper.launchSync { sgWorldInstance.addOnFrameListener(listener) }

        // Warning: Removing listeners on a non-async render thread will freeze the app on restart!
        return Listener { ThreadWrapper.launchLazy { sgWorldInstance.removeOnFrameListener(listener) } }
    }

    fun addOnLButtonDownListener(callback: () -> Unit) = addOnLButtonDownListener(null) { _, _ -> callback(); false }

    fun addOnLButtonDownListener(
        elementsMapper: ElementsMapper?,
        callback: (Location?, List<String>) -> Boolean,
    ): Listener {
        val listener = ISGWorld.OnLButtonDownListener(enrichRawLocationData(elementsMapper, callback))
        ThreadWrapper.launchSync { sgWorldInstance.addOnLButtonDownListener(listener) }

        return Listener { ThreadWrapper.launchLazy { sgWorldInstance.removeOnLButtonDownListener(listener) } }
    }

    fun addOnLButtonClickedListener(callback: () -> Unit) =
        addOnLButtonClickedListener(null) { _, _ -> callback(); false }

    fun addOnLButtonClickedListener(
        elementsMapper: ElementsMapper?,
        callback: (Location?, List<String>) -> Boolean,
    ): Listener {
        val listener = ISGWorld.OnLButtonClickedListener(enrichRawLocationData(elementsMapper, callback))
        ThreadWrapper.launchSync { sgWorldInstance.addOnLButtonClickedListener(listener) }

        return Listener { ThreadWrapper.launchLazy { sgWorldInstance.removeOnLButtonClickedListener(listener) } }
    }

    fun addOnRButtonDownListener(
        filterObjectType: FilterObjectType = FilterObjectType.LABEL,
        elementsMapper: ElementsMapper? = null,
        callback: (Location?, List<String>) -> Boolean,
    ): Listener {
        val listener = ISGWorld.OnRButtonDownListener(enrichRawLocationData(elementsMapper, callback, filterObjectType))
        ThreadWrapper.launchSync { sgWorldInstance.addOnRButtonDownListener(listener) }

        return Listener { ThreadWrapper.launchLazy { sgWorldInstance.removeOnRButtonDownListener(listener) } }
    }

    fun addOnLButtonUpListenerOnce(callback: () -> Unit): Listener =
        addOnLButtonUpListenerOnce(null) { _, _ -> callback(); false }

    fun addOnLButtonUpListenerOnce(
        elementsMapper: ElementsMapper?,
        callback: (Location?, List<String>) -> Boolean,
    ): Listener {
        val listener = object : ISGWorld.OnLButtonUpListener {
            override fun OnLButtonUp(p0: Int, screenX: Int, screenY: Int): Boolean {
                ThreadWrapper.launchSync { sgWorldInstance.removeOnLButtonUpListener(this) }
                return enrichRawLocationData(elementsMapper, callback)(p0, screenX, screenY)
            }
        }
        ThreadWrapper.launchSync { sgWorldInstance.addOnLButtonUpListener(listener) }

        return Listener { ThreadWrapper.launchLazy { sgWorldInstance.removeOnLButtonUpListener(listener) } }
    }


    fun addOnRenderQualityChangedListener(callback: (Int) -> Unit): Listener {
        val listener = ISGWorld.OnRenderQualityChangedListener(callback)
        ThreadWrapper.launchSync { sgWorldInstance.addOnRenderQualityChangedListener(listener) }

        return Listener { ThreadWrapper.launchLazy { sgWorldInstance.removeOnRenderQualityChangedListener(listener) } }
    }

    fun addOnLButtonDownListenerOnce(callback: () -> Unit): Listener =
        addOnLButtonDownListenerOnce(null) { _, _ -> callback(); false }


    fun addOnLButtonDownListenerOnce(
        elementsMapper: ElementsMapper?,
        callback: (Location?, List<String>) -> Boolean,
    ): Listener {
        val listener = object : ISGWorld.OnLButtonDownListener {
            override fun OnLButtonDown(p0: Int, screenX: Int, screenY: Int): Boolean {
                ThreadWrapper.launchSync { sgWorldInstance.removeOnLButtonDownListener(this) }
                return enrichRawLocationData(elementsMapper, callback)(p0, screenX, screenY)
            }
        }
        ThreadWrapper.launchSync { sgWorldInstance.addOnLButtonDownListener(listener) }

        return Listener { ThreadWrapper.launchLazy { sgWorldInstance.removeOnLButtonDownListener(listener) } }
    }

    fun addFingerLocationListener(
        elementsMapper: ElementsMapper? = null,
        callback: (Location?, List<String>) -> Unit,
    ): Listener =
        addOnFrameListener {
            val (location, objects) = WindowWrapper.getFingerLocation()
            callback(location, objects.mapNotNull { elementsMapper?.getElementIdByExternalId(it.id) })
        }

    private fun enrichRawLocationData(
        elementsMapper: ElementsMapper? = null,
        callback: (Location?, List<String>) -> Boolean,
        filterObjectType: FilterObjectType = FilterObjectType.LABEL,
    ) = { _: Int, screenX: Int, screenY: Int ->
        val selectedLocation = WindowWrapper.pixelToLocation(screenX, screenY)
        val selectedElements = WindowWrapper.pixelToObjects(screenX, screenY, filterObjectType)
            .mapNotNull { elementsMapper?.getElementIdByExternalId(it.id) }
        callback(selectedLocation, selectedElements)
    }
}
