package com.jason.searcher.entities

import java.io.Serializable

class VideoCoverRatio private constructor(val value: Double) : Serializable {
    companion object {
        val Auto = VideoCoverRatio(0.0)

        val Fixed_16_9 = VideoCoverRatio(16.0 / 9.0)

        val Fixed_4_3 = VideoCoverRatio(4.0 / 3.0)

        val Fixed_1_1 = VideoCoverRatio(1.0)

        val Fixed_2_3 = VideoCoverRatio(2.0 / 3.0)

        fun Fixed(ratio: Double): VideoCoverRatio {
            return if (ratio.isNaN()) Auto else VideoCoverRatio(ratio)
        }
    }
}