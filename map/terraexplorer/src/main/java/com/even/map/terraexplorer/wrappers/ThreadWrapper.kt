package com.even.map.terraexplorer.wrappers

import android.util.Log
import com.skyline.terraexplorer.models.UI
import com.skyline.terraexplorer.views.TEGLRenderer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "MapThreadWrapper"

internal object ThreadWrapper {
    /**
     * Run the provided `block` in an async render thread at a late date
     * and without waiting for it's result.
     *
     * **Warning:** Exceptions thrown inside `block` will no be propagated
     * beyond the `launchLazy` call and will be silently logged.
     */
    fun launchLazy(block: () -> Unit) = UI.runOnRenderThreadAsync {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught exception while running a lazy async block:", e)
        }
    }

    /**
     * Run the provided `block` in an async render thread. Calling this
     * function inside a coroutine will suspend it until `block` has
     * finished executing.
     */
    suspend fun <T> launch(block: () -> T): T =
        suspendCoroutine { cont ->
            UI.runOnRenderThreadAsync {
                try {
                    cont.resume(block())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
        }

    /**
     * Switch to the render thread (if not already applied) and run the
     * provided `block`.
     *
     * **Warning:** Running complex code & heavy IO operations with this
     * function is not recommended as it can freeze up the `main` thread while
     * `block` is being executed!
     */
    fun <T> launchSync(block: () -> T): T =
        if (Thread.currentThread().id != TEGLRenderer._ThreadID) UI.runOnRenderThread<T>(block) else block()
}
