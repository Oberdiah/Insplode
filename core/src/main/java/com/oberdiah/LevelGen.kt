package com.oberdiah

import com.oberdiah.Utils.Perlin
import com.oberdiah.Utils.TileType

/**
 * Should only really be used to represent the tiles off the top and the bottom of the play area.
 * Probably better to use null if you've got a tile variable that may or may not exist.
 */
private val emptyTile = EmptyTile()

val levelOnScreen = sequence {
    for (sy in 0..(SCREEN_HEIGHT_IN_UNITS * TILES_PER_UNIT + 2).i) {
        for (x in 0 until NUM_TILES_ACROSS) {
            val y = sy + floor(CAMERA_POS_Y * TILES_PER_UNIT)
            val tile = getTile(x, y)
            if (tile is Tile) {
                yield(tile)
            }
        }
    }
}

/**
 * All tiles we're tracking.
 * This is a grid of tiles that we update as we go.
 * Tiles are only ever added or removed from this list carefully, and the only reason that would happen is
 * the camera moving down the screen.
 *
 * Low IDs (and therefor first in the list) are at the bottom of the screen, increasing as we go up.
 *
 * IDs are nearly always negative, as they're all underground.
 *
 * This should be the only persistent storage of tiles cross-frame. Anything else is
 * risky as we can invalidate tiles at any time.
 */
val tilesStorage =
    MutableList(NUM_TILES_ACROSS * SIMULATED_REGION_NUM_TILES_HIGH) { Tile(TileId(it)) }

/** If null is returned you've requested a tile outside the bounds of the stored tiles. */
fun getTile(p: Point): TileLike {
    return getTile(floor(p.x * TILES_PER_UNIT), floor(p.y * TILES_PER_UNIT))
}

/** If null is returned you've requested a tile outside the bounds of the stored tiles. */
fun getTile(x: Int, y: Int): TileLike {
    if (x >= NUM_TILES_ACROSS || x < 0 || y >= currentLowestSimpleY + SIMULATED_REGION_NUM_TILES_HIGH || y < requestedLowestSimpleY) {
        return emptyTile
    }
    val tile = tilesStorage[x + (y - currentLowestSimpleY) * NUM_TILES_ACROSS]
    if (DEBUG_VERIFY) {
        require(tile.x == x && tile.y == y) {
            "Tile not correct. Asked for $x, $y, received ${tile.x}, ${tile.y}"
        }
    }
    return tile
}

fun getTile(tileId: TileId): TileLike {
    val storageIdx = tileId.id - currentLowestSimpleY * NUM_TILES_ACROSS
    if (storageIdx >= 0 && storageIdx < tilesStorage.size) {
        return tilesStorage[storageIdx]
    }
    return emptyTile
}

private val tilesStoredTopRow
    get() = tilesStorage.size - NUM_TILES_ACROSS until tilesStorage.size

private fun shiftTilesUp(rowsToMove: Int) {
    val amountToMove = rowsToMove * NUM_TILES_ACROSS
    for (i in tilesStorage.lastIndex downTo amountToMove) {
        tilesStorage[i] = tilesStorage[i - amountToMove]
    }
}

private fun shiftTilesDown(rowsToMove: Int) {
    val amountToMove = rowsToMove * NUM_TILES_ACROSS
    for (i in 0 until tilesStorage.size - amountToMove) {
        tilesStorage[i] = tilesStorage[i + amountToMove]
    }
}

// Called once on app load.
fun initTiles() {
    for (tile in tilesStorage) {
        tile.init()
    }
}

fun resetLevel() {
    requestedLowestSimpleY = 0
    currentLowestSimpleY = 0

    for (tile in tilesStorage) {
        tile.dispose()
    }
    for (i in tilesStorage.indices) {
        tilesStorage[i] = Tile(TileId(i))
    }
    for (tile in tilesStorage) {
        tile.init()
        generateTile(tile)
    }
}

fun requestNewLowestSimpleY(newLowest: Int) {
    requestedLowestSimpleY = min(currentLowestSimpleY, newLowest)
}

