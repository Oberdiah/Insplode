package com.oberdiah.utils

import com.oberdiah.AVERAGE_DELTA
import com.oberdiah.DELTA
import com.oberdiah.format
import com.oberdiah.max

var timerString = "No data yet"

private var frameStart = 0L
fun timerStart() {
    frameStart = System.nanoTime()
}

private val fpsQueue = mutableListOf<Double>()
private var lastFrameTime = 0.0

class TimedInstance(val name: String, initialTime: Double) {
    var timeMS: Double = initialTime
        set(value) {
            peak = max(peak, value)
            field = value
        }

    var peak: Double = 0.0
    var heldPeak: Double = 0.0

    override fun toString(): String {
        return "$name: ${heldPeak.format(2)}ms"
    }
}

val frameTimes = mutableMapOf<String, TimedInstance>()
var timeSinceLastResetMs = 0.0

fun timerEnd() {
    timeSinceLastResetMs += DELTA

    lastFrameTime = (System.nanoTime() - frameStart) / 1000000.0
    fpsQueue.add(lastFrameTime)
    if (fpsQueue.size > 100) {
        fpsQueue.removeAt(0)
    }
    val avg = fpsQueue.average()

    val times = frameTimes.toList().sortedBy { it.second.heldPeak }.reversed()

    timerString = "Last frame: ${lastFrameTime.format(3)} avg: ${avg.format(3)} (${
        (avg / (10 / 120.0)).format(1)
    }%)\n"
    timerString += "Average delta: ${AVERAGE_DELTA.format(3)}\n"
    for (t in times) {
        if (t.second.heldPeak > 0.2) {
            timerString += "${t.second}\n"
        }
    }

    if (timeSinceLastResetMs > 1.0) {
        timeSinceLastResetMs = 0.0
        for (t in frameTimes.values) {
            t.heldPeak = t.peak
            t.peak = 0.0
        }
    }
}

fun time(name: String = "Extra", callback: () -> Unit) {
    val start = System.nanoTime()
    callback()
    val end = System.nanoTime()

    val thisTime = (end - start) / 1000000.0

    frameTimes.getOrPut(name) { TimedInstance(name, thisTime) }.timeMS = thisTime
}