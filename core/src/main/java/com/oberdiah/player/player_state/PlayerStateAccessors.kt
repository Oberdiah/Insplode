package com.oberdiah.player.player_state

/**
 * Has all of the player state accessors. I didn't want these mixed up as there's loads.
 */
open class PlayerStateAccessors : PlayerStateClasses() {
    protected var s = PlayerStateHandler()

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

    fun isIntentionallyMovingUp(): Boolean {
        return s.state == PlayerMode.INTENTIONALLY_MOVING_UP
    }

    fun timeSinceStartedIntentionallyMovingUp(): Double {
        if (!isIntentionallyMovingUp()) {
            return 0.0
        }

        return s.timeSinceWeEnteredThisState
    }

    fun isIdle(): Boolean {
        return s.state == PlayerMode.IDLE
    }

    fun timeSinceStartedIdle(): Double {
        if (!isIdle()) {
            return 0.0
        }

        return s.timeSinceWeEnteredThisState
    }
}