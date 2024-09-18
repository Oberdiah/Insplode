package com.oberdiah

import com.badlogic.gdx.utils.Align
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.wToSSpace
import kotlin.math.pow
import kotlin.random.Random

var playerScore = 0
    private set

var growingScore = 0
    private set

private var numConsecutiveBounces = 0
private var lastScoreCollectionTime = RUN_TIME_ELAPSED
private var growingScoreStartedOn = RUN_TIME_ELAPSED

private const val FADE_IN_TIME = 0.05
private const val FADE_OUT_TIME = 0.3
private const val GROWING_SCORE_TIME = 1.5

/** The near-instant delay that causes the score to come in in small bits */
private var scoreLeftToSound = 0
private var lastTimeScoreSounded = 0.0

fun multiplier(): Double {
    return 1 + (numConsecutiveBounces.d * 0.1)
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

fun registerBombDestroyWithScoreSystem(bomb: Bomb) {
    val numToNormallySpawn = (bomb.power.d * 2.0).pow(2.0).i
    spawnPointOrbs(bomb.body.p, (numToNormallySpawn * multiplier()).i)
    numConsecutiveBounces++
}

fun registerLandedOnGroundWithScoreSystem() {
    numConsecutiveBounces = 0
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
    lastScoreCollectionTime = RUN_TIME_ELAPSED
}

fun tickScoreSystem() {
    if (scoreLeftToSound > 0) {
        val timeToNext = Random.nextDouble(0.1, 0.12) * 0.98.pow(scoreLeftToSound)
        if (lastTimeScoreSounded + timeToNext < RUN_TIME_ELAPSED) {
            val scoreToGive = 1
            givePlayerScoreInternal(scoreToGive)
            scoreLeftToSound -= scoreToGive
            lastTimeScoreSounded = RUN_TIME_ELAPSED
        }
    }

    if (RUN_TIME_ELAPSED - lastScoreCollectionTime > GROWING_SCORE_TIME) {
        growingScore = 0
    }
}

const val HEIGHT_ABOVE_HEAD = 0.5

fun renderScoreSystem(r: Renderer) {
    if (player.isDead) return

    if (numConsecutiveBounces > 0) {
        val textOffset = Point(0, PLAYER_SIZE.h)
        val textOffset2 = Point(0, fontSmall.lineHeight * HEIGHT_ABOVE_HEAD)
        r.color = colorScheme.textColor
        r.text(
            fontSmall,
            "x${multiplier().format(1)}",
            wToSSpace(player.body.p + textOffset) + textOffset2,
            Align.center
        )
    }

    if (growingScore > 0) {
        val fadeInAlpha = (RUN_TIME_ELAPSED - growingScoreStartedOn) / FADE_IN_TIME
        val fadeOutAlpha =
            ((lastScoreCollectionTime + GROWING_SCORE_TIME) - RUN_TIME_ELAPSED) / FADE_OUT_TIME
        val alpha = saturate(min(fadeInAlpha, fadeOutAlpha))

        val pickupMotion =
            1 - saturate((RUN_TIME_ELAPSED - lastScoreCollectionTime) / FADE_OUT_TIME)

        val textOffset = Point(0, PLAYER_SIZE.h + pickupMotion * 0.15)
        val textOffset2 = Point(0, fontSmall.lineHeight * (1 + HEIGHT_ABOVE_HEAD))

        r.color = colorScheme.textColor.withAlpha(alpha)
        r.text(
            fontSmall,
            "+$growingScore",
            wToSSpace(player.body.p + textOffset) + textOffset2,
            Align.center
        )
    }
}