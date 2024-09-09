package com.oberdiah.Utils

import com.badlogic.gdx.Gdx
import com.oberdiah.APP_FRAME
import com.oberdiah.HEIGHT
import com.oberdiah.TouchPoint
import com.oberdiah.d

fun isKeyPressed(key: Int): Boolean {
    return Gdx.input.isKeyPressed(key)
}

fun isKeyJustPressed(key: Int): Boolean {
    return Gdx.input.isKeyJustPressed(key)
}

private val allTouches = Array(Gdx.input.maxPointers) { TouchPoint(it) }
val TOUCHES_DOWN = mutableListOf<TouchPoint>()
val TOUCHES_UP = mutableListOf<TouchPoint>()
val TOUCHES_DOWN_LAST_FRAME = mutableListOf<TouchPoint>()
val TOUCHES_UP_LAST_FRAME = mutableListOf<TouchPoint>()
val TOUCHES_WENT_DOWN = mutableListOf<TouchPoint>()
val TOUCHES_WENT_UP = mutableListOf<TouchPoint>()
val A_TOUCH_WENT_DOWN
    get() = TOUCHES_WENT_DOWN.isNotEmpty()
val A_TOUCH_WENT_UP
    get() = TOUCHES_WENT_UP.isNotEmpty()


fun calculateInputGlobals() {
    TOUCHES_DOWN_LAST_FRAME.clear()
    TOUCHES_UP_LAST_FRAME.clear()
    TOUCHES_DOWN_LAST_FRAME.addAll(TOUCHES_DOWN)
    TOUCHES_UP_LAST_FRAME.addAll(TOUCHES_UP)
    TOUCHES_DOWN.clear()
    TOUCHES_WENT_DOWN.clear()
    TOUCHES_UP.clear()
    TOUCHES_WENT_UP.clear()

    allTouches.forEachIndexed { index, point ->
        point.x = Gdx.input.getX(index).d
        point.y = HEIGHT - Gdx.input.getY(index).d
        if (Gdx.input.isTouched(index)) {
            TOUCHES_DOWN.add(point)
        } else {
            TOUCHES_UP.add(point)
        }
    }

    TOUCHES_DOWN.forEach { thisFrame ->
        if (TOUCHES_DOWN_LAST_FRAME.none { it.index == thisFrame.index }) {
            TOUCHES_WENT_DOWN.add(thisFrame)
            thisFrame.frameDown = APP_FRAME
        }
    }
    TOUCHES_UP.forEach { thisFrame ->
        if (TOUCHES_UP_LAST_FRAME.none { it.index == thisFrame.index }) {
            TOUCHES_WENT_UP.add(thisFrame)
            thisFrame.frameUp = APP_FRAME
        }
    }
    TOUCHES_DOWN.sortBy { it.frameDown }
    TOUCHES_UP.sortBy { it.frameUp }
}