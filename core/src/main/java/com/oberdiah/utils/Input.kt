package com.oberdiah.utils

import com.badlogic.gdx.Gdx
import com.oberdiah.HEIGHT
import com.oberdiah.Point
import com.oberdiah.d

fun isKeyPressed(key: Int): Boolean {
    return Gdx.input.isKeyPressed(key)
}

fun isKeyJustPressed(key: Int): Boolean {
    return Gdx.input.isKeyJustPressed(key)
}

private val allTouches = Array(Gdx.input.maxPointers) { TouchPoint(it, Point()) }
val TOUCHES_DOWN = mutableListOf<TouchPoint>()
val TOUCHES_UP = mutableListOf<TouchPoint>()
val TOUCHES_DOWN_LAST_FRAME = mutableListOf<TouchPoint>()
val TOUCHES_UP_LAST_FRAME = mutableListOf<TouchPoint>()
val TOUCHES_WENT_DOWN = mutableListOf<TouchPoint>()
val TOUCHES_WENT_UP = mutableListOf<TouchPoint>()

class TouchPoint(val index: Int, var p: Point) {
    var timeUp: Double = -1.0
    var timeDown: Double = -1.0

    val x: Double
        get() = p.x

    val y: Double
        get() = p.y

    val wo: Point
        get() = p.wo

    val ui: Point
        get() = p.ui
}

fun calculateInputGlobals() {
    TOUCHES_DOWN_LAST_FRAME.clear()
    TOUCHES_UP_LAST_FRAME.clear()
    TOUCHES_DOWN_LAST_FRAME.addAll(TOUCHES_DOWN)
    TOUCHES_UP_LAST_FRAME.addAll(TOUCHES_UP)
    TOUCHES_DOWN.clear()
    TOUCHES_WENT_DOWN.clear()
    TOUCHES_UP.clear()
    TOUCHES_WENT_UP.clear()

    for (index in allTouches.indices) {
        val point = Point(Gdx.input.getX(index).d, HEIGHT - Gdx.input.getY(index).d)
        allTouches[index].p = point
        if (Gdx.input.isTouched(index)) {
            TOUCHES_DOWN.add(allTouches[index])
        } else {
            TOUCHES_UP.add(allTouches[index])
        }
    }

    TOUCHES_DOWN.forEach { thisFrame ->
        if (TOUCHES_DOWN_LAST_FRAME.none { it.index == thisFrame.index }) {
            TOUCHES_WENT_DOWN.add(thisFrame)
            thisFrame.timeDown = GameTime.APP_TIME
        }
    }
    TOUCHES_UP.forEach { thisFrame ->
        if (TOUCHES_UP_LAST_FRAME.none { it.index == thisFrame.index }) {
            TOUCHES_WENT_UP.add(thisFrame)
            thisFrame.timeUp = GameTime.APP_TIME
        }
    }
    TOUCHES_DOWN.sortBy { it.timeDown }
    TOUCHES_UP.sortBy { it.timeUp }
}