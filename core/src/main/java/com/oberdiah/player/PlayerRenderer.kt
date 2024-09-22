package com.oberdiah.player

import com.badlogic.gdx.graphics.Color
import com.oberdiah.CAMERA_POS_Y
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.PAUSED
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.div
import com.oberdiah.frameAccurateLerp
import com.oberdiah.minus
import com.oberdiah.plus
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha

class PlayerRenderer {
    private var renderedHeadOffset = 0.0

    fun render(r: Renderer) {
        if (player.isDead) {
            return
        }

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
        } else if (player.isSlamming) {
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
}