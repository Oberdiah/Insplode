package com.oberdiah.utils

import com.oberdiah.AVERAGE_DELTA
import com.oberdiah.format

var timerString = "No data yet"

private var frameStart = 0L
fun timerStart() {
    frameStart = System.nanoTime()
}

private val fpsQueue = mutableListOf<Double>()
private var lastFrameTime = 0.0

class TimedInstance(val name: String, val timeMS: Double) {
    override fun toString(): String {
        return "$name: ${timeMS.format(2)}ms"
    }
}

val frameTimes = mutableListOf<TimedInstance>()

fun timerEnd() {
    lastFrameTime = (System.nanoTime() - frameStart) / 1000000.0
    fpsQueue.add(lastFrameTime)
    if (fpsQueue.size > 100) {
        fpsQueue.removeAt(0)
    }
    val avg = fpsQueue.average()

    frameTimes.sortByDescending { it.timeMS }

    timerString = "Last frame: ${lastFrameTime.format(3)} avg: ${avg.format(3)} (${
        (avg / (10 / 120.0)).format(1)
    }%)\n"
    timerString += "Average delta: ${AVERAGE_DELTA.format(3)}\n"
    for (t in frameTimes) {
        if (t.timeMS > 0.2) {
            timerString += "$t\n"
        }
    }

    frameTimes.clear()
}

fun time(name: String = "Extra", callback: () -> Unit) {
    val start = System.nanoTime()
    callback()
    val end = System.nanoTime()
    frameTimes.add(TimedInstance(name, (end - start) / 1000000.0))
}