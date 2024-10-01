package com.oberdiah.utils

import com.badlogic.gdx.Gdx
import com.oberdiah.d

object GameTime {
    var GAME_SPEED = 1.0
        private set

    fun updateGameSpeed(newSpeed: Double) {
        GAME_SPEED = newSpeed
    }

    /**
     * For use for graphics that shouldn't slow down or speed up no matter the game speed.
     * This should really only be used for UI elements.
     */
    var GRAPHICS_DELTA = 0.016
        private set

    /**
     * For use for gameplay that needs to slow down or speed up with the game speed.
     * As a rule of thumb, if it's rendered or part of the game-world in any way, it should use this.
     */
    var GAMEPLAY_DELTA = 0.016
        private set

    /**
     * Specifically not touched whatsoever by gameplay speeds or other things.
     * Monotonically increasing since app launch.
     */
    var APP_TIME = 0.0
        private set

    val AVERAGE_GRAPHICS_DELTA: Double
        get() = LAST_HUNDRED_DELTAS.average()

    val AVERAGE_GAMEPLAY_DELTA: Double
        get() = AVERAGE_GRAPHICS_DELTA * GAME_SPEED

    private val LAST_HUNDRED_DELTAS = mutableListOf<Double>()

    fun tick() {
        val graphicsDelta = Gdx.graphics.deltaTime.d

        APP_TIME += graphicsDelta
        GRAPHICS_DELTA = graphicsDelta
        LAST_HUNDRED_DELTAS.add(graphicsDelta)
        GAMEPLAY_DELTA = GRAPHICS_DELTA * GAME_SPEED

        if (LAST_HUNDRED_DELTAS.size > 100) {
            LAST_HUNDRED_DELTAS.removeAt(0)
        }
    }

}