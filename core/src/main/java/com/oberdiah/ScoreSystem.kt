package com.oberdiah

import com.badlogic.gdx.utils.Align
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.ui.MENU_ZONE_BOTTOM_Y
import com.oberdiah.ui.coinAreaPosition
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.GameTime.APP_TIME
import com.oberdiah.utils.GameTime.GAMEPLAY_DELTA
import com.oberdiah.utils.GameTime.updateGameSpeed
import com.oberdiah.utils.colorScheme
import kotlin.math.pow
import kotlin.random.Random

var lastScore: Int? = null
    private set

var playerScore = 0
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

private var coinsToGiveAtEndOfGame = 0
private var lastScoreGivenOn = 0.0

// In score per second
private var scoreGivingSpeed = 0.0

fun multiplier(): Double {
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

/** Per game-second. */
fun bounceDecayRate(): Double {
    return clamp((numConsecutiveBounces - 10) * 0.1, 0.0, 0.5)
}

fun resetScoreSystem() {
    playerScore = 0
    growingScore = 0
    numConsecutiveBounces = 0
    lastScoreCollectionTime = RUN_TIME_ELAPSED
    growingScoreStartedOn = RUN_TIME_ELAPSED
    scoreLeftToSound = 0
    lastTimeScoreSounded = 0.0

//    registerGameEndWithScoreSystem()
}

fun registerBombSlamWithScoreSystem(bomb: Bomb) {
    val numToNormallySpawn = bomb.getPointsWorth()
    PointOrbs.spawnOrbs(bomb.body.p, (numToNormallySpawn * multiplier()).i)
    updateGameSpeed(timeWarp())

    bounceDecayAccumulator = 0.0

    numConsecutiveBounces++

    playMultiplierSound(numConsecutiveBounces)
}

fun registerGameEndWithScoreSystem() {
    lastScore = playerScore
    lastScoreGivenOn = APP_TIME
    scoreGivingSpeed = max(coinsToGiveAtEndOfGame / 4.0, 4.0)
}

fun registerCasuallyLandedWithScoreSystem() {
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
    coinsToGiveAtEndOfGame += score
    statefulHighScore.value = max(playerScore, statefulHighScore.value)
    lastScoreCollectionTime = RUN_TIME_ELAPSED
}

fun tickScoreSystem() {
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
                coinsToGiveAtEndOfGame
            )
        }
        if (coinsToGiveAtEndOfGame > 0 && lastScoreGivenOn + delayBetweenScoreGiving < APP_TIME) {
            statefulCoinBalance.value += scoreToMove
            coinsToGiveAtEndOfGame -= scoreToMove
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
                color = colorScheme.pickupColor.cpy().mul(Random.nextDouble(0.8, 1.2).f),
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

fun renderScores(r: Renderer) {
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

    val coinsFractionLeft = coinsToGiveAtEndOfGame / (lastScore?.d ?: 1.0)
    val coinTextOpacity = saturate(coinsFractionLeft * 5.0)

    val inGameScreenSpaceLocation = Point(WIDTH / 2, HEIGHT * 0.9).wo

    if (coinsToGiveAtEndOfGame > 0) {
        r.color = colorScheme.textColor.withAlpha(coinTextOpacity)
        r.text(
            fontLarge,
            "$coinsToGiveAtEndOfGame",
            W / 2, min(endOfGameCoinsHeight, inGameScreenSpaceLocation.y),
            Align.center,
            shouldCache = false
        )
    }
    if (lastScore != null) {
        r.color = colorScheme.textColor.withAlpha(1.0 - coinTextOpacity)
        r.text(
            fontSmallish,
            "Score: $lastScore",
            W / 2, MENU_ZONE_BOTTOM_Y + H * 3.0 / 4 - 3.0,
            Align.center,
            shouldCache = false
        )
    }
}