package com.oberdiah.utils

import com.oberdiah.d
import kotlin.math.pow
import kotlin.random.Random

object Perlin {
    var noise = OpenSimplexNoise(8)

    fun randomize() {
        noise = OpenSimplexNoise(Random.nextLong())
    }

    fun noise(x: Number, y: Number, period: Number): Double {
        return noise.eval(x.d / period.d, y.d / period.d)
    }

    fun fbm(xI: Number, yI: Number, octaves: Int, smoothing: Number): Double {
        var x = xI.d / 2.0.pow(octaves)
        var y = yI.d / 2.0.pow(octaves)
        var value = 0.0
        var amplitude = 1.0
        for (i in 0 until octaves) {
            value += amplitude * (noise.eval(x, y))
            x *= 2.0
            y *= 2.0
            amplitude /= smoothing.d
        }
        return value
    }
}