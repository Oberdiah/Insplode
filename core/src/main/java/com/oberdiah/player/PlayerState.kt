package com.oberdiah.player

import com.oberdiah.Bomb
import com.oberdiah.DELTA
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.Point
import com.oberdiah.Velocity
import com.oberdiah.abs
import com.oberdiah.boom
import com.oberdiah.clamp
import com.oberdiah.compareTo
import com.oberdiah.d
import com.oberdiah.i
import com.oberdiah.max
import com.oberdiah.plus
import com.oberdiah.registerBombSlamWithScoreSystem
import com.oberdiah.spawnSmoke
import com.oberdiah.times
import com.oberdiah.utils.addScreenShake
import kotlin.math.pow
import kotlin.random.Random

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

    /** The player is dead */
    DEAD
}

private class PlayerStateHandler {
    var state: PlayerMode = PlayerMode.SLAMMING
        private set

    var timeSinceWeEnteredThisState = 0.0
        private set

    fun tick() {
        timeSinceWeEnteredThisState += DELTA
    }

    fun reset() {
        timeSinceWeEnteredThisState = 0.0
        setState(PlayerMode.SLAMMING)
    }

    fun setState(newState: PlayerMode) {
        state = newState
        timeSinceWeEnteredThisState = 0.0
    }
}

/**
 * Actually performs actions on the player and keeps track of how long it has been since each action was performed.
 *
 * Only actions can modify player state.
 */
class PlayerState {
    private var s = PlayerStateHandler()

    fun reset() {
        s.reset()
    }

    fun tick() {
        s.tick()
    }

    fun isSlamming(): Boolean {
        return s.state == PlayerMode.SLAMMING
    }

    fun timeSinceStartedSlamming(): Double {
        if (!isSlamming()) {
            return 0.0
        }

        return s.timeSinceWeEnteredThisState
    }

    fun isDead(): Boolean {
        return s.state == PlayerMode.DEAD
    }

    fun isAlive(): Boolean {
        return !isDead()
    }

    fun timeSinceDied(): Double {
        if (!isDead()) {
            return 0.0
        }

        return s.timeSinceWeEnteredThisState
    }

    fun startSlam() {
        if (s.state != PlayerMode.INTENTIONALLY_MOVING_UP) {
            println("Player should be in the INTENTIONALLY_MOVING_UP state to start a slam, was in ${s.state}")
        }
        s.setState(PlayerMode.SLAMMING)
    }

    fun playerHasSlammedIntoABomb(bomb: Bomb) {
        if (s.state != PlayerMode.SLAMMING) {
            println("Player should be in the SLAMMING state to slam into a bomb, was in ${s.state}")
        }
        s.setState(PlayerMode.INTENTIONALLY_MOVING_UP)

        if (abs(playerInfoBoard.lastTickVelocity.y) > MINIMUM_SLAM_VELOCITY) {
            boom(bomb.body.p, bomb.power, affectsThePlayer = false)
            bomb.destroy()
            val currentVel = player.body.velocity.y
            val desiredVel =
                clamp(abs(player.body.velocity.y).pow(0.75) + bomb.power * 2.0, 5.0, 15.0)
            val impulse = player.body.mass * (desiredVel - currentVel)
            player.body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)
            registerBombSlamWithScoreSystem(bomb)
        }
    }

    fun playerHasSlammedIntoTheGround() {
        if (s.state != PlayerMode.SLAMMING) {
            println("Player should be in the SLAMMING state to slam into the ground, was in ${s.state}")
        }
        s.setState(PlayerMode.IDLE)

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

    fun killThePlayer() {
        player.body.linearDamping = Float.MAX_VALUE
        // Spawn a bunch of smoke in the shape of the player
        for (i in 0 until 100) {
            val pos = player.body.p + Point(
                Random.nextDouble(-PLAYER_SIZE.x / 2, PLAYER_SIZE.x / 2),
                Random.nextDouble(-PLAYER_SIZE.y / 2, PLAYER_SIZE.y / 2)
            )
            val vel = Velocity(Random.nextDouble(-0.5, 0.5), Random.nextDouble(-0.5, 0.5))
            spawnSmoke(pos, vel)
        }
    }
}