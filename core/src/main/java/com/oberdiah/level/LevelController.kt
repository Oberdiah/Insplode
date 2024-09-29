package com.oberdiah.level

import com.badlogic.gdx.graphics.Color
import com.oberdiah.APP_TIME
import com.oberdiah.BombType
import com.oberdiah.ClusterBomb
import com.oberdiah.DELTA
import com.oberdiah.GAME_IS_RUNNING
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.ImpactBomb
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.LAST_APP_TIME_GAME_STATE_CHANGED
import com.oberdiah.LineBomb
import com.oberdiah.NUM_TILES_ACROSS
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SAFE_BOMB_SPAWN_HEIGHT
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.Size
import com.oberdiah.SpringBomb
import com.oberdiah.StickyBomb
import com.oberdiah.TILE_SIZE_IN_UNITS
import com.oberdiah.Tile
import com.oberdiah.TimedBomb
import com.oberdiah.UNITS_WIDE
import com.oberdiah.compareTo
import com.oberdiah.createRandomFacingPoint
import com.oberdiah.d
import com.oberdiah.lerp
import com.oberdiah.max
import com.oberdiah.minus
import com.oberdiah.player.DEAD_CONTEMPLATION_TIME
import com.oberdiah.player.player
import com.oberdiah.player.playerState
import com.oberdiah.plus
import com.oberdiah.saturate
import com.oberdiah.sin
import com.oberdiah.spawnSmoke
import com.oberdiah.times
import com.oberdiah.upgrades.TOP_OF_UPGRADE_SCREEN_UNITS
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha
import kotlin.random.Random

val LASER_HEIGHT_IN_MENU: Double
    get() {
        return TOP_OF_UPGRADE_SCREEN_UNITS - 1.0
    }

val LASER_HEIGHT_START_IN_GAME = 10.0

var RUN_TIME_ELAPSED = 0.0
var gameMessage = ""
var currentPhase = 0

fun resetLevelController() {
    currentPhase = 0
    RUN_TIME_ELAPSED = 0.0
    gameMessage = ""
    maxDepthThisRun = 0.0
    currentDepthThisRun = 0.0
    laserIdealHeight = LASER_HEIGHT_START_IN_GAME
}

var currentDepthThisRun = 0.0
var maxDepthThisRun = 0.0
var laserIdealHeight = LASER_HEIGHT_START_IN_GAME
val LASER_HEIGHT: Double
    get() {
        var transition = saturate((RUN_TIME_ELAPSED - LASER_DELAY))
        if (playerState.isDead) {
            transition = saturate(1.0 - playerState.timeSinceDied / (DEAD_CONTEMPLATION_TIME - 1.0))
        } else if (GAME_STATE == GameState.TransitioningToDiegeticMenu) {
            val timeSinceTransition = APP_TIME - LAST_APP_TIME_GAME_STATE_CHANGED
            transition = saturate(1.0 - timeSinceTransition)
        }

        return lerp(LASER_HEIGHT_IN_MENU, laserIdealHeight, transition)
    }
const val LASER_SPEED = 0.3
const val LASER_WIDTH = 0.15
const val LASER_DELAY = 3.0

