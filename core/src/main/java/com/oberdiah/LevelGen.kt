package com.oberdiah

import com.oberdiah.Utils.Perlin
import com.oberdiah.Utils.TileType

val nonTile = Tile(Int.MAX_VALUE)

val levelOnScreen = sequence {
    for (sy in 0..(SQUARES_TALL * SIMPLES_RESOLUTION + 2).i) {
        for (x in 0 until SIMPLES_WIDTH) {
            val y = sy + floor(CAMERA_POS_Y * SIMPLES_RESOLUTION)
            val tile = getTile(x, y)
            if (tile != nonTile) {
                yield(tile)
            }
        }
    }
}

fun getTile(p: Point): Tile {
    return getTile(floor(p.x * SIMPLES_RESOLUTION), floor(p.y * SIMPLES_RESOLUTION))
}

val simplesStored = MutableList(SIMPLES_WIDTH * SIMPLES_HEIGHT_STORED) { Tile(it) }

fun getTile(x: Int, y: Int): Tile {
    if (x >= SIMPLES_WIDTH || x < 0 || y >= LOWEST_SIMPLE_Y_STORED + SIMPLES_HEIGHT_STORED || y < LOWEST_SIMPLE_Y_STORED) {
        return nonTile
    }
    val tile = simplesStored[x + (y - LOWEST_SIMPLE_Y_STORED) * SIMPLES_WIDTH]
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

fun resetLevel() {
    LOWEST_SIMPLE_Y_STORED = 0
    previouSlowestSimpleYStored = 0

    for (tile in simplesStored) {
        if (tile.initialised) {
            tile.body.destroy()
        }
    }
    for (i in simplesStored.indices) {
        simplesStored[i] = Tile(i)
    }
    for (tile in simplesStored) {
        tile.init()
        generateTile(tile)
    }
}

private var previouSlowestSimpleYStored = 0
fun updateLevelStorage() {
    // diff > 0, camera gone up, diff < 0, camera gone down.
    val diff = LOWEST_SIMPLE_Y_STORED - previouSlowestSimpleYStored

    // We only generate on the way down - if stuff falls off the top it's gone for good.
    // TODO Reuse could be done here - the bodies could be reused - the entire tile could be reused
    // I didn't because I wanted to get it working.
    val amountToMove = abs(diff) * SIMPLES_WIDTH
    if (diff < 0) {
        for (i in simplesStored.size - amountToMove until simplesStored.size) {
            if (simplesStored[i].isSafe) {
                simplesStored[i].body.destroy()
            }
        }

        shiftSimplesUp(abs(diff))

        for (i in 0 until amountToMove) {
            simplesStored[i] = Tile(i + LOWEST_SIMPLE_Y_STORED * SIMPLES_WIDTH)
        }

        // TODO might be worth figuring out how to do this properly so we didn't spam objects into the GC
        // Rebuild top row neighbours
        for (i in simplesStoredTopRow) {
            if (simplesStored[i].isSafe) {
                simplesStored[i].rebuildNeighbours()
            }
        }
        // Rebuild (old) bottom row neighbours
        for (i in amountToMove until amountToMove + SIMPLES_WIDTH) {
            if (simplesStored[i].isSafe) {
                simplesStored[i].rebuildNeighbours()
            }
        }

        for (i in 0 until amountToMove) {
            simplesStored[i].init()
            generateTile(simplesStored[i])
        }
    } else if (diff > 0) {
        for (i in 0 until amountToMove) {
            if (simplesStored[i].isSafe) {
                simplesStored[i].body.destroy()
            }
        }

        shiftSimplesDown(abs(diff))

        for (i in simplesStoredTopRow) {
            simplesStored[i] = nonTile
        }
    }

    require(simplesStored[0].isNAT || simplesStored[0].y == LOWEST_SIMPLE_Y_STORED)
    require(simplesStored[simplesStored.lastIndex].isNAT || simplesStored[simplesStored.lastIndex].y == LOWEST_SIMPLE_Y_STORED + SIMPLES_HEIGHT_STORED - 1)

    previouSlowestSimpleYStored = LOWEST_SIMPLE_Y_STORED
}

fun generateTile(tile: Tile) {
    require(tile.isSafe)

    tile.exists = true

    // All coordinates here in simples coords
    val x = tile.x.d
    val y = tile.y.d

    val worldHeight = LAND_SURFACE_Y * SIMPLES_RESOLUTION - abs(Perlin.noise(x, 0, 16)) * 5
    val depth = worldHeight - y
    if (depth < 0) {
        tile.exists = false
        return
    }

    val grassDepth = Perlin.noise(x, 0, 3) * 2.0 + 5.0
    if (grassDepth > depth) {
        tile.tileType = TileType.Grass
    } else {
        val caveNoise = Perlin.noise(x, y / 3, 6.0)
        val caveVal = -1.4 + min(depth * 0.03, 1.0)
        val caveStone = caveNoise < caveVal + 0.3
        val randoStone = Perlin.noise(x / 5, y + 2839675, 7.0) > 0.3
        if (caveStone || randoStone) {
            tile.tileType = TileType.Stone
        } else {
            tile.tileType = TileType.Dirt
        }
        if (caveNoise < caveVal) {
            tile.exists = false
        }
    }
}