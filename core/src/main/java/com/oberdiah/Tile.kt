package com.oberdiah

import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.oberdiah.Utils.TileType
import kotlin.random.Random

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

class Tile(private val id: Int) {
    override fun toString(): String {
        return "${if (exists) tileType else "Air"}: ($x, $y), ${if (attachedToGround) "" else "Not "}Grounded"
    }

    var exists: Boolean = false
        set(value) {
            require(!(value && isNAT))
            if (field != value) {
                changedTiles.add(this)
            }
            field = value
        }

    var tileType = TileType.Grass
        // get() = if (exists) field else TileType.Air
        set(value) {
            changedTiles.add(this)
            field = value
        }

    val isNAT
        get() = id == Int.MAX_VALUE
    val isSafe
        get() = id != Int.MAX_VALUE

    var attachedToGround = false
    var destructionTime = 0.0

    lateinit var data: TileData
    lateinit var body: PhysBody
    val rando = Random.nextDouble()

    val fixtures = mutableListOf<Fixture>()

    val marchingCubeNeighbors: List<Tile>
        get() = data.marchingCubeNeighbors

    val allSurroundingTiles: List<Tile>
        get() = data.allSurroundingTiles

    fun rebuildNeighbours() {
        data = TileData(this)
    }

    var initialised = false

    fun init() {
        require(isSafe)
        initialised = true
        rebuildNeighbours()
        val groundBodyDef = BodyDef()
        groundBodyDef.position.set(x * SIMPLE_SIZE_IN_WORLD.f, y * SIMPLE_SIZE_IN_WORLD.f)
        body = createBody(groundBodyDef, false)
        body.userData = this
    }

    private var _coord = Point()
    val coord: Point
        get() {
            _coord.x = x.d * SIMPLE_SIZE_IN_WORLD
            _coord.y = y.d * SIMPLE_SIZE_IN_WORLD
            return _coord
        }

    val x: Int
        get() {
            require(isSafe)
            return (id % SIMPLES_WIDTH + SIMPLES_WIDTH) % SIMPLES_WIDTH
        }

    val y: Int
        get() {
            require(isSafe)
            return floor(id.d / SIMPLES_WIDTH)
        }

    private var _rect = Rect(Point(), Size(SIMPLE_SIZE_IN_WORLD, SIMPLE_SIZE_IN_WORLD))
    val rect: Rect
        get() {
            _rect.p = coord
            return _rect
        }
}