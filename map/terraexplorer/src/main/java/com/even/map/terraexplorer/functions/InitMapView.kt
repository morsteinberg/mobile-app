package com.even.map.terraexplorer.functions

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import com.even.core.coroutines.DispatchersProvider
import com.even.core.extensions.StringExtensions.withFileExtension
import com.even.core.file.DeviceStorageAssetManager
import com.even.core.logger.Logger
import com.even.core.types.FileType
import com.even.map.terraexplorer.constants.Param
import com.even.map.terraexplorer.predefinedLayersGroupName
import com.even.map.terraexplorer.sgWorldInstance
import com.even.map.terraexplorer.wrappers.ProjectTreeWrapper
import com.even.map.terraexplorer.wrappers.ProjectWrapper
import com.even.map.terraexplorer.wrappers.ThreadWrapper
import com.even.map.terraexplorer.wrappers.VersionWrapper
import com.skyline.core.CoreServices
import com.skyline.terraexplorer.TEApp
import com.skyline.terraexplorer.models.LocalBroadcastManager
import com.skyline.terraexplorer.views.TEGLRenderer
import com.skyline.terraexplorer.views.TEView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun initMapView(
    logger: Logger,
    deviceStorageAssetManager: DeviceStorageAssetManager,
    dispatchers: DispatchersProvider,
    activity: Activity,
    onCreated: () -> Unit,
): View {
    TEApp.setMainActivityContext(activity)
    TEApp.setApplicationContext(activity.applicationContext)
    CoreServices.Init(activity)

    LocalBroadcastManager.getInstance(activity).registerReceiver(
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onEngineInitialized(logger, deviceStorageAssetManager, dispatchers, context, onCreated)
                LocalBroadcastManager.getInstance(activity).unregisterReceiver(this)
            }
        },
        IntentFilter(TEGLRenderer.ENGINE_INITIALIZED),
    )

    return TEView(activity.baseContext)
}

private fun onEngineInitialized(
    logger: Logger,
    deviceStorageAssetManager: DeviceStorageAssetManager,
    dispatchers: DispatchersProvider,
    context: Context,
    onCreated: () -> Unit,
) {
    logger.i("Terra-Explorer engine initialized")
    ThreadWrapper.launchLazy {
        ProjectWrapper(context).open()
        setParams()
        loadPredefinedLayers(deviceStorageAssetManager)
        ProjectTreeWrapper.hideRootId()

        logger.i("Terra-Explorer project Opened")
        logger.d("Terra Explorer version: ${VersionWrapper.getTEVersion()}")
        CoroutineScope(dispatchers.main).launch {
            onCreated()
        }
    }
}

private fun loadPredefinedLayers(deviceStorageAssetManager: DeviceStorageAssetManager) {
    val flyAssetName = predefinedLayersGroupName.withFileExtension(FileType.TERRAEXPLORER_PROJECT)
    ProjectTreeWrapper.loadFlyFile(deviceStorageAssetManager.getFilePath(flyAssetName))
}

private fun setParams() {
    listOf(
        Param.HOLD_GESTURE_THRESHOLD_MS to 500,
        Param.TOGGLE_FEATURE_DECLUSTERING to 0,
    ).forEach {
        sgWorldInstance.SetParam(it.first.code, it.second)
    }
}
