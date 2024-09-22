package com.oberdiah.player.player_state

import com.oberdiah.DELTA

/**
 * Holds internal classes the player state may need
 */
open class PlayerStateClasses {
    protected enum class PlayerMode {
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

    protected class PlayerStateHandler {
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

    protected var s = PlayerStateHandler()
}