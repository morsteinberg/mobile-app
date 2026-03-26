package com.even.map.terraexplorer.functions

import com.even.core.extensions.ListExtensions.diffByKey
import kotlinx.coroutines.flow.Flow

class StateTracker<T, K> {
    private var previousState: List<T> = emptyList()

    suspend fun collect(
        state: Flow<List<T>>,
        keySelector: (T) -> K,
        onAdded: (suspend (T) -> Unit)? = null,
        onUpdated: (suspend (T) -> Unit)? = null,
        onRemoved: (suspend (T) -> Unit)? = null
    ) {
        state.collect { currentState ->
            val diff = previousState.diffByKey(currentState, keySelector)

            diff.addedItems.forEach { item -> onAdded?.invoke(item) }
            diff.updatedItems.forEach { item -> onUpdated?.invoke(item) }
            diff.removedItems.forEach { item -> onRemoved?.invoke(item) }

            previousState = currentState
        }
    }
}
