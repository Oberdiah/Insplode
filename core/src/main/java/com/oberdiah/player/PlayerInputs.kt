package com.oberdiah.player

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.oberdiah.IS_DEBUG_ENABLED
import com.oberdiah.Point
import com.oberdiah.UNITS_WIDE
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.abs
import com.oberdiah.boom
import com.oberdiah.d
import com.oberdiah.f
import com.oberdiah.saturate
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.isKeyJustPressed
import com.oberdiah.utils.isKeyPressed

class PlayerInputs {
    /** The point where the player's finger last went down. */
    private var lastFingerPoint = Point()
    private var lastBodyXValue = 0.0

    var desiredXPos = 0.0
        private set

    val canJump
        get() = playerState.isIdle && playerInfoBoard.isStandingOnStandableGenerous

    fun reset() {
        lastFingerPoint = Point()
        lastBodyXValue = 0.0
    }

    fun tick() {
        val vel = player.body.velocity

        if (isJumpJustPressed() && canJump) {
            playerState.justPerformedAJump()
        } else if (isSlamJustPressed() && !canJump) {
            playerState.justStartedASlam()
        }

        if (TOUCHES_DOWN.size == 1) {
            TOUCHES_WENT_DOWN.forEach {
                lastFingerPoint = it / UNIT_SIZE_IN_PIXELS
                lastBodyXValue = player.body.p.x
            }
        }

        desiredXPos = player.body.p.x

        TOUCHES_DOWN.firstOrNull()?.let { touch ->
            val fingerX = touch.x / UNIT_SIZE_IN_PIXELS
            desiredXPos = lastBodyXValue + (fingerX - lastFingerPoint.x) * 1.8
        }

        if (IS_DEBUG_ENABLED) {
            if (isKeyPressed(Keys.R)) {
                // Explode on the player
                boom(player.body.p, 1.0, false)
            }
        }

        // If on Desktop, read A/D and Left/Right arrow keys
        if (isKeyPressed(Keys.A) || isKeyPressed(Keys.LEFT)) {
            desiredXPos = 0.0
        }
        if (isKeyPressed(Keys.D) || isKeyPressed(Keys.RIGHT)) {
            desiredXPos = UNITS_WIDE.d
        }

        val magnitude = saturate(abs(desiredXPos - player.body.p.x) / PLAYER_UNCERTAINTY_WINDOW)

        val desiredXVel = when {
            desiredXPos < player.body.p.x -> -5 * magnitude
            desiredXPos > player.body.p.x -> 5 * magnitude
            else -> 0.0
        }

        val velChange = desiredXVel - vel.x

        // If velChange is large enough, spawn particles to simulate kicked up dirt in the direction of movement
        if (playerInfoBoard.isStandingOnStandableExact) {
            if (abs(velChange) > 5.1) {
                playerRenderer.spawnParticlesAtMyFeet(
                    number = 3,
                    addedVelocity = Point(velChange * 0.5, 0.0)
                )
            }
        }

        val impulse = player.body.mass * velChange
        player.body.applyImpulse(Point(impulse.f, 0))
    }

    private fun isJumpJustPressed(): Boolean {
        return TOUCHES_WENT_UP.firstOrNull()?.let {
            return true
        } ?: if (Gdx.app.type == Application.ApplicationType.Desktop) {
            isKeyJustPressed(Keys.SPACE) || isKeyJustPressed(Keys.W) || isKeyJustPressed(Keys.UP)
        } else {
            false
        }
    }

    private fun isSlamJustPressed(): Boolean {
        return TOUCHES_WENT_UP.firstOrNull()?.let {
            return true
        } ?: if (Gdx.app.type == Application.ApplicationType.Desktop) {
            isKeyJustPressed(Keys.S) || isKeyJustPressed(Keys.DOWN)
        } else {
            false
        }
    }
}