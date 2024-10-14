package com.oberdiah.player

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
import com.oberdiah.frameAccurateLerp
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
    private const val PLAYER_FINGER_DRAG_DISTANCE = 0.2

    /** The point where the player's finger was last frame. */
    private var lastFingerPoint = Point()
    private var driftingPlayerSwipeStartY = 0.0
    var currentPreparingAction: PreparingAction = PreparingAction.None
        private set

    enum class PreparingAction {
        Jump,
        Slam,
        None
    }

    private fun calculateCurrentPreparingAction(): PreparingAction {
        TOUCHES_DOWN.firstOrNull()?.let { touch ->
            val v =
                (touch.y / UNIT_SIZE_IN_PIXELS - driftingPlayerSwipeStartY) / PLAYER_FINGER_DRAG_DISTANCE
            if (v >= 1) {
                return PreparingAction.Jump
            } else if (v <= -1) {
                return PreparingAction.Slam
            }
        }
        return PreparingAction.None
    }

    var desiredXPos = 0.0
        private set

    val canJump
        get() = player.state.isIdle && PlayerInfoBoard.isStandingOnStandableGenerous

    fun render(r: Renderer) {
        val pos = player.body.p
        if (GAME_IS_RUNNING && !pauseHovered && UpgradeController.playerHas(Upgrade.Movement)) {
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

        if (isJumpFiredThisFrame() && canJump) {
            player.state.justPerformedAJump()
        } else if (isSlamFiredThisFrame() && !canJump) {
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
                val finger = it / UNIT_SIZE_IN_PIXELS
                desiredXPos = player.body.p.x
                driftingPlayerSwipeStartY = finger.y
                lastFingerPoint = finger
            }
        }

        var xPosToUse = player.body.p.x

        TOUCHES_DOWN.firstOrNull()?.let { touch ->
            val finger = touch / UNIT_SIZE_IN_PIXELS

            val diff = finger - lastFingerPoint
            if (diff.x.abs > diff.y.abs) {
                desiredXPos += diff.x * 1.8
            }

            driftingPlayerSwipeStartY = frameAccurateLerp(driftingPlayerSwipeStartY, finger.y, 10.0)
            lastFingerPoint = finger
            xPosToUse = desiredXPos
        }

        // If on Desktop, read A/D and Left/Right arrow keys
        if (isKeyPressed(Keys.A) || isKeyPressed(Keys.LEFT)) {
            xPosToUse = 0.0
        }
        if (isKeyPressed(Keys.D) || isKeyPressed(Keys.RIGHT)) {
            xPosToUse = UNITS_WIDE.d
        }

        val magnitude = saturate(abs(xPosToUse - player.body.p.x) / PLAYER_UNCERTAINTY_WINDOW)

        val movementSpeed = UpgradeController.getMovementSpeed()

        val desiredXVel = when {
            xPosToUse < player.body.p.x -> -movementSpeed * magnitude
            xPosToUse > player.body.p.x -> movementSpeed * magnitude
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

        currentPreparingAction = calculateCurrentPreparingAction()
    }

    private fun isJumpFiredThisFrame(): Boolean {
        if (!UpgradeController.playerHas(Upgrade.Jump)) {
            return false
        }

        return TOUCHES_WENT_UP.firstOrNull()?.let {
            currentPreparingAction == PreparingAction.Jump
        } ?: isKeyJustPressed(Keys.SPACE) || isKeyJustPressed(Keys.W) || isKeyJustPressed(Keys.UP)
    }

    private fun isSlamFiredThisFrame(): Boolean {
        if (!UpgradeController.playerHas(Upgrade.Slam)) {
            return false
        }

        return TOUCHES_WENT_UP.firstOrNull()?.let {
            currentPreparingAction == PreparingAction.Slam
        } ?: isKeyJustPressed(Keys.S) || isKeyJustPressed(Keys.DOWN)
    }
}