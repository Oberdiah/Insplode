package com.oberdiah.player

import com.oberdiah.Bomb
import com.oberdiah.DELTA
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.Point
import com.oberdiah.abs
import com.oberdiah.boom
import com.oberdiah.clamp
import com.oberdiah.compareTo
import com.oberdiah.d
import com.oberdiah.i
import com.oberdiah.max
import com.oberdiah.plus
import com.oberdiah.registerBombSlamWithScoreSystem
import com.oberdiah.times
import com.oberdiah.utils.addScreenShake
import kotlin.math.pow

private enum class PlayerMode {
    /**
     * The player may or may not be on the ground.
     * If they're in the air they're definitely not in the air due to a jump.
     */
    IDLE,

    /** This could be either due to a jump or a slam bouncing. */
    INTENTIONALLY_MOVING_UP,

    /** The player is moving down at speed, slamming. */
    SLAMMING,
}

/**
 * Actually performs actions on the player and keeps track of how long it has been since each action was performed.
 *
 * Only actions can modify player state.
 */
class PlayerState {
    private var state: PlayerMode = PlayerMode.SLAMMING
    private var timeSinceLastSlam = 0.0

    fun reset() {
        state = PlayerMode.SLAMMING
        timeSinceLastSlam = 0.0
    }

    fun tick() {
        timeSinceLastSlam += DELTA
    }

    fun isSlamming(): Boolean {
        return state == PlayerMode.SLAMMING
    }

    fun startSlam() {
        require(state == PlayerMode.INTENTIONALLY_MOVING_UP) {
            "Player must be in the INTENTIONALLY_MOVING_UP state to start a slam"
        }
        state = PlayerMode.SLAMMING
    }

    fun playerHasSlammedIntoABomb(bomb: Bomb) {
        require(state == PlayerMode.SLAMMING) {
            "Player must be in the SLAMMING state to slam into a bomb"
        }
        state = PlayerMode.INTENTIONALLY_MOVING_UP

        if (abs(playerInfoBoard.lastTickVelocity.y) > MINIMUM_SLAM_VELOCITY) {
            boom(bomb.body.p, bomb.power, affectsThePlayer = false)
            bomb.destroy()
            val currentVel = player.body.velocity.y
            val desiredVel =
                clamp(abs(player.body.velocity.y).pow(0.75) + bomb.power * 2.0, 5.0, 15.0)
            val impulse = player.body.mass * (desiredVel - currentVel)
            player.body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)
            timeSinceLastSlam = 0.0
            registerBombSlamWithScoreSystem(bomb)
        }
    }

    fun playerHasSlammedIntoTheGround() {
        require(state == PlayerMode.SLAMMING) {
            "Player must be in the SLAMMING state to slam into the ground"
        }
        state = PlayerMode.IDLE

        val vel = playerInfoBoard.lastTickVelocity
        playerRenderer.spawnParticlesAtMyFeet(
            ferocity = vel.len.d * 0.2,
            number = max((vel.len.d * 0.5).i, 2)
        )

        if (vel.len > 10) {
            // Player landing deserves more than the normal amount of shake
            addScreenShake(vel.len.d * 0.025)
            boom(player.body.p, vel.len.d * 0.03, affectsThePlayer = false)
        }
    }
}