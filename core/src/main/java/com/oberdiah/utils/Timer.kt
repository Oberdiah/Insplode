package com.oberdiah.utils

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
            numAvgEntries++
            currentAvg = (currentAvg * (numAvgEntries - 1) + value) / numAvgEntries
            field = value
        }

    var currentAvg = 0.0
    var numAvgEntries = 0
    var peak: Double = 0.0
    var heldPeak: Double = 0.0
    var heldAvg: Double = 0.0

    fun tickOver() {
        heldPeak = peak
        heldAvg = currentAvg
        peak = 0.0
        currentAvg = 0.0
        numAvgEntries = 0
    }

    override fun toString(): String {
        return "${name.padEnd(25, ' ')} Avg: ${heldAvg.format(2)}ms, Peak: ${heldPeak.format(2)}ms"
    }
}

val frameTimes = mutableMapOf<String, TimedInstance>()
private var lastTimeReset = 0.0

fun timerEnd() {
    lastFrameTime = (System.nanoTime() - frameStart) / 1000000.0
    fpsQueue.add(lastFrameTime)
    if (fpsQueue.size > 100) {
        fpsQueue.removeAt(0)
    }
    val avg = fpsQueue.average()

    val times = frameTimes.toList().sortedBy { it.second.heldAvg }.reversed()

    timerString = "Last frame: ${lastFrameTime.format(3)} avg: ${avg.format(3)} (${
        (avg / (10 / 120.0)).format(1)
    }%)\n"
    timerString += "Average delta: ${GameTime.AVERAGE_GRAPHICS_DELTA.format(3)}\n"
    for (t in times) {
        if (t.second.heldPeak > 0.2 || t.second.heldAvg > 0.2) {
            timerString += "${t.second}\n"
        }
    }

    if (GameTime.APP_TIME > lastTimeReset + 1.0) {
        if (GameTime.APP_TIME % 10.0 < 1.0) {
            println(timerString)
        }
        lastTimeReset = GameTime.APP_TIME
        for (t in frameTimes.values) {
            t.tickOver()
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