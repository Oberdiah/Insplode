package com.oberdiah.level

import com.badlogic.gdx.graphics.Color
import com.oberdiah.BombType
import com.oberdiah.CAMERA_POS_Y
import com.oberdiah.ClusterBomb
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.ImpactBomb
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.LineBomb
import com.oberdiah.NUM_TILES_ACROSS
import com.oberdiah.Point
import com.oberdiah.PointOrbs
import com.oberdiah.Rect
import com.oberdiah.Renderer
import com.oberdiah.SAFE_BOMB_SPAWN_HEIGHT
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.ScoreSystem
import com.oberdiah.Size
import com.oberdiah.SpringBomb
import com.oberdiah.StickyBomb
import com.oberdiah.TILE_SIZE_IN_UNITS
import com.oberdiah.Tile
import com.oberdiah.TimedBomb
import com.oberdiah.UNITS_WIDE
import com.oberdiah.abs
import com.oberdiah.createRandomFacingPoint
import com.oberdiah.currentlyPlayingUpgrade
import com.oberdiah.d
import com.oberdiah.f
import com.oberdiah.lerp
import com.oberdiah.max
import com.oberdiah.min
import com.oberdiah.player.DEAD_CONTEMPLATION_TIME
import com.oberdiah.player.PlayerInfoBoard
import com.oberdiah.player.player
import com.oberdiah.saturate
import com.oberdiah.sin
import com.oberdiah.spawnSmoke
import com.oberdiah.statefulEasyMode
import com.oberdiah.times
import com.oberdiah.ui.Banner
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.renderButton
import com.oberdiah.utils.vibrate
import com.oberdiah.withAlpha
import kotlin.random.Random

val LASER_HEIGHT_IN_MENU: Double
    get() {
        val finalRunFogClamp = if (ScoreSystem.playerHasFinishedTheGame()) {
            9999.0
        } else {
            UpgradeController.getUpgradeYPos(Upgrade.FinalRun) + UpgradeController.UPGRADE_ENTRY_HEIGHT
        }

        return min(
            min(
                UpgradeController.yPosOfTopOfHighestUpgrade() + UpgradeController.UPGRADE_ENTRY_HEIGHT * 3.0,
                finalRunFogClamp
            ),
            UpgradeController.TOP_OF_UPGRADE_SCREEN_UNITS
        )
    }

private val LASER_HEIGHT_START_IN_GAME
    get() = UpgradeController.getLaserStartHeight()

/** This only starts ticking up once the player hits the ground */
var RUN_TIME_ELAPSED = 0.0
private var currentPhase = 0
private var bombRandoms = mutableMapOf<BombType, Random>()

var currentDepthThisRun = 0.0

