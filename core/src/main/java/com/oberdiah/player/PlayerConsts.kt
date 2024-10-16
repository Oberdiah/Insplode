package com.oberdiah.player

import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.Point
import com.oberdiah.Size
import com.oberdiah.TILE_SIZE_IN_UNITS
import com.oberdiah.clamp
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.upgrades.UpgradeController.UPGRADE_ENTRY_HEIGHT
import com.oberdiah.upgrades.UpgradeController.getUpgradeYPos

val PLAYER_SIZE = Size(0.375, 0.7) * GLOBAL_SCALE

val DEAD_CONTEMPLATION_TIME
    get() = clamp(RUN_TIME_ELAPSED * 0.1, 1.5, 2.5)

/** The x-zone in which the player will no longer be moved closer to where they want to be */
const val PLAYER_UNCERTAINTY_WINDOW = TILE_SIZE_IN_UNITS * 0.5

/**
 * A duration in which the player cannot regain jump, to prevent them from regaining jump just after
 * a successful slam hit
 */
const val JUMP_PREVENTION_WINDOW = 0.3

val player = Player(Point(5, PLAYER_SPAWN_Y))

val PLAYER_SPAWN_Y
    get() = getUpgradeYPos(UpgradeController.currentlyPlayingUpgrade) +
            UPGRADE_ENTRY_HEIGHT + 1.5