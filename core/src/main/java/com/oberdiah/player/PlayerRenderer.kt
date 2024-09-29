package com.oberdiah.player

import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.Velocity
import com.oberdiah.div
import com.oberdiah.frameAccurateLerp
import com.oberdiah.minus
import com.oberdiah.plus
import com.oberdiah.spawnFragment
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import kotlin.random.Random

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
        r.circle(
            pos.x,
            pos.y + 0.35 * GLOBAL_SCALE + renderedHeadOffset,
            radius
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