fun renderLaser(r: Renderer) {
    // We need a fairly good buffer because the particles take time to spawn
    if (LASER_HEIGHT > JUST_UP_OFF_SCREEN_UNITS + 5) return

    val startPoint = Point(0, LASER_HEIGHT)
    val endPoint = Point(UNITS_WIDE, LASER_HEIGHT)

    val width = lerp(1.0, 0.8, sin(RUN_TIME_ELAPSED * 8.241))
    r.color = colorScheme.laserColor1
    r.line(startPoint, endPoint, LASER_WIDTH * width)

    val width2 = lerp(1.0, 0.8, sin(RUN_TIME_ELAPSED * 10.0))
    r.color = Color.WHITE.withAlpha(0.5)
    r.line(startPoint, endPoint, LASER_WIDTH * 0.75 * width2)

    r.color = Color.BLACK.cpy().add(0.1f, 0.1f, 0.1f, 0.0f)
    r.rect(
        startPoint,
        Size(endPoint.x - startPoint.x, SCREEN_HEIGHT_IN_UNITS),
    )

    for (i in 0 until 10) {
        val particlePos = Point(Random.nextDouble(startPoint.x, endPoint.x), LASER_HEIGHT)
        val destroyingTileType = getTile(particlePos).getTileType()

        var color = colorScheme.laserParticleColors.random()
        if (destroyingTileType != TileType.Air) {
            color = destroyingTileType.color()
        }

        spawnSmoke(
            particlePos,
            createRandomFacingPoint() + Point(0.0, 0.5),
            color = color.cpy(),
            gravityScaling = 0.0,
            canCollide = false
        )
    }
}

fun tickLevelController() {
    RUN_TIME_ELAPSED += DELTA

    val lastLaserHeight = LASER_HEIGHT
    if (RUN_TIME_ELAPSED > LASER_DELAY) {
        laserIdealHeight -= DELTA * LASER_SPEED
    }

    if (getTileId(Point(0, LASER_HEIGHT)) != getTileId(Point(0, lastLaserHeight))) {
        // Delete this entire row of tiles
        for (x in 0 until NUM_TILES_ACROSS) {
            val tile =
                getTile(
                    Point(
                        (x.d + 0.5) * TILE_SIZE_IN_UNITS,
                        LASER_HEIGHT + TILE_SIZE_IN_UNITS * 2.0
                    )
                )
            if (tile is Tile && tile.doesExist()) {
                tile.dematerialize()
            }
        }
    }

    currentDepthThisRun = max(player.body.p.y, 0.0)
    maxDepthThisRun = max(currentDepthThisRun, maxDepthThisRun)

    bombDropData.forEach { (bombType, bombData) ->
        if (RUN_TIME_ELAPSED > bombData.nextBombAt) {
            bombData.randomiseNextBombAt()
            spawnBomb(bombType)
        }
    }

    var goalTime = 0.0
    phases.forEachIndexed { index, phase ->
        if (index == currentPhase) {
            if (RUN_TIME_ELAPSED > goalTime) {
                phase.callback()
                currentPhase++
            }
        }
        goalTime += phase.d.d
    }
}

fun spawnBomb(type: BombType, fraction: Number = Random.nextDouble(0.05, 0.95)) {
    val pos = Point(fraction * UNITS_WIDE, SAFE_BOMB_SPAWN_HEIGHT)
    when (type) {
        BombType.SmallTimed -> TimedBomb(pos, type)
        BombType.MediumTimed -> TimedBomb(pos, type)
        BombType.LargeTimed -> TimedBomb(pos, type)
        BombType.MegaTimed -> TimedBomb(pos, type)
        BombType.LineBomb -> LineBomb(pos)
        BombType.ClusterBomb -> ClusterBomb(pos)
        BombType.StickyBomb -> StickyBomb(pos)
        BombType.ImpactBomb -> ImpactBomb(pos)
        BombType.SpringBomb -> SpringBomb(pos)
        else -> throw Exception()
    }
}

data class BombData(var delay: Number) {
    var nextBombAt: Number = 0.0

    init {
        randomiseNextBombAt()
    }

    fun randomiseNextBombAt() {
        val minTime = 0.0
        val maxTime = delay * 2.0
        nextBombAt = RUN_TIME_ELAPSED + minTime + (maxTime - minTime) * Random.nextDouble()
    }
}

val bombDropData = mutableMapOf<BombType, BombData>()

fun startRandomBombs(type: BombType, delay: Number) {
    bombDropData[type] = BombData(delay)
}

fun stopAllBombs() {
    bombDropData.clear()
}

fun stopRandomBombs(type: BombType) {
    bombDropData.remove(type)
}