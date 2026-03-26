package com.even.map.terraexplorer.wrappers

import com.even.map.terraexplorer.constants.toWPT
import com.even.map.terraexplorer.sgWorldInstance
import com.even.map.terraexplorer.toLocation
import com.even.map.types.FilterObjectType
import com.even.map.types.Location
import com.skyline.teapi81.IPosition
import com.skyline.teapi81.ITerraExplorerObject
import com.skyline.teapi81.ITerraExplorerObjects
import com.skyline.teapi81.TEIUnknownHandle
import com.skyline.teapi81.WorldPointType

private val window
    get() = sgWorldInstance.window

private class TerraExplorerObjectsIterator(
    private val objects: ITerraExplorerObjects,
) : Iterator<ITerraExplorerObject> {
    private var index = 0

    override fun hasNext(): Boolean = getCurrentObject() != null

    override fun next(): ITerraExplorerObject {
        val currentObject = getCurrentObject() ?: throw NoSuchElementException("No such element at index $index")
        index++
        return currentObject
    }

    private fun getCurrentObject(): ITerraExplorerObject? =
        (objects.get_Item(index) as TEIUnknownHandle?)?.CastTo(ITerraExplorerObject::class.java)
}

internal class TerraExplorerObjectsIterable(
    private val objects: ITerraExplorerObjects,
) : Iterable<ITerraExplorerObject> {
    override fun iterator(): Iterator<ITerraExplorerObject> = TerraExplorerObjectsIterator(objects)
}

internal object WindowWrapper {
    fun pixelToObjects(screenX: Int, screenY: Int, filter: FilterObjectType) = ThreadWrapper.launchSync {
        TerraExplorerObjectsIterable(window.PixelToObjects(screenX, screenY, filter.toWPT()))
    }

    fun pixelToLocation(screenX: Int, screenY: Int): Location? = ThreadWrapper.launchSync {
        window.PixelToWorld(screenX, screenY)?.takeIf { it.type != WorldPointType.WPT_SKY }?.position?.toLocation()
    }

    fun centerPixelPosition(): IPosition? = ThreadWrapper.launchSync {
        window.CenterPixelToWorld().takeIf { it.type != WorldPointType.WPT_SKY }?.position
    }

    fun centerPixelToLocation(): Location? = ThreadWrapper.launchSync {
        centerPixelPosition()?.toLocation()
    }

    fun pixelFromCenterToDistance(pxHeight: Int): Double? = ThreadWrapper.launchSync {
        val centerPosition = window.CenterPixelToWorld().takeIf { it.type != WorldPointType.WPT_SKY }?.position
        val p = window.PixelFromWorld(centerPosition)
        val centerPx = p.x to p.y
        val centerPxWithHeight = centerPx.copy(second = centerPx.second + pxHeight)
        val centerWithHeightPosition = window.PixelToWorld(
            centerPxWithHeight.first.toInt(),
            centerPxWithHeight.second.toInt(),
        )?.takeIf { it.type != WorldPointType.WPT_SKY }?.position
        if (centerPosition != null && centerWithHeightPosition != null) {
            NavigateWrapper.getDistance(centerPosition, centerWithHeightPosition)
        } else null
    }

    fun getFingerLocation(): Pair<Location, TerraExplorerObjectsIterable> = ThreadWrapper.launchSync {
        val m = window.GetMouseInfo()
        val p = window.PixelToWorld(m.x, m.y)
        p.position.toLocation() to pixelToObjects(m.x, m.y, FilterObjectType.LABEL)
    }
}
