package com.oberdiah.player

import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.PLAYER_SPAWN_Y
import com.oberdiah.Point
import com.oberdiah.Size
import com.oberdiah.TILE_SIZE_IN_UNITS
import com.oberdiah.div
import com.oberdiah.player.player_state.PlayerStateImpl
import com.oberdiah.times

val playerRenderer = PlayerRenderer()
val playerState = PlayerStateImpl()
val playerInfoBoard = PlayerInformationBoard()
val playerInputs = PlayerInputs()

val playerFeetPosition
    get() = player.body.p - Point(0.0, PLAYER_SIZE.w / 2 * GLOBAL_SCALE)

val PLAYER_SIZE = Size(0.375, 0.7) * GLOBAL_SCALE

const val COYOTE_TIME = 0.15
const val PLAYER_GRAVITY_MODIFIER = 0.5
const val JUMP_BUILD_UP_TIME = 1.0

/** The x-zone in which the player will no longer be moved closer to where they want to be */
const val PLAYER_UNCERTAINTY_WINDOW = TILE_SIZE_IN_UNITS * 0.5

/** Below this, slams don't happen */
const val MINIMUM_SLAM_VELOCITY = 5.0

/**
 * A duration in which the player cannot regain jump, to prevent them from regaining jump just after
 * a successful slam hit
 */
const val JUMP_PREVENTION_WINDOW = 0.3

val player = Player(Point(5, PLAYER_SPAWN_Y))