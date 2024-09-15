package com.oberdiah

import kotlin.random.Random

/** We don't store tiles here as this array exists across frames and the tiles may not. */
val currentlyFloatingTileIds = mutableSetOf<Int>()
fun tickCollapse() {
    if (tilesChangedThisFrame.isNotEmpty()) {
        val wavefront = mutableSetOf<Tile>()
        val visited = mutableSetOf<Tile>()
        currentlyFloatingTileIds.clear()
        currentlyFloatingTileIds.addAll(simplesStored.filter { it.doesExist() }.map { it.getId() })

        for (x in 0 until SIMPLES_WIDTH) {
            val tile = getTile(x, getLowestStoredSimpleY())
            if (tile is Tile && tile.doesExist()) {
                wavefront.add(tile)
            }
        }

        while (wavefront.size > 0) {
            val tile = wavefront.elementAt(0)
            wavefront.remove(tile)
            currentlyFloatingTileIds.remove(tile.getId())
            visited.add(tile)
            for (t in tile.allSurroundingTiles) {
                if (t is Tile && !visited.contains(t) && t.doesExist()) {
                    wavefront.add(t)
                }
            }
        }
    }

    val toRemove = mutableSetOf<Tile>()
    for (tileId in currentlyFloatingTileIds) {
        val tile = getTile(tileId)

        if (tile is Tile) {
            if (!tile.data.bl.doesExist() && !tile.data.br.doesExist() && !tile.data.bm.doesExist()) {
                tile.destructionTime += DELTA
            }
            if (tile.destructionTime > Random.nextDouble()) {
                toRemove.add(tile)
            }
        }
    }

    for (tile in toRemove) {
        tile.dematerialize()
        currentlyFloatingTileIds.remove(tile.getId())
        spawnFragment(
            tile.coord.cpy,
            Point(Random.nextDouble() - 0.5, -Random.nextDouble()),
            tile.getTileType()
        )
    }
}