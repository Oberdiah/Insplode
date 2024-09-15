package com.oberdiah

import kotlin.random.Random

/**
 * We don't store tiles here as this array exists across frames and the tiles may not.
 *
 * This is a slightly confusing set - it not only contains all existing tiles known to be floating,
 * but also all non-existing tiles. We do it this way because we want to be able to avoid
 * triggering the collapse algorithm due to the floating tile collapse animation.
 *
 * We can't just filter out the non-existing tiles from the update list
 * because we need to know if they used to exist last frame.
 */
private val allFloatingTileIds = mutableSetOf<TileId>()
fun tickCollapse() {
    if ((tileIdsChangedLastFrame.filter { it.y != getLowestStoredSimpleY() } - allFloatingTileIds).isNotEmpty()) {
        val wavefront = mutableSetOf<Tile>()
        val visited = mutableSetOf<Tile>()

        allFloatingTileIds.clear()
        allFloatingTileIds.addAll(tilesStorage.map { it.getId() })

        for (x in 0 until NUM_TILES_ACROSS) {
            val tile = getTile(x, getLowestStoredSimpleY())
            if (tile is Tile && tile.doesExist()) {
                wavefront.add(tile)
            }
        }

        while (wavefront.size > 0) {
            val tile = wavefront.elementAt(0)
            allFloatingTileIds.remove(tile.getId())
            wavefront.remove(tile)
            visited.add(tile)
            for (t in tile.allSurroundingTiles) {
                if (t is Tile && !visited.contains(t) && t.doesExist()) {
                    wavefront.add(t)
                }
            }
        }
    }

    val toRemove = mutableSetOf<Tile>()
    val noLongerFloating = mutableSetOf<TileId>()
    for (tileId in allFloatingTileIds) {
        val tile = getTile(tileId)
        if (tile !is Tile) {
            noLongerFloating.add(tileId)
            continue
        }
        if (!tile.doesExist()) {
            continue
        }

        if (!tile.data.bl.doesExist() && !tile.data.br.doesExist() && !tile.data.bm.doesExist()) {
            tile.destructionTime += DELTA
        }
        if (tile.destructionTime > Random.nextDouble()) {
            toRemove.add(tile)
        }
    }

    for (tileId in noLongerFloating) {
        allFloatingTileIds.remove(tileId)
    }

    for (tile in toRemove) {
        tile.dematerialize()
        spawnFragment(
            tile.coord.cpy,
            Point(Random.nextDouble() - 0.5, -Random.nextDouble()),
            tile.getTileType()
        )
    }
}