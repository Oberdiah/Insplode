package com.oberdiah.level

import com.oberdiah.NUM_TILES_ACROSS
import com.oberdiah.Point
import com.oberdiah.TILE_SIZE_IN_UNITS
import com.oberdiah.Tile
import com.oberdiah.TileId
import com.oberdiah.abs
import com.oberdiah.playRockCrumbleSound
import com.oberdiah.spawnFragment
import com.oberdiah.tileIdsChangedLastFrameAllNeighbors
import com.oberdiah.utils.GameTime
import kotlin.random.Random

private val collapsingTileIds = mutableSetOf<TileId>()

fun resetLevelCollapse() {
    collapsingTileIds.clear()
}

fun tickCollapse() {
    // We don't want to collapse in the first frame, it's just a huge waste of everyone's time.
    if (RUN_TIME_ELAPSED == 0.0) {
        return
    }

    for (tileId in tileIdsChangedLastFrameAllNeighbors) {
        val tile = Level.getTile(tileId)

        if (tile !is Tile || tile.y <= Level.getLowestStoredTileYIdx() + abs(Level.getLowestStoredTileYDiff())) {
            continue
        }

        if (tile.doesExist()) {
            if (!tile.data.bl.doesExist() && !tile.data.br.doesExist() && !tile.data.bm.doesExist()) {
                collapsingTileIds.add(tile.getId())
            }
        } else {
            if (!tile.data.lm.doesExist() && !tile.data.rm.doesExist()) {
                // Add the tile above us.
                val tileAbove = tile.data.tm
                if (tileAbove is Tile && tileAbove.doesExist()) {
                    collapsingTileIds.add(tileAbove.getId())
                }
            }
        }
    }

    val toRemove = mutableSetOf<Tile>()
    for (tileId in collapsingTileIds) {
        val tile = Level.getTile(tileId)
        if (tile !is Tile) {
            continue
        }
        if (!tile.doesExist()) {
            continue
        }

        if (!tile.data.bl.doesExist() && !tile.data.br.doesExist() && !tile.data.bm.doesExist()) {
            tile.destructionTime += GameTime.GAMEPLAY_DELTA
        }
        if (tile.destructionTime > Random.nextDouble()) {
            toRemove.add(tile)
        }
    }

    for (tile in toRemove) {
        collapsingTileIds.remove(tile.getId())
        playRockCrumbleSound()
        spawnFragment(
            tile.coord,
            Point(Random.nextDouble() - 0.5, -Random.nextDouble()),
            tile.getTileType()
        )
        tile.dematerialize(reason = Tile.DematerializeReason.Collapse)
    }
}