package com.oberdiah.player.player_state

import com.oberdiah.player.player
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.GameTime

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
        var state: PlayerMode = getResetState()
            private set

        var timeSinceWeEnteredThisState = 0.0
            private set

        fun tick() {
            timeSinceWeEnteredThisState += GameTime.GAMEPLAY_DELTA
        }

        private fun getResetState(): PlayerMode {
            return if (UpgradeController.playerHas(Upgrade.Slam)) {
                PlayerMode.SLAMMING
            } else {
                PlayerMode.INTENTIONALLY_MOVING_UP
            }
        }

        fun reset() {
            setState(getResetState())
            timeSinceWeEnteredThisState = 0.0
        }

        fun setState(newState: PlayerMode) {
            if (newState != state) {
                player.setGhosting(newState == PlayerMode.INTENTIONALLY_MOVING_UP)
            }

            state = newState
            timeSinceWeEnteredThisState = 0.0
        }
    }

    protected var s = PlayerStateHandler()
}