var maxDepthThisRun = 0.0
var laserInGameHeight = LASER_HEIGHT_START_IN_GAME
const val TIME_FOR_LASER_TO_DESCEND = 1.0
val LASER_HEIGHT: Double
    get() {
        var transition = saturate(RUN_TIME_ELAPSED * TIME_FOR_LASER_TO_DESCEND)
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
var APP_TIME_GAME_STARTED = 0.0
    private set

fun resetLevelController() {
    currentPhase = 0
    RUN_TIME_ELAPSED = 0.0
    maxDepthThisRun = 0.0
    currentDepthThisRun = 0.0
    laserInGameHeight = LASER_HEIGHT_START_IN_GAME
    bombRandoms.clear()
    gameHasReallyStarted = false
    bombDropData.clear()
    APP_TIME_GAME_STARTED = GameTime.APP_TIME
    tutorialButtonOffset = -5.0
}

fun renderLevelController(r: Renderer) {
    // We need a fairly good buffer because the particles take time to spawn
    if (LASER_HEIGHT > JUST_UP_OFF_SCREEN_UNITS + 5) {
        if (inMovementTutorial || inJumpTutorial || inSlamTutorial) {
            var isButtonPressed = false

            TOUCHES_DOWN.forEach {
                if (getFinishTutorialRect().contains(it.wo)) {
                    isButtonPressed = true
                }
            }

            TOUCHES_WENT_UP.forEach {
                if (getFinishTutorialRect().contains(it.wo)) {
                    if (inMovementTutorial) {
                        shownTutorialForMove.value = true
                    } else if (inJumpTutorial) {
                        shownTutorialForJump.value = true
                    } else if (inSlamTutorial) {
                        shownTutorialForSlam.value = true
                    }
                    vibrate(10)
                    gameHasReallyStarted = true
                    Banner.dismissBanner()
                }
            }

            renderButton(
                r,
                getFinishTutorialRect(),
                isButtonPressed,
                "Let's play!",
            )
        }

        return
    }

    val startPoint = Point(0, LASER_HEIGHT)
    val endPoint = Point(UNITS_WIDE, LASER_HEIGHT)

    val width = lerp(1.0, 0.8, sin(RUN_TIME_ELAPSED * 8.241))
    r.color = colorScheme.laserColor1
    r.line(startPoint, endPoint, LASER_WIDTH * width)

    val width2 = lerp(1.0, 0.8, sin(RUN_TIME_ELAPSED * 10.0))
    r.color = Color.WHITE.withAlpha(0.5)
    r.line(startPoint, endPoint, LASER_WIDTH * 0.75 * width2)

    val red = 1f - saturate((GameTime.APP_TIME - lastTimeSlamHappened) * 4.0).f

    r.color = Color.BLACK.cpy().add(0.1f + red, 0.1f + red * 0.5f, 0.1f + red * 0.5f, 0.0f)
    r.rect(
        startPoint,
        Size(endPoint.x - startPoint.x, SCREEN_HEIGHT_IN_UNITS * 2.0),
    )

    if (GAME_STATE != GameState.PausedPopup) {
        for (i in 0 until 10) {
            val particlePos = Point(Random.nextDouble(startPoint.x, endPoint.x), LASER_HEIGHT)
            val destroyingTileType = Level.getTile(particlePos).getTileType()

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

private fun getFinishTutorialRect(): Rect {
    return Rect.centered(
        Point(SCREEN_WIDTH_IN_UNITS / 2.0, min(CAMERA_POS_Y + 3.0 + tutorialButtonOffset, -1.0)),
        Size(5.0, 1.5),
    )
}

val shownTutorialForMove = StatefulBoolean("shownTutorialForMove", false)
val shownTutorialForJump = StatefulBoolean("shownTutorialForJump", false)
val shownTutorialForSlam = StatefulBoolean("shownTutorialForSlam", false)

fun hasShownAnyTutorials(): Boolean {
    return shownTutorialForMove.value || shownTutorialForJump.value || shownTutorialForSlam.value
}

fun resetTutorialSettings() {
    shownTutorialForMove.value = false
    shownTutorialForJump.value = false
    shownTutorialForSlam.value = false
}

private var lastTimeSlamHappened = 0.0
fun playerHasSlammed(laserPushBackMultiplier: Double) {
    laserInGameHeight += 0.5 * laserPushBackMultiplier
    lastTimeSlamHappened = GameTime.APP_TIME
}

var gameHasReallyStarted = false
    private set

private val inMovementTutorial
    get() = currentlyPlayingUpgrade.value == Upgrade.Movement &&
            !shownTutorialForMove.value &&
            !gameHasReallyStarted

private val inJumpTutorial
    get() = currentlyPlayingUpgrade.value == Upgrade.Jump &&
            !shownTutorialForJump.value &&
            !gameHasReallyStarted

private val inSlamTutorial
    get() = currentlyPlayingUpgrade.value == Upgrade.Slam &&
            !shownTutorialForSlam.value &&
            !gameHasReallyStarted

private var tutorialButtonOffset = -5.0

fun tickLevelController() {
    if (!gameHasReallyStarted) {
        if (player.state.isIdle) {
            if (inMovementTutorial) {
                if (PlayerInfoBoard.distanceMovedLeftRightThisRun > 5.0) {
                    tutorialButtonOffset = lerp(tutorialButtonOffset, 0.0, 0.05)
                }
                Banner.showBanner(
                    "Touch and drag left and right to move",
                    keepAliveUntilDismissed = true
                )
                return
            } else if (inJumpTutorial) {
                if (PlayerInfoBoard.numTimesJumpedThisRun >= 3) {
                    tutorialButtonOffset = lerp(tutorialButtonOffset, 0.0, 0.05)
                }

                Banner.showBanner("Swipe up to jump", keepAliveUntilDismissed = true)
                return
            } else if (inSlamTutorial) {
                if (PlayerInfoBoard.numTimesSlammedThisRun >= 3) {
                    tutorialButtonOffset = lerp(tutorialButtonOffset, 0.0, 0.05)
                }

                Banner.showBanner("Swipe down in the air to slam", keepAliveUntilDismissed = true)
                return
            } else {
                gameHasReallyStarted = true
                Banner.dismissBanner()
            }
        } else {
            return
        }
    }

    val deltaScaling = if (statefulEasyMode.value) {
        0.5
    } else {
        1.0
    }

    RUN_TIME_ELAPSED += GameTime.GAMEPLAY_DELTA * deltaScaling

    val lastLaserHeight = LASER_HEIGHT
    val laserSpeedMultiplier =
        if (RUN_TIME_ELAPSED < 100.0) 1.0 + RUN_TIME_ELAPSED / 100.0 else 1.0 + RUN_TIME_ELAPSED / 50
    laserInGameHeight -= GameTime.GAMEPLAY_DELTA * UpgradeController.getLaserSpeed() * deltaScaling * laserSpeedMultiplier

    if (Level.getTileId(Point(0, LASER_HEIGHT)) != Level.getTileId(Point(0, lastLaserHeight))) {
        // Delete this entire row of tiles
        for (x in 0 until NUM_TILES_ACROSS) {
            val tile =
                Level.getTile(
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


    val playerHeight = player.body.p.y
    var goalTime = 0.0
    phases.forEachIndexed { index, phase ->
        if (playerHeight.abs > (phase.expectedDepthUnits ?: 9999.0) && index > currentPhase) {
            currentPhase = index
            goalTime += phases[currentPhase].d.d
            phases[currentPhase].callback()
        }
        if (index == currentPhase) {
            if (RUN_TIME_ELAPSED > goalTime) {
                phase.callback()
                currentPhase++
            }
        }
        goalTime += phase.d.d
    }
}

fun spawnBomb(type: BombType, fraction: Double = getBombRandom(type).nextDouble(0.05, 0.95)) {
    if ((LASER_HEIGHT - laserInGameHeight).abs > 0.2) {
        println("WARNING: Bomb spawned at wrong height $LASER_HEIGHT vs $laserInGameHeight")
    }

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
            PointOrbs.spawnOrbs(pos, 1, addRandomVelocity = false, canBePickedUpInstantly = true)
        }

        // \/ \/ \/ Unused \/ \/ \/
        BombType.StickyBomb -> StickyBomb(pos)
        BombType.ClusterBomb -> ClusterBomb(pos)
        BombType.ImpactBomb -> ImpactBomb(pos)
        else -> throw Exception()
    }
}

data class BombData(var delay: Double, val type: BombType) {
    var nextBombAt: Double = 0.0

    init {
        randomiseNextBombAt()
    }

    fun randomiseNextBombAt() {
        val minTime = 0.0
        val maxTime = delay * 2.0
        nextBombAt = RUN_TIME_ELAPSED + lerp(minTime, maxTime, getBombRandom(type).nextDouble())
    }
}

val bombDropData = mutableMapOf<BombType, BombData>()

fun startRandomBombs(type: BombType, requestedDelay: Double) {
    val actualDelay = if (UpgradeController.playerHas(Upgrade.RapidBombs)) {
        requestedDelay
    } else {
        requestedDelay * 1.4
    }

    bombDropData[type]?.let {
        it.delay = actualDelay
    } ?: run {
        bombDropData[type] = BombData(actualDelay, type)
    }
}

fun stopAllBombs() {
    bombDropData.clear()
}

fun stopRandomBombs(type: BombType) {
    bombDropData.remove(type)
}

private fun getBombRandom(type: BombType): Random {
    return bombRandoms.getOrPut(type) { Random(type.ordinal) }
}