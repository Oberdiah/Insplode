package com.oberdiah.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.HEIGHT
import com.oberdiah.IS_DEBUG_ENABLED
import com.oberdiah.PAUSED
import com.oberdiah.Rect
import com.oberdiah.Renderer
import com.oberdiah.Screen
import com.oberdiah.Utils.TOUCHES_DOWN
import com.oberdiah.Utils.TOUCHES_WENT_UP
import com.oberdiah.Utils.isKeyJustPressed
import com.oberdiah.Utils.isKeyPressed
import com.oberdiah.WIDTH
import com.oberdiah.max
import com.oberdiah.withAlpha

var PAUSE_SIDE = Align.right
private val PAUSE_SIZE_FRACT = 0.12
private val PAUSE_FROM_TOP_FRACT = 0.05
private val PAUSE_FROM_EDGE_FRACT = 0.05
private val PAUSE_SIZE
    get() = PAUSE_SIZE_FRACT * WIDTH
private val PAUSE_FROM_TOP
    get() = PAUSE_FROM_TOP_FRACT * WIDTH
private val PAUSE_FROM_EDGE
    get() = PAUSE_FROM_EDGE_FRACT * WIDTH

private val pauseRect = Rect()
val PAUSE_RECT: Rect
    get() {
        pauseRect.p.x =
            if (PAUSE_SIDE == Align.left) PAUSE_FROM_EDGE else WIDTH - PAUSE_SIZE - PAUSE_FROM_EDGE
        pauseRect.p.y = HEIGHT - PAUSE_SIZE - PAUSE_FROM_TOP
        pauseRect.s.w = PAUSE_SIZE
        pauseRect.s.h = PAUSE_SIZE
        return pauseRect
    }

fun renderPauseButton(r: Renderer) {
    pauseUIFadeOff *= 0.95
    if (pauseHovered) {
        r.color = Color.WHITE.withAlpha(max(pauseUIFadeOff, 0.9))
    } else {
        r.color = Color.WHITE.withAlpha(max(pauseUIFadeOff, 0.5))
    }
    pauseHovered = false
    TOUCHES_DOWN.forEach {
        if (PAUSE_RECT.contains(it)) {
            pauseHovered = true
        }
    }
    TOUCHES_WENT_UP.forEach {
        if (PAUSE_RECT.contains(it)) {
            pauseUIFadeOff = 1.0
            PAUSED = true
            switchScreen(Screen.Paused)
        }
    }
    // Escape key pauses the game.
    if (isKeyJustPressed(Keys.ESCAPE)) {
        PAUSED = true
        switchScreen(Screen.Paused)
    }

    val p = PAUSE_RECT.p
    r.rect(p.x + PAUSE_SIZE * 0.2, p.y + PAUSE_SIZE * 0.1, PAUSE_SIZE * 0.2, PAUSE_SIZE * 0.8)
    r.rect(p.x + PAUSE_SIZE * 0.6, p.y + PAUSE_SIZE * 0.1, PAUSE_SIZE * 0.2, PAUSE_SIZE * 0.8)
}