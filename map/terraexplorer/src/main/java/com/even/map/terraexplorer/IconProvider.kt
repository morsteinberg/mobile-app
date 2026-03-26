package com.even.map.terraexplorer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream

private const val ICONS_SUBFOLDER_NAME = "TE/Icons"
private const val NODE_SUBFOLDER_NAME = "Nodes"
private const val ICON_QUALITY = 100

internal class IconProvider(private val context: Context) {
    private val iconPathCacheMap = HashMap<Int, String>()
    private val nodeIconPathCacheMap = HashMap<Int, String>()

    private val iconsFolder = "${context.filesDir}/$ICONS_SUBFOLDER_NAME"
    private val nodeIconsFolder = "$iconsFolder/$NODE_SUBFOLDER_NAME"

    fun getIconPath(@DrawableRes resId: Int): String? {
        val currentIconPath = iconPathCacheMap[resId]

        if (currentIconPath == null) {
            val iconBitmap = ResourcesCompat.getDrawable(
                context.applicationContext.resources, resId, null,
            )?.toBitmap() ?: return null

            saveAssetFileToCache(iconsFolder, resId, iconBitmap, iconPathCacheMap)
        }

        return iconPathCacheMap[resId]
    }

    fun getColoredNodePath(color: Int): String? {
        val currentIconPath = nodeIconPathCacheMap[color]

        if (currentIconPath == null) {
            val drawable = ResourcesCompat.getDrawable(
                context.applicationContext.resources, R.drawable.sketch_vertex_node, null,
            ) ?: return null
            val mutableDrawable = colorLayerInDrawable(drawable, R.id.inner_circle_fill, color)

            saveAssetFileToCache(nodeIconsFolder, color, mutableDrawable.toBitmap(), nodeIconPathCacheMap)
        }

        return nodeIconPathCacheMap[color]
    }

    private fun saveAssetFileToCache(
        folderPath: String,
        fileId: Int,
        bitmap: Bitmap,
        filePathCache: HashMap<Int, String>,
    ) {
        File(folderPath).mkdirs()
        val fullPath = "$folderPath/$fileId.png"

        FileOutputStream(File(fullPath)).let {
            bitmap.compress(Bitmap.CompressFormat.PNG, ICON_QUALITY, it)
            it.flush()
            it.close()
        }

        filePathCache[fileId] = fullPath
    }

    private fun colorLayerInDrawable(drawable: Drawable, @IdRes layerId: Int, color: Int): LayerDrawable {
        val mutableDrawable = drawable.mutate() as LayerDrawable
        mutableDrawable.findDrawableByLayerId(layerId).setTint(color)

        return mutableDrawable
    }
}
