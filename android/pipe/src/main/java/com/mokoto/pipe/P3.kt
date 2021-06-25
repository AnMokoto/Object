package com.mokoto.pipe

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class P3(
    val x: Float,
    val y: Float,
    val z: Float
) : Serializable
