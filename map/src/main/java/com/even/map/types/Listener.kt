package com.even.map.types

class Listener(private val removeCallback: () -> Unit) {
    fun remove() = removeCallback()
}
