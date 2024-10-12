package com.oberdiah

import com.badlogic.gdx.utils.Align
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.PLAYER_SIZE
import com.oberdiah.player.player
import com.oberdiah.ui.MENU_ZONE_BOTTOM_Y
import com.oberdiah.ui.coinAreaPosition
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.GameTime.APP_TIME
import com.oberdiah.utils.GameTime.GAMEPLAY_DELTA
import com.oberdiah.utils.GameTime.updateGameSpeed
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import kotlin.math.pow
import kotlin.random.Random

object ScoreSystem {

    var lastScore: Int? = null
        private set

    var growingScore = 0
        private set

    var bounceDecayAccumulator = 0.0
        private set

    var lastScoreCollectionTime = RUN_TIME_ELAPSED
        private set

    var growingScoreStartedOn = RUN_TIME_ELAPSED
        private set

    const val GROWING_SCORE_REFRESH_COUNTDOWN = 1.5

    private var numConsecutiveBounces = 0

    /** The near-instant delay that causes the score to come in in small bits */
    private var scoreLeftToSound = 0
    private var lastTimeScoreSounded = 0.0

    private var playerScore = 0
    private var lastScoreGivenOn = 0.0

    // In score per second
    private var scoreGivingSpeed = 0.0

    const val TIME_TO_GIVE_SCORE = 2.5

    private fun convertPlayerScoreIntoCoins(scoreToMove: Int) {
        // The only place we should be updating the coin balance
        statefulCoinBalance.value += scoreToMove
        playerScore -= scoreToMove
    }

    fun getCurrentMultiplier(): Double {
        return if (numConsecutiveBounces < 15) {
            1 + (numConsecutiveBounces * 0.1)
        } else {
            1 + numConsecutiveBounces * 0.1 + ((numConsecutiveBounces - 15.0) * 0.1).pow(1.5)
        }
    }

    /** The amount we multiply time by to speed up the game */
    fun timeWarp(): Double {
        return 1 + max((numConsecutiveBounces - 15) * 0.05, 0)
    }

    fun canStartGame(): Boolean {
        // All score has become coins
        return playerScore == 0
    }

    /** Per game-second. */
    fun bounceDecayRate(): Double {
        return clamp((numConsecutiveBounces - 10) * 0.1, 0.0, 0.5)
    }

    fun registerBombSlam(bomb: Bomb) {
        val numToNormallySpawn = bomb.getPointsWorth()
        val numToActuallySpawn = (numToNormallySpawn * getCurrentMultiplier()).i
        PointOrbs.spawnOrbs(bomb.body.p, numToActuallySpawn)
        updateGameSpeed(timeWarp())

        bounceDecayAccumulator = 0.0
        numConsecutiveBounces++

        playMultiplierSound(numConsecutiveBounces)
    }

    fun registerBombComboExplode(bomb: Bomb) {
        val numToNormallySpawn = ceil(bomb.getPointsWorth() * 0.15)
        PointOrbs.spawnOrbs(bomb.body.p, (numToNormallySpawn * getCurrentMultiplier()).i)
    }

    fun registerTileDestroyed(pos: Point, tileType: TileType, reason: Tile.DematerializeReason) {
        if (reason == Tile.DematerializeReason.Laser) {
            return
        }

        var numToSpawn = when (tileType) {
            TileType.OrbTile -> 1
            TileType.GoldenOrbTile -> 50
            else -> 0
        }

        if (reason == Tile.DematerializeReason.Collapse) {
            if (numToSpawn == 1) {
                if (Random.nextDouble() < 0.5) {
                    return
                }
            } else {
                numToSpawn /= 2
            }
        }

        PointOrbs.spawnOrbs(pos, (numToSpawn * getCurrentMultiplier()).i)
    }

    fun registerGameEnd() {
        lastScore = playerScore
        lastScoreGivenOn = APP_TIME
        scoreGivingSpeed = max(playerScore / TIME_TO_GIVE_SCORE, 4.0)

        growingScore = 0
        numConsecutiveBounces = 0
        lastScoreCollectionTime = RUN_TIME_ELAPSED
        growingScoreStartedOn = RUN_TIME_ELAPSED
        scoreLeftToSound = 0
        lastTimeScoreSounded = 0.0
    }

    fun registerGameStart() {
        if (playerScore > 0) {
            println("Warning: $playerScore was left over when launching, this shouldn't really happen?")
        }
        convertPlayerScoreIntoCoins(playerScore)
    }

    fun registerCasuallyLanded() {
        if (numConsecutiveBounces > 0) {
            playMultiplierLossSound()
        }
        bounceDecayAccumulator = 0.0
        numConsecutiveBounces = 0

        updateGameSpeed(1.0)
    }

    fun givePlayerScore(score: Int) {
        scoreLeftToSound += score
        lastTimeScoreSounded = 0.0
    }

    /**
     * We use this internally to actually give the player the score.
     * The public interface is different because we want to delay score-giving over time.
     */
    private fun givePlayerScoreInternal(score: Int) {
        playPickupSound(growingScore)
        if (growingScore == 0) {
            growingScoreStartedOn = RUN_TIME_ELAPSED
        }
        growingScore += score
        playerScore += score
        statefulHighScore.value = max(playerScore, statefulHighScore.value)
        lastScoreCollectionTime = RUN_TIME_ELAPSED
    }

