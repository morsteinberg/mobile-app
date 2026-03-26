package com.even.map.terraexplorer.wrappers

import android.content.Context
import com.even.core.extensions.StringExtensions.withFileExtension
import com.even.core.types.FileType.TERRAEXPLORER_TERRAIN
import com.even.map.terraexplorer.sgWorldInstance

private val project
    get() = sgWorldInstance.project

private val GLOBUS_FILE_STORAGE_PATH =
    "/data/terraexplorer/resources/Resources/Default_Local_Terrain".withFileExtension(TERRAEXPLORER_TERRAIN)

internal class ProjectWrapper(context: Context) {
    private val url: String = getURL(context)

    internal fun open() = project.Open(url)

    private fun getURL(context: Context): String {
        return context.getExternalFilesDir(null)?.absolutePath + GLOBUS_FILE_STORAGE_PATH
    }
}
