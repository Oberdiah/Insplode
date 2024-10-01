package com.oberdiah

import com.badlogic.gdx.utils.Align
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.PLAYER_SIZE
import com.oberdiah.player.player
import com.oberdiah.player.playerState
import com.oberdiah.utils.colorScheme
import kotlin.math.pow
import kotlin.random.Random

var lastScore: Int? = null
    private set

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
    return clamp(1 + (numConsecutiveBounces.d * 0.1), 1.0, 2.0)
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
    statefulHighScore.value = max(playerScore, statefulHighScore.value)
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

// Uses the in-world renderer
fun renderScoreSystemWorldSpace(r: Renderer) {
    if (playerState.isDead) return

    if (numConsecutiveBounces > 0) {
        val textOffset = Point(
            0,
            PLAYER_SIZE.h +
                    fontSmall.lineHeight * HEIGHT_ABOVE_HEAD / UNIT_SIZE_IN_PIXELS
        )
        r.color = colorScheme.textColor
        r.text(
            fontSmall,
            "x${multiplier().format(1)}",
            player.body.p + textOffset,
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

        val textOffset = Point(
            0,
            PLAYER_SIZE.h +
                    pickupMotion * 0.15 +
                    fontSmall.lineHeight * (1 + HEIGHT_ABOVE_HEAD) / UNIT_SIZE_IN_PIXELS
        )

        r.color = colorScheme.textColor.withAlpha(alpha)
        r.text(
            fontSmall,
            "+$growingScore",
            player.body.p + textOffset,
            Align.center
        )
    }
}