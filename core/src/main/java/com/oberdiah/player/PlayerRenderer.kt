package com.oberdiah.player

import com.badlogic.gdx.graphics.Color
import com.oberdiah.*
import com.oberdiah.Point.Companion.invoke
import com.oberdiah.level.APP_TIME_GAME_STARTED
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import kotlin.random.Random

object PlayerRenderer {
    private var renderedHeadOffset = 0.0
    private var colorRotation = 0.0f

    fun render(r: Renderer) {
        colorRotation += GameTime.GRAPHICS_DELTA.f

        val pos = player.body.p

        var colorful = false

        if (PlayerInputs.canJump) {
            colorful = true
            r.color = colorScheme.player
        } else if (player.state.isSlamming) {
            colorful = true
            r.color = colorScheme.playerSlamming
        } else {
            r.color = colorScheme.playerNoJump
        }

        val playerSize = PLAYER_SIZE * saturate((GameTime.APP_TIME - APP_TIME_GAME_STARTED) * 4.0)

        if (rainbowPlayerEnabled.value && colorful && player.body.velocity.len > 0.1) {
            val hsv = FloatArray(3)
            r.color.toHsv(hsv)
            hsv[0] = (hsv[0] + colorRotation * 100) % 360
            r.color = Color(1f, 1f, 1f, 1f).fromHsv(hsv)

            spawnSmoke(
                Rect.centered(pos, playerSize * 0.5).randomPointInside(),
                Velocity(),
                r.color.cpy()
            )
        }

        val preparingAction = PlayerInputs.currentPreparingAction
        val desiredHeadOffset =
            if (player.state.isSlamming || preparingAction == PlayerInputs.PreparingAction.Jump) {
                0.2
            } else if (preparingAction == PlayerInputs.PreparingAction.Slam) {
                -0.2
            } else {
                0.0
            }

        renderedHeadOffset = frameAccurateLerp(renderedHeadOffset, desiredHeadOffset, 20.0)

        val radius = (1 - renderedHeadOffset * 0.5) * playerSize.w / 2

        r.circle(pos, radius)

        r.rect(
            pos.x - radius,
            pos.y,
            radius * 2,
            playerSize.h / 2 + renderedHeadOffset
        )

        // Move the player's head up and down based on the actionPerformAmount

        val topOfPlayersHeadPos =
            Point(pos.x, pos.y + playerSize.h / 2 * GLOBAL_SCALE + renderedHeadOffset)

        r.circle(topOfPlayersHeadPos, radius)

        if (ScoreSystem.bounceDecayAccumulator > 0) {
            r.color = Color.WHITE.withAlpha(0.5)
            if (ScoreSystem.timeWarp() > 1.0) {
                r.color = Color.CORAL.withAlpha(0.5)
            }

            r.arcFrom0(topOfPlayersHeadPos, radius * 0.8, ScoreSystem.bounceDecayAccumulator)
        }

        ScoreSystem.renderFloatingPlayerText(r)
    }

    fun spawnParticlesAtMyFeet(
        addedVelocity: Point = Point(0.0, 0.0),
        number: Int = 5,
        ferocity: Double = 2.0
    ) {
        val posToSpawn = player.body.p - Point(0.0, 0.15 * GLOBAL_SCALE)

        for (i in 0 until number) {
            spawnFragment(
                posToSpawn,
                addedVelocity + Velocity(
                    ferocity * (Random.nextDouble() - 0.5),
                    ferocity * Random.nextDouble()
                ),
                PlayerInfoBoard.tileBelowMe?.getTileType() ?: TileType.Air
            )
        }
    }
}