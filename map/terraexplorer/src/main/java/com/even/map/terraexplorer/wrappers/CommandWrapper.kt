package com.even.map.terraexplorer.wrappers

import com.even.map.terraexplorer.sgWorldInstance
import com.skyline.terraexplorer.models.UI


private val command
    get() = sgWorldInstance.command

private enum class Command(val code: Int) {
    NORTH(1056),
}

internal object CommandWrapper {
    fun north() = ThreadWrapper.launchSync { command.Execute(Command.NORTH.code) }
}