    fun tick() {
        bounceDecayAccumulator += bounceDecayRate() * GAMEPLAY_DELTA

        while (bounceDecayAccumulator >= 1) {
            bounceDecayAccumulator -= 1
            numConsecutiveBounces = max(numConsecutiveBounces - 1, 0)
            updateGameSpeed(timeWarp())
        }

        if (GAME_STATE == GameState.DiegeticMenu) {
            val delayBetweenScoreGiving = 1 / scoreGivingSpeed
            var scoreToMove = 1
            if (delayBetweenScoreGiving < GameTime.GRAPHICS_DELTA) {
                scoreToMove = min(
                    (GameTime.GRAPHICS_DELTA / delayBetweenScoreGiving).i,
                    playerScore
                )
            }
            if (playerScore > 0 && lastScoreGivenOn + delayBetweenScoreGiving < APP_TIME) {
                convertPlayerScoreIntoCoins(scoreToMove)
                lastScoreGivenOn = APP_TIME
                val smokeSpawnPoint = Rect.centered(
                    Point(SCREEN_WIDTH_IN_UNITS / 2, endOfGameCoinsHeight),
                    Size(SCREEN_HEIGHT_IN_UNITS / 30, SCREEN_HEIGHT_IN_UNITS / 30)
                ).randomPointInside()

                val velocity = coinAreaPosition.wo - smokeSpawnPoint
                velocity.len = 15

                spawnSmoke(
                    smokeSpawnPoint,
                    velocity,
                    color = colorScheme.pointOrbColor.cpy().mul(Random.nextDouble(0.8, 1.2).f),
                    canCollide = false,
                    radiusScaling = 1.7,
                    gravityScaling = 0.0
                )
            }
        }

        if (scoreLeftToSound > 0) {
            val timeToNext = Random.nextDouble(0.1, 0.12) * 0.98.pow(scoreLeftToSound)
            if (lastTimeScoreSounded + timeToNext < RUN_TIME_ELAPSED) {
                val scoreToMove = 1
                givePlayerScoreInternal(scoreToMove)
                scoreLeftToSound -= scoreToMove
                lastTimeScoreSounded = RUN_TIME_ELAPSED
            }
        }

        if (RUN_TIME_ELAPSED - lastScoreCollectionTime > GROWING_SCORE_REFRESH_COUNTDOWN) {
            growingScore = 0
        }
    }

    private val endOfGameCoinsHeight
        get() = MENU_ZONE_BOTTOM_Y + SCREEN_HEIGHT_IN_UNITS * 0.5

    fun renderDiegeticText(r: Renderer) {
        val H = SCREEN_HEIGHT_IN_UNITS
        val W = SCREEN_WIDTH_IN_UNITS.d

        r.color = colorScheme.textColor

        if (statefulHighScore.value != 0) {
            r.text(
                fontSmallish,
                "High Score: ${statefulHighScore.value}",
                W / 2, MENU_ZONE_BOTTOM_Y + H * 3.0 / 4 - 2.0,
                Align.center,
                shouldCache = false
            )
        }

        val inGameScreenSpaceLocation = Point(WIDTH / 2, HEIGHT * 0.9).wo

        if (playerScore > 0) {
            r.color = colorScheme.textColor
            r.text(
                fontLarge,
                "$playerScore",
                W / 2, min(endOfGameCoinsHeight, inGameScreenSpaceLocation.y),
                Align.center,
                shouldCache = false
            )
        }
        if (lastScore != null) {
            val lastScore = lastScore
            val lastScoreOpacity = if (GAME_STATE == GameState.DiegeticMenu && lastScore != null) {
                val timeToGiveScore = lastScore / scoreGivingSpeed
                1.0 - saturate(((LAST_APP_TIME_GAME_STATE_CHANGED + timeToGiveScore) - APP_TIME) * 3.0)
            } else {
                0.0
            }

            r.color = colorScheme.textColor.withAlpha(lastScoreOpacity)
            r.text(
                fontSmallish,
                "Score: $lastScore",
                W / 2, MENU_ZONE_BOTTOM_Y + H * 3.0 / 4 - 3.0,
                Align.center,
                shouldCache = false
            )
        }
    }

    private const val FADE_IN_TIME = 0.05
    private const val FADE_OUT_TIME = 0.3
    private const val HEIGHT_ABOVE_HEAD = 0.5

    fun renderFloatingPlayerText(r: Renderer) {
        if (player.state.isDead) return

        val lineHeight = fontSmall.lineHeight / UNIT_SIZE_IN_PIXELS

        if (getCurrentMultiplier() > 1.0) {
            val textOffset = Point(
                0,
                PLAYER_SIZE.h + lineHeight * HEIGHT_ABOVE_HEAD
            )

            r.color = colorScheme.textColor

            r.text(
                fontSmall,
                "x${getCurrentMultiplier().format(1)}",
                player.body.p + textOffset,
                Align.center,
                shouldCache = false
            )
        }

        if (growingScore > 0) {
            val fadeInAlpha = (RUN_TIME_ELAPSED - growingScoreStartedOn) / FADE_IN_TIME
            val fadeOutAlpha =
                ((lastScoreCollectionTime + GROWING_SCORE_REFRESH_COUNTDOWN) - RUN_TIME_ELAPSED) / FADE_OUT_TIME
            val alpha = saturate(min(fadeInAlpha, fadeOutAlpha))

            val pickupMotion =
                1 - saturate((RUN_TIME_ELAPSED - lastScoreCollectionTime) / FADE_OUT_TIME)

            val textOffset = Point(
                0,
                PLAYER_SIZE.h +
                        pickupMotion * 0.15 +
                        lineHeight * (1 + HEIGHT_ABOVE_HEAD)
            )

            r.color = colorScheme.textColor.withAlpha(alpha)
            r.text(
                fontSmall,
                "+$growingScore",
                player.body.p + textOffset,
                Align.center,
                shouldCache = false
            )
        }
    }
}