// What's been asked of us by the camera this frame
private var requestedLowestSimpleY = 0

// What the tilesStored array is actually currently using as truth
private var currentLowestSimpleY = 0

fun getLowestStoredSimpleY(): Int {
    return currentLowestSimpleY
}

fun updateLevelStorage() {
    // diff > 0, camera gone up, diff < 0, camera gone down.
    val diff = requestedLowestSimpleY - currentLowestSimpleY
    currentLowestSimpleY = requestedLowestSimpleY

    // We only generate on the way down - if stuff falls off the top it's gone for good.
    val amountToMove = abs(diff) * NUM_TILES_ACROSS

    if (diff < 0) { // Camera moved down
        // Dispose of the ones at the top, we're about to get rid of them.
        for (i in tilesStorage.size - amountToMove until tilesStorage.size) {
            tilesStorage[i].dispose()
        }

        shiftTilesUp(abs(diff))

        // Generate new ones at the bottom.
        for (i in 0 until amountToMove) {
            tilesStorage[i] = Tile(TileId(i + currentLowestSimpleY * NUM_TILES_ACROSS))
        }

        // Rebuild top row neighbours (i.e let them know they no longer have anyone above them)
        for (i in tilesStoredTopRow) {
            tilesStorage[i].rebuildNeighbours()
            tilesStorage[i].forceUpdate()
        }
        // Rebuild the row that used to be at the bottom's neighbours, as they now have new neighbours below them.
        for (i in amountToMove until amountToMove + NUM_TILES_ACROSS) {
            tilesStorage[i].rebuildNeighbours()
        }

        for (i in 0 until amountToMove) {
            tilesStorage[i].init()
            // GenerateTile forces the update for us.
            generateTile(tilesStorage[i])
        }
    } else if (diff > 0) { // Camera moved up
        // Dispose of the ones at the bottom, we're about to get rid of them.
        for (i in 0 until amountToMove) {
            tilesStorage[i].dispose()
        }

        shiftTilesDown(abs(diff))

        // Generate new ones at the top.
        for (i in tilesStorage.size - amountToMove until tilesStorage.size) {
            tilesStorage[i] = Tile(TileId(i + currentLowestSimpleY * NUM_TILES_ACROSS))
        }

        // Rebuild bottom row neighbours (i.e let them know they no longer have anyone below them)
        for (i in 0 until NUM_TILES_ACROSS) {
            tilesStorage[i].rebuildNeighbours()
        }

        // Rebuild the row that used to be at the top's neighbours, as they now have new neighbours above them.
        for (i in tilesStorage.size - amountToMove until tilesStorage.size - amountToMove + NUM_TILES_ACROSS) {
            tilesStorage[i].rebuildNeighbours()
        }

        for (i in tilesStorage.size - amountToMove until tilesStorage.size) {
            tilesStorage[i].init()
        }
    }
}

fun generateTile(tile: Tile) {
    tile.materialize()

    // All coordinates here in tiles coords
    val x = tile.x.d
    val y = tile.y.d

    val worldHeight = LAND_SURFACE_Y * TILES_PER_UNIT - abs(Perlin.noise(x, 0, 16)) * 5
    val depth = worldHeight - y
    if (depth < 0) {
        tile.dematerialize()
        return
    }

    val grassDepth = Perlin.noise(x, 0, 3) * 2.0 + 5.0
    if (grassDepth > depth) {
        tile.setTileType(TileType.Grass)
    } else {
        val caveNoise = Perlin.noise(x, y / 3, 6.0)
        val caveVal = -1.4 + min(depth * 0.03, 1.0)
        val caveStone = caveNoise < caveVal + 0.3
        val randoStone = Perlin.noise(x / 5, y + 2839675, 7.0) > 0.3
        if (caveStone || randoStone) {
            tile.setTileType(TileType.Stone)
        } else {
            tile.setTileType(TileType.Dirt)
        }
        if (caveNoise < caveVal) {
            tile.dematerialize()
        }
    }
}