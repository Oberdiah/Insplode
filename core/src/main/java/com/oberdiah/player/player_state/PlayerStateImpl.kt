package com.oberdiah.player.player_state

import com.oberdiah.Bomb
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
import com.oberdiah.min
import com.oberdiah.player.MINIMUM_SLAM_VELOCITY
import com.oberdiah.player.PLAYER_SIZE
import com.oberdiah.player.player
import com.oberdiah.player.playerInfoBoard
import com.oberdiah.player.playerRenderer
import com.oberdiah.plus
import com.oberdiah.registerBombSlamWithScoreSystem
import com.oberdiah.spawnSmoke
import com.oberdiah.times
import com.oberdiah.utils.addScreenShake
import kotlin.math.pow
import kotlin.random.Random

/**
 * Actually performs actions on the player and keeps track of how long it has been since each action was performed.
 *
 * Only actions can modify player state.
 *
 * This is literally only an inheritance line so I could split these into multiple files.
 */
class PlayerStateImpl : PlayerStateAccessors() {
    fun reset() {
        s.reset()
    }

    fun tick() {
        s.tick()
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
        s.setState(PlayerMode.DEAD)

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

    fun performJump() {
        if (s.state != PlayerMode.IDLE) {
            println("Player should be in the IDLE state to jump, was in ${s.state}")
        }
        s.setState(PlayerMode.INTENTIONALLY_MOVING_UP)

        val desiredUpVel = 9.0
        val velChange = min(desiredUpVel - player.body.velocity.y, desiredUpVel)
        val impulse = player.body.mass * velChange
        player.body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)

        playerRenderer.spawnParticlesAtMyFeet(number = 2)
    }
}