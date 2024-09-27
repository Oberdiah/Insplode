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

        r.circle(pos, PLAYER_SIZE.w / 2)

        val desiredHeadOffset =
            0.0 // Could do something here in future - We could maybe also do skinnier on growing taller?
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