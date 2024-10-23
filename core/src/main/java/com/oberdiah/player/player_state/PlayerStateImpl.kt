package com.oberdiah.player.player_state

import com.badlogic.gdx.audio.Music
import com.oberdiah.Bomb
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.MusicCoordinator
import com.oberdiah.Point
import com.oberdiah.ScoreSystem
import com.oberdiah.Velocity
import com.oberdiah.abs
import com.oberdiah.boom
import com.oberdiah.clamp
import com.oberdiah.i
import com.oberdiah.max
import com.oberdiah.min
import com.oberdiah.playPlayerLandSound
import com.oberdiah.player.PLAYER_SIZE
import com.oberdiah.player.Player
import com.oberdiah.player.PlayerInfoBoard
import com.oberdiah.player.PlayerRenderer
import com.oberdiah.player.player
import com.oberdiah.spawnSmoke
import com.oberdiah.utils.addScreenShake
import com.oberdiah.utils.colorScheme
import kotlin.math.pow
import kotlin.random.Random

/**
 * Actually performs actions on the player and keeps track of how long it has been since each action was performed.
 *
 * Only actions can modify player state.
 *
 * This is literally only an inheritance line so I could split these into multiple files.
 *
 * Imagine these function names all start with 'player has ...'
 */
class PlayerStateImpl : PlayerStateAccessors() {
    fun justStartedASlam() {
        if (s.state != PlayerMode.INTENTIONALLY_MOVING_UP) {
            println("Player should be in the INTENTIONALLY_MOVING_UP state to start a slam, was in ${s.state}")
        }
        s.setState(PlayerMode.SLAMMING)
    }

    fun justSlammedIntoABomb(bomb: Bomb) {
        if (s.state != PlayerMode.SLAMMING) {
            println("Player should be in the SLAMMING state to slam into a bomb, was in ${s.state}")
        }

        s.setState(PlayerMode.INTENTIONALLY_MOVING_UP)
        bomb.gotSlammed()

        val desiredVel =
            clamp(
                abs(PlayerInfoBoard.slammingVelocity).pow(0.5) + bomb.power * 2.0 + 3.5,
                5.0,
                15.0
            )
        player.body.velocity = Point(player.body.velocity.x, desiredVel)
    }

    fun justSlammedIntoTheGround() {
        if (s.state != PlayerMode.SLAMMING) {
            println("Player should be in the SLAMMING state to slam into the ground, was in ${s.state}")
        }
        s.setState(PlayerMode.IDLE)

        val multiplier = ScoreSystem.getCurrentMultiplier()

        ScoreSystem.registerCasuallyLanded()

        val vel = abs(PlayerInfoBoard.slammingVelocity)
        PlayerRenderer.spawnParticlesAtMyFeet(
            ferocity = vel * 0.2,
            number = max((vel * 0.5).i, 2)
        )

        player.body.velocity = Point(player.body.velocity.x, 0.0)

        if (vel > 10) {
            // Player landing deserves more than the normal amount of shake
            addScreenShake(vel * 0.025)
            boom(
                player.body.p,
                vel * 0.03 * multiplier.pow(0.6),
                affectsThePlayer = false,
                affectsTheLandscape = false
            )
        }
    }

    fun justDied(reason: Player.DeathReason) {
        deathReason = reason

        s.setState(PlayerMode.DEAD)

        player.body.linearDamping = Float.MAX_VALUE
        // Spawn a bunch of smoke in the shape of the player
        for (i in 0 until 100) {
            val pos = player.body.p + Point(
                Random.nextDouble(-PLAYER_SIZE.x / 2, PLAYER_SIZE.x / 2),
                Random.nextDouble(-PLAYER_SIZE.y / 2, PLAYER_SIZE.y / 2)
            )
            val vel = Velocity(Random.nextDouble(-0.5, 0.5), Random.nextDouble(-0.5, 0.5))
            if (i % 2 == 0) {
                spawnSmoke(pos, vel)
            } else {
                spawnSmoke(pos, vel, color = colorScheme.player)
            }
        }
    }

    fun justPerformedAJump() {
        if (s.state != PlayerMode.IDLE) {
            println("Player should be in the IDLE state to jump, was in ${s.state}")
        }
        s.setState(PlayerMode.INTENTIONALLY_MOVING_UP)

        val desiredUpVel = 9.0
        val velChange = min(desiredUpVel - player.body.velocity.y, desiredUpVel)
        val impulse = player.body.mass * velChange
        player.body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)

        PlayerRenderer.spawnParticlesAtMyFeet(number = 2)
    }

    /**
     * We can casually land in two different ways - either we don't slam at all, or we tried to
     * slam but didn't build up enough speed.
     */
    fun justCasuallyLanded() {
        if (!(s.state == PlayerMode.INTENTIONALLY_MOVING_UP || s.state == PlayerMode.SLAMMING)) {
            println("Player should be in either int_mov_up or slamming states to land on the ground, was in ${s.state}")
        }
        s.setState(PlayerMode.IDLE)
        playPlayerLandSound()

        ScoreSystem.registerCasuallyLanded()
    }
}