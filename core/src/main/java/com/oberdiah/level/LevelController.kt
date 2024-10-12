package com.oberdiah.level

import com.badlogic.gdx.graphics.Color
import com.oberdiah.BombType
import com.oberdiah.ClusterBomb
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.ImpactBomb
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.LineBomb
import com.oberdiah.NUM_TILES_ACROSS
import com.oberdiah.Point
import com.oberdiah.PointOrbs
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
import com.oberdiah.min
import com.oberdiah.player.DEAD_CONTEMPLATION_TIME
import com.oberdiah.player.player
import com.oberdiah.saturate
import com.oberdiah.sin
import com.oberdiah.spawnSmoke
import com.oberdiah.statefulEasyMode
import com.oberdiah.times
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha
import kotlin.random.Random

val LASER_HEIGHT_IN_MENU: Double
    get() {
        return min(
            UpgradeController.yPosOfTopOfHighestUpgrade() + UpgradeController.UPGRADE_ENTRY_HEIGHT * 3.0,
            UpgradeController.TOP_OF_UPGRADE_SCREEN_UNITS
        )
    }

val LASER_HEIGHT_START_IN_GAME
    get() = UpgradeController.getLaserStartHeight()

var RUN_TIME_ELAPSED = 0.0
var gameMessage = ""
var currentPhase = 0
var random = Random(0)

fun resetLevelController() {
    currentPhase = 0
    RUN_TIME_ELAPSED = 0.0
    gameMessage = ""
    maxDepthThisRun = 0.0
    currentDepthThisRun = 0.0
    laserInGameHeight = LASER_HEIGHT_START_IN_GAME
    random = Random(0)
    bombDropData.clear()
}

var currentDepthThisRun = 0.0
var maxDepthThisRun = 0.0
var laserInGameHeight = LASER_HEIGHT_START_IN_GAME
val LASER_HEIGHT: Double
    get() {
        var transition = saturate((RUN_TIME_ELAPSED - LASER_DELAY))
        if (player.state.isDead) {
            transition = min(
                transition,
                saturate(1.0 - player.state.timeSinceDied / (DEAD_CONTEMPLATION_TIME - 1.0))
            )
        } else if (GAME_STATE == GameState.TransitioningToDiegeticMenu) {
            transition = 0.0
        }

        return lerp(LASER_HEIGHT_IN_MENU, laserInGameHeight, transition)
    }
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

    if (GAME_STATE != GameState.PausedPopup) {
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
}

fun tickLevelController() {
    val deltaScaling = if (statefulEasyMode.value) {
        0.5
    } else {
        1.0
    }

    RUN_TIME_ELAPSED += GameTime.GAMEPLAY_DELTA * deltaScaling

    val lastLaserHeight = LASER_HEIGHT
    if (RUN_TIME_ELAPSED > LASER_DELAY) {
        laserInGameHeight -= GameTime.GAMEPLAY_DELTA * UpgradeController.getLaserSpeed() * deltaScaling
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
                tile.dematerialize(reason = Tile.DematerializeReason.Laser)
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

fun spawnBomb(type: BombType, fraction: Number = random.nextDouble(0.05, 0.95)) {
    val pos = Point(fraction * UNITS_WIDE, SAFE_BOMB_SPAWN_HEIGHT)
    when (type) {
        BombType.SmallTimed -> {
            if (UpgradeController.playerHas(Upgrade.SmallTimedBomb)) {
                TimedBomb(pos, type)
            }
        }

        BombType.MediumTimed -> {
            if (UpgradeController.playerHas(Upgrade.MediumTimedBomb)) {
                TimedBomb(pos, type)
            }
        }

        BombType.LargeTimed -> {
            if (UpgradeController.playerHas(Upgrade.LargeTimedBomb)) {
                TimedBomb(pos, type)
            }
        }

        BombType.MegaTimed -> {
            if (UpgradeController.playerHas(Upgrade.MegaTimedBomb)) {
                TimedBomb(pos, type)
            }
        }

        BombType.UltraTimed -> {
            if (UpgradeController.playerHas(Upgrade.UltraTimedBomb)) {
                TimedBomb(pos, type)
            }
        }

        BombType.LineBomb -> {
            if (UpgradeController.playerHas(Upgrade.LineBomb)) {
                LineBomb(pos)
            }
        }

        BombType.SpringBomb -> {
            if (UpgradeController.playerHas(Upgrade.SpringBomb)) {
                SpringBomb(pos)
            }
        }

        BombType.PointOrb -> {
            PointOrbs.spawnOrbs(pos, 1, addRandomVelocity = false)
        }

        // \/ \/ \/ Unused \/ \/ \/
        BombType.StickyBomb -> StickyBomb(pos)
        BombType.ClusterBomb -> ClusterBomb(pos)
        BombType.ImpactBomb -> ImpactBomb(pos)
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
        nextBombAt = RUN_TIME_ELAPSED + lerp(minTime, maxTime, random.nextDouble())
    }
}

val bombDropData = mutableMapOf<BombType, BombData>()

fun startRandomBombs(type: BombType, requestedDelay: Number) {
    val actualDelay = if (UpgradeController.playerHas(Upgrade.RapidBombs)) {
        requestedDelay * 0.8
    } else {
        requestedDelay
    }

    bombDropData[type]?.let {
        it.delay = actualDelay
    } ?: run {
        bombDropData[type] = BombData(actualDelay)
    }
}

fun stopAllBombs() {
    bombDropData.clear()
}

fun stopRandomBombs(type: BombType) {
    bombDropData.remove(type)
}