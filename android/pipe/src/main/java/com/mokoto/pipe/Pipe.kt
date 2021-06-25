package com.mokoto.pipe

import androidx.annotation.Keep


@Keep
object Pipe {

    external fun computeFocalLengthInPixels(
        width: Int,
        height: Int,
        focal_length_in_35mm: Float = 25f,
        focal_length_mm: Float = 3.38f
    ): Float

    external fun calculateDepth(
        width: Int,
        height: Int,
        marks: Array<P3>
    ): Float

    external fun initialization(
        focalLengthInPixel: Float,
        inMeter: Float = 10f,
    ): Int
}

//fun main() {
//    Pipe.initialization()
//    Pipe.computeFocalLengthInPixels(640, 480)
//}