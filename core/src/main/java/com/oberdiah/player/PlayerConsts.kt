package com.oberdiah.player

import com.oberdiah.TILE_SIZE_IN_UNITS

const val COYOTE_TIME = 0.15
const val PLAYER_GRAVITY_MODIFIER = 0.5
const val JUMP_BUILD_UP_TIME = 1.0

/** The x-zone in which the player will no longer be moved closer to where they want to be */
const val PLAYER_UNCERTAINTY_WINDOW = TILE_SIZE_IN_UNITS * 0.5

/** Below this, slams don't happen */
const val MINIMUM_SLAM_VELOCITY = 5.0