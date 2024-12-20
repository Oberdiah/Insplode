package com.oberdiah.utils

import com.oberdiah.d
import kotlin.math.pow
import kotlin.random.Random

object Perlin {
    var noise = OpenSimplexNoise(8)

    fun randomize() {
        noise = OpenSimplexNoise(Random.nextLong())
    }

    fun noise(x: Double, y: Double, period: Double): Double {
        return noise.eval(x / period, y / period)
    }

    fun fbm(xI: Double, yI: Double, octaves: Int, smoothing: Double): Double {
        var x = xI / 2.0.pow(octaves)
        var y = yI / 2.0.pow(octaves)
        var value = 0.0
        var amplitude = 1.0
        for (i in 0 until octaves) {
            value += amplitude * (noise.eval(x, y))
            x *= 2.0
            y *= 2.0
            amplitude /= smoothing
        }
        return value
    }
}