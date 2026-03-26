package com.even.map.terraexplorer.wrappers

import com.even.map.terraexplorer.constants.ObjectType
import com.even.map.terraexplorer.sgWorldInstance
import com.skyline.teapi81.ApiException
import com.skyline.teapi81.IFeatureLayer
import com.skyline.teapi81.ITerraExplorerObject
import com.skyline.teapi81.ITerrainArrow
import com.skyline.teapi81.ITerrainImageLabel
import com.skyline.teapi81.ITerrainPolygon
import com.skyline.teapi81.ITerrainPolyline
import com.skyline.teapi81.TEIUnknownHandle
import java.io.File

private val projectTree
    get() = sgWorldInstance.projectTree

internal object ProjectTreeWrapper {
    val hiddenGroupId: String
        get() = ThreadWrapper.launchSync { projectTree.rootID }

    fun deleteItems(ids: List<String>) = ThreadWrapper.launchSync {
        ids.toSet().forEach { projectTree.DeleteItem(it) }
    }

    suspend fun getLayer(inProjectPath: String): IFeatureLayer? = ThreadWrapper.launch {
        try {
            projectTree.GetLayer(projectTree.FindItem(inProjectPath))
        } catch (_: ApiException) {
            null
        }
    }


    fun getElement(id: String): ITerraExplorerObject = ThreadWrapper.launchSync {
        projectTree.GetObject(id)
    }

    fun getAndResolveObject(id: String): TEIUnknownHandle? = ThreadWrapper.launchSync {
        val terraObject = getElement(id)

        when (terraObject.objectType) {
            ObjectType.POLYLINE.value -> terraObject.CastTo(ITerrainPolyline::class.java)
            ObjectType.POLYGON.value -> terraObject.CastTo(ITerrainPolygon::class.java)
            ObjectType.ARROW.value -> terraObject.CastTo(ITerrainArrow::class.java)
            ObjectType.IMAGE_LABEL.value -> terraObject.CastTo(ITerrainImageLabel::class.java)
            else -> null
        }
    }

    fun hideRootId() =
        ThreadWrapper.launchSync { projectTree.SetVisibility(projectTree.rootID, false) }

    fun loadFlyFile(file: File): Unit = ThreadWrapper.launchSync { projectTree.LoadFlyLayer(file.toString()) }
}
