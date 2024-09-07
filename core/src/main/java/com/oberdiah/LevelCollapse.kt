package com.oberdiah

import kotlin.random.Random

val currentlyFloatingTiles = mutableSetOf<Tile>()
fun tickCollapse() {
    if (tilesChangedThisFrame.isNotEmpty()) {
        val wavefront = mutableSetOf<Tile>()
        val visited = mutableSetOf<Tile>()
        currentlyFloatingTiles.clear()
        currentlyFloatingTiles.addAll(simplesStored.filter { it.exists })

        for (x in 0 until SIMPLES_WIDTH) {
            val tile = getTile(x, LOWEST_SIMPLE_Y_STORED)
            if (tile.isSafe && tile.exists) {
                wavefront.add(tile)
            }
        }

        while (wavefront.size > 0) {
            val tile = wavefront.elementAt(0)
            tile.attachedToGround = true
            wavefront.remove(tile)
            currentlyFloatingTiles.remove(tile)
            visited.add(tile)
            for (t in tile.allSurroundingTiles) {
                if (!visited.contains(t) && t.exists) {
                    wavefront.add(t)
                }
            }
        }
    }

    val toRemove = mutableSetOf<Tile>()
    for (tile in currentlyFloatingTiles) {
        if (!tile.data.bl.exists && !tile.data.br.exists && !tile.data.bm.exists) {
            tile.destructionTime += DELTA
        }
        if (tile.destructionTime > Random.nextDouble()) {
            toRemove.add(tile)
        }
    }

    for (tile in toRemove) {
        tile.exists = false
        currentlyFloatingTiles.remove(tile)
        spawnFragment(tile.coord.cpy, Point(Random.nextDouble() - 0.5, -Random.nextDouble()), tile.tileType)
    }
}