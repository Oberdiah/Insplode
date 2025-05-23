package com.oberdiah.player

import com.oberdiah.*
import com.oberdiah.Point.Companion.invoke
import com.oberdiah.level.Level
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController

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

    val isUsingApexWings
        get() = player.state.isIntentionallyMovingUp &&
                player.body.velocity.y < 1.0 &&
                player.body.velocity.y > 0.0 &&
                UpgradeController.playerHas(Upgrade.Slam)

    val currentGravity: Double
        get() {
            val postSlamGravity = 0.5
            return if (UpgradeController.playerHas(Upgrade.Slam)) {
                if (isUsingApexWings) {
                    postSlamGravity * 0.5
                } else if (player.state.isSlamming) {
                    postSlamGravity * 8.0
                } else {
                    postSlamGravity
                }
            } else {
                if (UpgradeController.playerHas(Upgrade.Mobility)) {
                    0.75
                } else {
                    1.0
                }
            }
        }

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

    var distanceMovedLeftRightThisRun = 0.0
    var numTimesJumpedThisRun = 0
    var numTimesSlammedThisRun = 0
    private var lastTickPosition: Point? = null

    fun reset() {
        tileBelowMe = null
        lastTickPosition = null

        isStandingOnStandableGenerous = false
        isStandingOnStandableExact = false
        isStandingOnNotBombExact = false

        distanceMovedLeftRightThisRun = 0.0
        numTimesJumpedThisRun = 0
        numTimesSlammedThisRun = 0

        bombsStandingOnGenerous.clear()
    }

    fun tick() {
        previousVelocities.add(0, player.body.velocity)
        if (previousVelocities.size > 10) {
            previousVelocities.removeLastOrNull()
        }

        val lastTickPos = lastTickPosition
        if (lastTickPos != null) {
            distanceMovedLeftRightThisRun += (player.body.p.x - lastTickPos.x).abs
        }
        lastTickPosition = player.body.p

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

                val newTile = Level.getTile(pos)
                if (newTile is Tile && newTile.doesExist()) {
                    return newTile
                }
            }
        }

        return null
    }
}