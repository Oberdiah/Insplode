package com.oberdiah

import kotlin.random.Random

val currentlyFloatingTiles = mutableSetOf<Tile>()
fun tickCollapse() {
    if (tilesChangedThisFrame.isNotEmpty()) {
        val wavefront = mutableSetOf<Tile>()
        val visited = mutableSetOf<Tile>()
        currentlyFloatingTiles.clear()
        currentlyFloatingTiles.addAll(simplesStored.filter { it.doesExist() })

        for (x in 0 until SIMPLES_WIDTH) {
            val tile = getTile(x, getLowestStoredSimpleY())
            if (tile is Tile && tile.doesExist()) {
                wavefront.add(tile)
            }
        }

        while (wavefront.size > 0) {
            val tile = wavefront.elementAt(0)
            wavefront.remove(tile)
            currentlyFloatingTiles.remove(tile)
            visited.add(tile)
            for (t in tile.allSurroundingTiles) {
                if (t is Tile && !visited.contains(t) && t.doesExist()) {
                    wavefront.add(t)
                }
            }
        }
    }

    val toRemove = mutableSetOf<Tile>()
    for (tile in currentlyFloatingTiles) {
        if (!tile.data.bl.doesExist() && !tile.data.br.doesExist() && !tile.data.bm.doesExist()) {
            tile.destructionTime += DELTA
        }
        if (tile.destructionTime > Random.nextDouble()) {
            toRemove.add(tile)
        }
    }

    for (tile in toRemove) {
        tile.dematerialize()
        currentlyFloatingTiles.remove(tile)
        spawnFragment(
            tile.coord.cpy,
            Point(Random.nextDouble() - 0.5, -Random.nextDouble()),
            tile.getTileType()
        )
    }
}