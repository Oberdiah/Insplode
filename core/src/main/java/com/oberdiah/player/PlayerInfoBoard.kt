package com.oberdiah.player

import com.oberdiah.Bomb
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.Point
import com.oberdiah.TILE_SIZE_IN_UNITS
import com.oberdiah.Tile
import com.oberdiah.Velocity
import com.oberdiah.level.getTile
import com.oberdiah.whatAmITouching

/**
 * Has lots of useful information about the player.
 * Ideally doesn't keep track of any state, and if it does it should be done silently
 */
object PlayerInfoBoard {
    /** Velocities further from 0 are older */
    private val previousVelocities = mutableListOf<Velocity>()

    val velocity
        get() = previousVelocities.getOrNull(2) ?: Velocity()

    /** The fastest downward-Y velocity in the last 10 ticks. Used for slamming */
    val slammingVelocity
        get() = previousVelocities.minOfOrNull { it.y } ?: 0.0

    val playerFeetPosition
        get() = player.body.p - Point(0.0, PLAYER_SIZE.w / 2 * GLOBAL_SCALE)

    // If this is non-null, then it is a tile that exists.
    var tileBelowMe: Tile? = null
        private set

    /**
     * Our feet are vaguely near something we can stand on.
     * Good for determining if we should be able to jump.
     */
    var isStandingOnStandableGenerous: Boolean = false
        private set

    /**
     * Our feet are visually touching something we can stand on.
     */
    var isStandingOnStandableExact: Boolean = false
        private set

    /**
     * Our feet are visually touching the ground. Explicitly not a bomb.
     */
    var isStandingOnNotBombExact: Boolean = false
        private set

    val bombsStandingOnGenerous = mutableListOf<Bomb>()

    fun reset() {
        tileBelowMe = null

        isStandingOnStandableGenerous = false
        isStandingOnStandableExact = false
        isStandingOnNotBombExact = false
        bombsStandingOnGenerous.clear()
    }

    fun tick() {
        previousVelocities.add(0, player.body.velocity.cpy)
        if (previousVelocities.size > 10) {
            previousVelocities.removeLastOrNull()
        }

        tileBelowMe = whatTileIsBelowMe()

        isStandingOnStandableGenerous = false
        bombsStandingOnGenerous.clear()
        isStandingOnStandableExact = false
        isStandingOnNotBombExact = false

        whatAmITouching(listOf(player.wideFeetBox)).forEach {
            val isTile = it is Tile && it.doesExist()
            val isBomb = it is Bomb

            if (isTile || isBomb) {
                isStandingOnStandableGenerous = true
            }
            if (it is Bomb) {
                bombsStandingOnGenerous += it
            }
        }

        whatAmITouching(listOf(player.narrowFeetBox)).forEach {
            val isTile = it is Tile && it.doesExist()
            if (isTile) {
                isStandingOnStandableExact = true
                isStandingOnNotBombExact = true
            }

            if (it is Bomb) {
                if (it.isStandable) {
                    isStandingOnStandableExact = true
                }
            }
        }
    }

    private fun whatTileIsBelowMe(): Tile? {
        // We effectively need to check in a 3x3 grid below the player

        // Top middle of the search grid
        val searchStartPos = playerFeetPosition

        for (x in intArrayOf(0, 1, -1)) {
            for (y in 0 downTo -2) {
                val pos = searchStartPos + Point(
                    x * TILE_SIZE_IN_UNITS * 0.75,
                    y * TILE_SIZE_IN_UNITS * 0.75
                )

                val newTile = getTile(pos)
                if (newTile is Tile && newTile.doesExist()) {
                    return newTile
                }
            }
        }

        return null
    }
}