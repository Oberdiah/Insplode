package com.oberdiah.player

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.GROWING_SCORE_REFRESH_COUNTDOWN
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.Velocity
import com.oberdiah.bounceDecayAccumulator
import com.oberdiah.fontSmall
import com.oberdiah.format
import com.oberdiah.frameAccurateLerp
import com.oberdiah.growingScore
import com.oberdiah.growingScoreStartedOn
import com.oberdiah.lastScoreCollectionTime
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.min
import com.oberdiah.multiplier
import com.oberdiah.saturate
import com.oberdiah.spawnFragment
import com.oberdiah.timeWarp
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha
import kotlin.random.Random

private const val FADE_IN_TIME = 0.05
private const val FADE_OUT_TIME = 0.3
private const val HEIGHT_ABOVE_HEAD = 0.5

class PlayerRenderer {
    private var renderedHeadOffset = 0.0

    fun render(r: Renderer) {
        val pos = player.body.p

        if (playerInputs.canJump) {
            r.color = colorScheme.player
        } else if (playerState.isSlamming) {
            r.color = colorScheme.playerSlamming
        } else {
            r.color = colorScheme.playerNoJump
        }

        val desiredHeadOffset = if (playerState.isPreparingToJump) {
            -0.2
        } else if (playerState.isSlamming) {
            0.2
        } else {
            0.0
        }

        renderedHeadOffset = frameAccurateLerp(renderedHeadOffset, desiredHeadOffset, 20.0)

        val radius = (1 - renderedHeadOffset * 0.5) * PLAYER_SIZE.w / 2

        r.circle(pos, radius)

        r.rect(
            pos.x - radius,
            pos.y,
            radius * 2,
            PLAYER_SIZE.h / 2 + renderedHeadOffset
        )

        // Move the player's head up and down based on the actionPerformAmount

        val topOfPlayersHeadPos = Point(pos.x, pos.y + 0.35 * GLOBAL_SCALE + renderedHeadOffset)

        r.circle(topOfPlayersHeadPos, radius)

        r.color = Color.WHITE.withAlpha(0.5)

        if (timeWarp() > 1.0) {
            r.color = Color.CORAL.withAlpha(0.5)
        }

        r.arcFrom0(topOfPlayersHeadPos, radius * 0.8, bounceDecayAccumulator)

        renderFloatingPlayerText(r)
    }

    // Uses the in-world renderer
    private fun renderFloatingPlayerText(r: Renderer) {
        if (playerState.isDead) return

        val lineHeight = fontSmall.lineHeight / UNIT_SIZE_IN_PIXELS

        if (multiplier() > 1.0) {
            val textOffset = Point(
                0,
                PLAYER_SIZE.h + lineHeight * HEIGHT_ABOVE_HEAD
            )

            r.color = colorScheme.textColor

            r.text(
                fontSmall,
                "x${multiplier().format(1)}",
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

    fun spawnParticlesAtMyFeet(
        addedVelocity: Point = Point(0.0, 0.0),
        number: Int = 5,
        ferocity: Double = 2.0
    ) {
        val posToSpawn = player.body.p - Point(0.0, 0.15 * GLOBAL_SCALE)

        for (i in 0 until number) {
            spawnFragment(
                posToSpawn.cpy,
                addedVelocity + Velocity(
                    ferocity * (Random.nextDouble() - 0.5),
                    ferocity * Random.nextDouble()
                ),
                playerInfoBoard.tileBelowMe?.getTileType() ?: TileType.Air
            )
        }
    }
}