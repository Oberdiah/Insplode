package com.oberdiah.player

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.oberdiah.CAMERA_POS_Y
import com.oberdiah.GAME_IS_RUNNING
import com.oberdiah.IS_DEBUG_ENABLED
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.UNITS_WIDE
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.abs
import com.oberdiah.boom
import com.oberdiah.d
import com.oberdiah.f
import com.oberdiah.saturate
import com.oberdiah.ui.pauseHovered
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.isKeyJustPressed
import com.oberdiah.utils.isKeyPressed
import com.oberdiah.withAlpha

object PlayerInputs {
    /** The point where the player's finger last went down. */
    private var lastFingerPoint = Point()
    private var lastBodyXValue = 0.0

    var desiredXPos = 0.0
        private set

    val canJump
        get() = (player.state.isPreparingToJump || player.state.isIdle) && PlayerInfoBoard.isStandingOnStandableGenerous

    fun reset() {
        lastFingerPoint = Point()
        lastBodyXValue = 0.0
    }

    fun render(r: Renderer) {
        val pos = player.body.p
        if (GAME_IS_RUNNING && !pauseHovered) {
            TOUCHES_DOWN.firstOrNull()?.let { _ ->
                val lineX = desiredXPos

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
    }

    fun tick() {
        val vel = player.body.velocity

        if (isJumpJustPressed() && canJump) {
            player.state.justPerformedAJump()
        } else if (isSlamJustPressed() && !canJump) {
            player.state.justStartedASlam()
        }

        if (IS_DEBUG_ENABLED) {
            if (isKeyPressed(Keys.R)) {
                // Explode on the player
                boom(player.body.p, 1.0, false)
            }
        }

        if (TOUCHES_DOWN.size == 1) {
            TOUCHES_WENT_DOWN.forEach {
                lastFingerPoint = it / UNIT_SIZE_IN_PIXELS
                lastBodyXValue = player.body.p.x

                if (UpgradeController.playerHas(Upgrade.Jump)) {
                    if (player.state.isIdle) {
                        player.state.justStartedPreparingAJump()
                    }
                }
            }
        }

        desiredXPos = player.body.p.x
        TOUCHES_DOWN.firstOrNull()?.let { touch ->
            val finger = touch / UNIT_SIZE_IN_PIXELS
            desiredXPos = lastBodyXValue + (finger.x - lastFingerPoint.x) * 1.8

            if (player.state.isPreparingToJump) {
                if (finger.distTo(lastFingerPoint) > 0.1) {
                    player.state.justCancelledPreparingAJump()
                }
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

        val movementSpeed = UpgradeController.getMovementSpeed()

        val desiredXVel = when {
            desiredXPos < player.body.p.x -> -movementSpeed * magnitude
            desiredXPos > player.body.p.x -> movementSpeed * magnitude
            else -> 0.0
        }

        val velChange = desiredXVel - vel.x

        // If velChange is large enough, spawn particles to simulate kicked up dirt in the direction of movement
        if (PlayerInfoBoard.isStandingOnStandableExact) {
            if (abs(velChange) > 5.1) {
                PlayerRenderer.spawnParticlesAtMyFeet(
                    number = 3,
                    addedVelocity = Point(velChange * 0.5, 0.0)
                )
            }
        }

        val impulse = player.body.mass * velChange
        player.body.applyImpulse(Point(impulse.f, 0))
    }

    private fun isJumpJustPressed(): Boolean {
        val onDesktop = Gdx.app.type == Application.ApplicationType.Desktop

        if (player.state.isPreparingToJump) {
            TOUCHES_WENT_UP.firstOrNull()?.let {
                return true
            }
        }

        if (onDesktop && UpgradeController.playerHas(Upgrade.Jump)) {
            return isKeyJustPressed(Keys.SPACE) || isKeyJustPressed(Keys.W) || isKeyJustPressed(Keys.UP)
        }

        return false
    }

    private fun isSlamJustPressed(): Boolean {
        return TOUCHES_WENT_UP.firstOrNull()?.let {
            true
        } ?: if (Gdx.app.type == Application.ApplicationType.Desktop) {
            isKeyJustPressed(Keys.S) || isKeyJustPressed(Keys.DOWN)
        } else {
            false
        }
    }
}