package com.oberdiah.player

import com.oberdiah.Point
import com.oberdiah.TILE_SIZE_IN_UNITS
import com.oberdiah.Tile
import com.oberdiah.getTile

/**
 * Has lots of useful information about the player.
 * Ideally doesn't keep track of any state, and if it does it should be done silently
 */
class PlayerInformationBoard {
    /// We need these because sometimes on collide the velocity is already really small for some reason
    var tickVelocity = Point(0.0, 0.0)
        private set

    var lastTickVelocity = Point(0.0, 0.0)
        private set

    // If this is non-null, then it is a tile that exists.
    var tileBelowMe: Tile? = null
        private set

    fun reset() {
        tickVelocity = Point(0.0, 0.0)
        lastTickVelocity = Point(0.0, 0.0)
    }

    fun tick() {
        lastTickVelocity = tickVelocity.cpy
        tickVelocity = player.body.velocity.cpy
        tileBelowMe = getTileBelowMe()
    }
}

private fun getTileBelowMe(): Tile? {
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