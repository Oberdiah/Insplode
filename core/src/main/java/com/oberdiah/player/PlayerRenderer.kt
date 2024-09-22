package com.oberdiah.player

import com.badlogic.gdx.graphics.Color
import com.oberdiah.CAMERA_POS_Y
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.PAUSED
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.Velocity
import com.oberdiah.div
import com.oberdiah.frameAccurateLerp
import com.oberdiah.minus
import com.oberdiah.plus
import com.oberdiah.spawnFragment
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha
import kotlin.random.Random

class PlayerRenderer {
    private var renderedHeadOffset = 0.0

    fun render(r: Renderer) {
        val pos = player.body.p

        if (!PAUSED) {
            TOUCHES_DOWN.firstOrNull()?.let { touch ->
                // If the player is within the uncertainty window, make the line green
                val lineX = player.getDesiredXPos(touch.x / UNIT_SIZE_IN_PIXELS)

                if (lineX in (pos.x - PLAYER_UNCERTAINTY_WINDOW * 1.1)..(pos.x + PLAYER_UNCERTAINTY_WINDOW * 1.1)) {
                    r.color = Color.WHITE.withAlpha(0.5)
                } else {
                    r.color = Color.WHITE.withAlpha(0.25)
                }
                // For visual interest, draw two lines one thinner than the other
                r.line(
                    lineX,
                    CAMERA_POS_Y,
                    lineX,
                    CAMERA_POS_Y + SCREEN_HEIGHT_IN_UNITS,
                    0.3,
                )
            }
        }

        if (player.canJump()) {
            r.color = colorScheme.player
        } else if (playerState.isSlamming()) {
            r.color = colorScheme.playerSlamming
        } else {
            r.color = colorScheme.playerNoJump
        }

        r.circle(pos, PLAYER_SIZE.w / 2)

        val desiredHeadOffset = -player.getJumpFraction() * 0.2
        renderedHeadOffset = frameAccurateLerp(renderedHeadOffset, desiredHeadOffset, 30.0)

        r.rect(
            pos.x - PLAYER_SIZE.w / 2,
            pos.y,
            PLAYER_SIZE.w,
            PLAYER_SIZE.h / 2 + renderedHeadOffset
        )

        // Move the player's head up and down based on the actionPerformAmount
        r.circle(
            pos.x,
            pos.y + 0.35 * GLOBAL_SCALE + renderedHeadOffset,
            PLAYER_SIZE.w / 2
        )
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