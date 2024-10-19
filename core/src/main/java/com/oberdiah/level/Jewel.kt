package com.oberdiah.level

import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.ScoreSystem
import com.oberdiah.Sprites
import com.oberdiah.Tile
import com.oberdiah.player.player
import com.oberdiah.sin
import com.oberdiah.ui.goToDiegeticMenu
import com.oberdiah.utils.TileType

object Jewel {
    private const val JEWEL_DEPTH = -100.0

    fun renderJewelInLevel(r: Renderer) {
        if (ScoreSystem.isFinalRun()) {
            r.centeredSprite(
                Sprites.getSprite("Victory"),
                Point(5.1, JEWEL_DEPTH),
                2.0,
                angle = RUN_TIME_ELAPSED * 20.0
            )
            r.centeredSprite(
                Sprites.getSprite("Victory"),
                Point(5.1, JEWEL_DEPTH),
                1.5 + sin(RUN_TIME_ELAPSED * 2.5) * 0.25,
                angle = -RUN_TIME_ELAPSED * 20.0
            )
        }
    }

    fun tick() {
        if (player.body.p.distTo(5.1, JEWEL_DEPTH) < 1.0 && ScoreSystem.isFinalRun()) {
            goToDiegeticMenu()
            ScoreSystem.registerGameEnd(grabbedJewel = true)
        }
    }

    /**
     * Returns true if we should stop level gen at this point.
     *
     * Returns false if we should continue.
     */
    fun generateJewelInLevel(tile: Tile): Boolean {
        if (ScoreSystem.isFinalRun()) {
            val distToJewel = tile.coord.distTo(5.1, JEWEL_DEPTH)
            if (distToJewel < 4.0) {
                tile.setTileType(TileType.CaveWall)

                if (distToJewel < 3.0) {
                    val belowFloor = tile.coord.y < JEWEL_DEPTH - 1.0
                    val belowMantle = tile.coord.y < JEWEL_DEPTH - 0.5

                    if (distToJewel < 2.6) {
                        if (belowFloor) {
                            tile.setTileType(TileType.Dirt)
                        }
                    } else if (belowMantle) {
                        tile.setTileType(TileType.Stone)
                    }
                }

                return true
            }
        }
        return false
    }
}