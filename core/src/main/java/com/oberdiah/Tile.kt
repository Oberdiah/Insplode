package com.oberdiah

import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.oberdiah.Utils.TileType

class TileData(var tile: Tile) {
    private val x = tile.x
    private val y = tile.y

    val bl = getTile(x - 1, y - 1)
    val bm = getTile(x, y - 1)
    val br = getTile(x + 1, y - 1)
    val rm = getTile(x + 1, y)
    val tr = getTile(x + 1, y + 1)
    val tm = getTile(x, y + 1)
    val tl = getTile(x - 1, y + 1)
    val lm = getTile(x - 1, y)

    val allSurroundingTiles = listOf(bl, bm, br, rm, tr, tm, tl, lm)
    val marchingCubeNeighbors = listOf(bl, bm, lm)
    val intCoordinateNeighbors = listOf(tr, tm, rm)
}

interface TileLike {
    /** Does this tile physically exist; can you collide with it and see it? */
    fun doesExist(): Boolean

    /** What type of tile is this? If it doesn't exist this will always return air. */
    fun getTileType(): TileType
}

// Useful so we can write methods that don't care about the difference between a tile and an empty
// space outside the world.
class EmptyTile : TileLike {
    override fun doesExist(): Boolean {
        return false
    }

    override fun getTileType(): TileType {
        return TileType.Air
    }
}

/**
 * Low IDs are at the bottom of the screen, increasing as we go up.
 *
 * IDs are nearly always negative, as they're all underground.
 */
@JvmInline
value class TileId(val id: Int) {
    companion object {
        fun fromXY(x: Int, y: Int): TileId {
            return TileId(x + y * SIMPLES_WIDTH)
        }
    }

    val x: Int
        get() {
            return (id % SIMPLES_WIDTH + SIMPLES_WIDTH) % SIMPLES_WIDTH
        }

    val y: Int
        get() {
            return floor(id.d / SIMPLES_WIDTH)
        }
}

/**
 * This is the list of tiles that have changed in this frame, directly or indirectly
 * (a neighbour changed and we may have to recompute our shape).
 */
private var tileIdsChangedInThisFrame = mutableSetOf<TileId>()

/**
 * We keep this around so people can be sure they deal with a full frame's worth of changes,
 * no matter the order of things
 */
var tileIdsChangedLastFrame = setOf<TileId>()
    private set

fun forceFullScreenRefresh() {
    tileIdsChangedInThisFrame = simplesStored.map(Tile::getId).toMutableSet()
    updateTileChanges()
}

fun updateTileChanges() {
    tileIdsChangedLastFrame = tileIdsChangedInThisFrame
    tileIdsChangedInThisFrame = mutableSetOf()
}

/**
 * The way tiles work conceptually is they're a 2D array of spaces on the grid. They may or
 * may not have a presence in the world. A 'tile' object represents a single space on the grid,
 * and we create and destroy tiles as we move further down. We do not create tiles for any other reason.
 *
 * A low id is at the bottom of the screen, increasing as we go up.
 * IDs are nearly always negative, as they're all underground.
 */
class Tile(private val id: TileId) : TileLike {
    override fun toString(): String {
        return "${if (doesExistPhysically) tileType else "Air"}: ($x, $y)"
    }

    fun getId(): TileId {
        return id
    }

    /**
     * The tile 'exists' property tracks whether the tile has a physical presence in the world.
     * If exists is true, the body is guaranteed to have a fixture. The opposite is not always the case,
     * as we do fixture cleanup at the end of the tick.
     */
    private var doesExistPhysically: Boolean = false
    private var tileType = TileType.Grass

    /**
     * This purely exists to make sure we don't accidentally use a tile that's been disposed of
     * or not initialized.
     * It cannot be referenced outside this class, and should not be relied upon.
     */
    private var isSafe = true

    /** Register this tile as now materially existing. It will collide, it will render, etc. */
    fun materialize() {
        setExists(true)
    }

    /** Register this tile as no longer materially existing. It will not collide, it will not render, etc. */
    fun dematerialize() {
        setExists(false)
    }

    private fun setExists(exists: Boolean) {
        if (this.doesExistPhysically != exists) {
            tileIdsChangedInThisFrame.add(this.id)

            this.marchingCubeNeighbors.forEach {
                if (it is Tile) {
                    tileIdsChangedInThisFrame.add(it.id)
                }
            }
        }
        this.doesExistPhysically = exists
    }

    override fun doesExist(): Boolean {
        return doesExistPhysically
    }

    fun setTileType(tileType: TileType) {
        require(tileType != TileType.Air) { "Cannot set a tile to air." }
        this.tileType = tileType
    }

    override fun getTileType(): TileType {
        return tileType
        // return if (exists) tileType else TileType.Air
    }

    var destructionTime = 0.0

    lateinit var data: TileData
    private lateinit var body: PhysBody

    val marchingCubeNeighbors: List<TileLike>
        get() {
            require(isSafe)
            if (DEBUG_VERIFY) {
                // Verify each of the neighbors is also safe
                data.marchingCubeNeighbors.forEach {
                    require(it !is Tile || it.isSafe) {
                        "Tile $this has a marching cube neighbor that is not safe: $it"
                    }
                }
            }
            return data.marchingCubeNeighbors
        }

    val allSurroundingTiles: List<TileLike>
        get() {
            require(isSafe)
            if (DEBUG_VERIFY) {
                // Verify each of the neighbors is also safe
                data.allSurroundingTiles.forEach {
                    require(it !is Tile || it.isSafe) {
                        "Tile $this has a surrounding tile that is not safe: $it"
                    }
                }
            }
            return data.allSurroundingTiles
        }

    fun rebuildNeighbours() {
        data = TileData(this)
    }

    fun init() {
        rebuildNeighbours()
        val groundBodyDef = BodyDef()
        groundBodyDef.position.set(x * SIMPLE_SIZE_IN_WORLD.f, y * SIMPLE_SIZE_IN_WORLD.f)
        body = createBody(groundBodyDef, false)
        body.userData = this
    }

    /** Call only if you're certain this tile will never be used again. */
    fun dispose() {
        body.destroy()
        isSafe = false
    }

    private var _coord = Point()
    val coord: Point
        get() {
            require(isSafe)
            _coord.x = x.d * SIMPLE_SIZE_IN_WORLD
            _coord.y = y.d * SIMPLE_SIZE_IN_WORLD
            return _coord
        }

    val x: Int
        get() {
            require(isSafe)
            return id.x
        }

    val y: Int
        get() {
            require(isSafe)
            return id.y
        }

    private var _rect = Rect(Point(), Size(SIMPLE_SIZE_IN_WORLD, SIMPLE_SIZE_IN_WORLD))
    val rect: Rect
        get() {
            require(isSafe)
            _rect.p = coord
            return _rect
        }

    fun recalculatePhysicsBody() {
        body.removeAllFixtures()

        val bottomLeft = this
        val topLeft = data.tm
        val bottomRight = data.rm
        val topRight = data.tr

        val bl = bottomLeft.doesExist()
        val tl = topLeft.doesExist()
        val br = bottomRight.doesExist()
        val tr = topRight.doesExist()
        if (bl || br || tl || tr) {
            val fa = marchingSquaresFAScaled(bl, br, tl, tr)
            val groundBox = PolygonShape()
            groundBox.set(fa)
            body.addFixture(groundBox, 0.0f)
            groundBox.dispose()
        }
    }
}