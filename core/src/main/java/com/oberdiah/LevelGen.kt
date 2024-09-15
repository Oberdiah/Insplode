package com.oberdiah

import com.oberdiah.Utils.Perlin
import com.oberdiah.Utils.TileType

/**
 * Should only really be used to represent the tiles off the top and the bottom of the play area.
 * Probably better to use null if you've got a tile variable that may or may not exist.
 */
private val emptyTile = EmptyTile()

val levelOnScreen = sequence {
    for (sy in 0..(SQUARES_TALL * SIMPLES_RESOLUTION + 2).i) {
        for (x in 0 until SIMPLES_WIDTH) {
            val y = sy + floor(CAMERA_POS_Y * SIMPLES_RESOLUTION)
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
 */
val simplesStored = MutableList(SIMPLES_WIDTH * SIMPLES_HEIGHT_STORED) { Tile(it) }

/** If null is returned you've requested a tile outside the bounds of the stored tiles. */
fun getTile(p: Point): TileLike {
    return getTile(floor(p.x * SIMPLES_RESOLUTION), floor(p.y * SIMPLES_RESOLUTION))
}

/** If null is returned you've requested a tile outside the bounds of the stored tiles. */
fun getTile(x: Int, y: Int): TileLike {
    if (x >= SIMPLES_WIDTH || x < 0 || y >= requestedLowestSimpleY + SIMPLES_HEIGHT_STORED || y < requestedLowestSimpleY) {
        return emptyTile
    }
    val tile = simplesStored[x + (y - requestedLowestSimpleY) * SIMPLES_WIDTH]
//    require(tile.x == x && tile.y == y) {
//        "Tile not correct. Asked for $x, $y, received ${tile.x}, ${tile.y}"
//    }
    return tile
}

private val simplesStoredTopRow
    get() = simplesStored.size - SIMPLES_WIDTH until simplesStored.size

private fun shiftSimplesUp(rowsToMove: Int) {
    val amountToMove = rowsToMove * SIMPLES_WIDTH
    for (i in simplesStored.lastIndex downTo amountToMove) {
        simplesStored[i] = simplesStored[i - amountToMove]
    }
}

private fun shiftSimplesDown(rowsToMove: Int) {
    val amountToMove = rowsToMove * SIMPLES_WIDTH
    for (i in 0 until simplesStored.size - amountToMove) {
        simplesStored[i] = simplesStored[i + amountToMove]
    }
}

// Called once on app load.
fun initTiles() {
    for (tile in simplesStored) {
        tile.init()
    }
}

fun resetLevel() {
    requestedLowestSimpleY = 0
    currentLowestSimpleY = 0

    for (tile in simplesStored) {
        tile.dispose()
    }
    for (i in simplesStored.indices) {
        simplesStored[i] = Tile(i)
    }
    for (tile in simplesStored) {
        tile.init()
        generateTile(tile)
    }
}

fun requestNewLowestSimpleY(newLowest: Int) {
    requestedLowestSimpleY = min(currentLowestSimpleY, newLowest)
}

// What's been asked of us by the camera this frame
private var requestedLowestSimpleY = 0

// What the simplesStored array is actually currently using as truth
private var currentLowestSimpleY = 0

fun getLowestStoredSimpleY(): Int {
    return currentLowestSimpleY
}

fun updateLevelStorage() {
    // diff > 0, camera gone up, diff < 0, camera gone down.
    val diff = requestedLowestSimpleY - currentLowestSimpleY
    currentLowestSimpleY = requestedLowestSimpleY

    // We only generate on the way down - if stuff falls off the top it's gone for good.
    val amountToMove = abs(diff) * SIMPLES_WIDTH

    if (diff < 0) { // Camera moved down
        // Dispose of the ones at the top, we're about to get rid of them.
        for (i in simplesStored.size - amountToMove until simplesStored.size) {
            simplesStored[i].dispose()
        }

        shiftSimplesUp(abs(diff))

        // Generate new ones at the bottom.
        for (i in 0 until amountToMove) {
            simplesStored[i] = Tile(i + currentLowestSimpleY * SIMPLES_WIDTH)
        }

        // Rebuild top row neighbours (i.e let them know they no longer have anyone above them)
        for (i in simplesStoredTopRow) {
            simplesStored[i].rebuildNeighbours()
        }
        // Rebuild the row that used to be at the bottom's neighbours, as they now have new neighbours below them.
        for (i in amountToMove until amountToMove + SIMPLES_WIDTH) {
            simplesStored[i].rebuildNeighbours()
        }

        for (i in 0 until amountToMove) {
            simplesStored[i].init()
            generateTile(simplesStored[i])
        }
    } else if (diff > 0) { // Camera moved up
        // Dispose of the ones at the bottom, we're about to get rid of them.
        for (i in 0 until amountToMove) {
            simplesStored[i].dispose()
        }

        shiftSimplesDown(abs(diff))

        // Generate new ones at the top.
        for (i in simplesStored.size - amountToMove until simplesStored.size) {
            simplesStored[i] = Tile(i + currentLowestSimpleY * SIMPLES_WIDTH)
        }

        // Rebuild bottom row neighbours (i.e let them know they no longer have anyone below them)
        for (i in 0 until SIMPLES_WIDTH) {
            simplesStored[i].rebuildNeighbours()
        }

        // Rebuild the row that used to be at the top's neighbours, as they now have new neighbours above them.
        for (i in simplesStored.size - amountToMove until simplesStored.size - amountToMove + SIMPLES_WIDTH) {
            simplesStored[i].rebuildNeighbours()
        }

        for (i in simplesStored.size - amountToMove until simplesStored.size) {
            simplesStored[i].init()
        }
    }
}

fun generateTile(tile: Tile) {
    tile.materialize()

    // All coordinates here in simples coords
    val x = tile.x.d
    val y = tile.y.d

    val worldHeight = LAND_SURFACE_Y * SIMPLES_RESOLUTION - abs(Perlin.noise(x, 0, 16)) * 5
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