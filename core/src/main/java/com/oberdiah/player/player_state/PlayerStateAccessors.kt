package com.oberdiah.player.player_state

import com.oberdiah.player.Player

/**
 * Has all of the player state accessors. I didn't want these mixed up as there's loads.
 */
open class PlayerStateAccessors : PlayerStateClasses() {
    var deathReason: Player.DeathReason? = null

    fun reset() {
        s.reset()
    }

    fun tick() {
        s.tick()
    }


    val isSlamming: Boolean
        get() {
            return s.state == PlayerMode.SLAMMING
        }

    val timeSinceStartedSlamming: Double
        get() {
            if (!isSlamming) {
                return 0.0
            }

            return s.timeSinceWeEnteredThisState
        }

    val isDead: Boolean
        get() {
            return s.state == PlayerMode.DEAD
        }

    val isAlive: Boolean
        get() {
            return !isDead
        }

    val timeSinceDied: Double
        get() {
            if (!isDead) {
                return 0.0
            }

            return s.timeSinceWeEnteredThisState
        }

    /**
     * Note: Not necessarily always up, just started that way.
     */
    val isIntentionallyMovingUp: Boolean
        get() {
            return s.state == PlayerMode.INTENTIONALLY_MOVING_UP
        }

    val timeSinceStartedIntentionallyMovingUp: Double
        get() {
            if (!isIntentionallyMovingUp) {
                return 0.0
            }

            return s.timeSinceWeEnteredThisState
        }

    val isIdle: Boolean
        get() {
            return s.state == PlayerMode.IDLE
        }

    val timeSinceStartedIdle: Double
        get() {
            if (!isIdle) {
                return 0.0
            }

            return s.timeSinceWeEnteredThisState
        }
}