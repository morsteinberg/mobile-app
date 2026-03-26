package com.even.map.terraexplorer.wrappers

import com.even.map.terraexplorer.sgWorldInstance
import com.skyline.terraexplorer.models.UI

private val version
    get() = sgWorldInstance.version

internal object VersionWrapper {
    fun getTEVersion() = ThreadWrapper.launchSync {
        version?.run { "$major.$minor.$build.$freeze" }
    }
}
