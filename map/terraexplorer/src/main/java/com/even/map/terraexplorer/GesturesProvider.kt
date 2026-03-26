package com.even.map.terraexplorer

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

class GesturesProvider(
    private val context: Context,
) {
    enum class Duration(val milliseconds: Long) {
        DEFAULT(20L)
    }

    fun vibrate(duration: Duration) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator.vibrate(VibrationEffect.createOneShot(duration.milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
