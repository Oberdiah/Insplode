package com.oberdiah

import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.utils.GameTime.GAMEPLAY_DELTA
import com.oberdiah.utils.GameTime.updateGameSpeed
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
}

fun registerBombSlamWithScoreSystem(bomb: Bomb) {
    val numToNormallySpawn = bomb.getPointsWorth()
    spawnPointOrbs(bomb.body.p, (numToNormallySpawn * multiplier()).i)
    updateGameSpeed(timeWarp())

    bounceDecayAccumulator = 0.0

    numConsecutiveBounces++

    playMultiplierSound(numConsecutiveBounces)
}

fun registerGameEndWithScoreSystem() {
    lastScore = playerScore
}

/** A bomb pop is when you pop a bomb by jumping on it or it hitting your head too hard. */
fun registerBombPopWithScoreSystem(bomb: Bomb) {
    spawnPointOrbs(bomb.body.p, 1)
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

    if (scoreLeftToSound > 0) {
        val timeToNext = Random.nextDouble(0.1, 0.12) * 0.98.pow(scoreLeftToSound)
        if (lastTimeScoreSounded + timeToNext < RUN_TIME_ELAPSED) {
            val scoreToGive = 1
            givePlayerScoreInternal(scoreToGive)
            scoreLeftToSound -= scoreToGive
            lastTimeScoreSounded = RUN_TIME_ELAPSED
        }
    }

    if (RUN_TIME_ELAPSED - lastScoreCollectionTime > GROWING_SCORE_REFRESH_COUNTDOWN) {
        growingScore = 0
    }
}