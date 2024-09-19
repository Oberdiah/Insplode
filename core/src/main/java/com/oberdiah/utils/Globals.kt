package com.oberdiah

import com.badlogic.gdx.Gdx
import com.oberdiah.utils.ScreenShakeSettings
import com.oberdiah.utils.calculateInputGlobals
import com.oberdiah.utils.camera

/** If this is true we add all the debug ui stuff, and also boot straight into the game. */
const val IS_DEBUG_ENABLED = true

/** If this is true, we do a bunch of extra debug checks to ensure things are behaving correctly. */
const val DEBUG_VERIFY = IS_DEBUG_ENABLED

/**
 * A multiplier on the scale of everything non-map related - Player size, bomb size, explosion size, etc.
 *
 * 2.0 is zoomed in, 0.5 is zoomed out.
 */
const val GLOBAL_SCALE = 1.0

/** The number of 'tiles' per unit on the grid. */
const val TILES_PER_UNIT = 5
const val TILES_EXTRA_FRACTION_STORED = 1.3

var SHOW_FRAMERATE_DATA = false
var ENABLED_PARTICLES = true
var DO_PHYSICS_DEBUG_RENDER = false
var LEFT_RIGHT_CONTROL = ControlScheme.LeftRightTap
var JUMP_CONTROL = ControlScheme.JumpButton
var JUMP_UI_FRACT = 0.15
var LEFT_BUTTON_UI_FRACT = 0.45
var RIGHT_BUTTON_UI_FRACT = 0.55
var SCREEN_SHAKE_SETTING = ScreenShakeSettings.Normal
var RENDER_JUMP_BUTTON = true

var HIGH_SCORE = 0.0

enum class ControlScheme(val title: String) {
    LeftRightTap("Left/Right"),
    MoveToFinger("Move to Finger"),
    JumpButton("Jump Button"),
    SwipeUp("Swipe Up"),
    AbilityButton("Ability Button"),
    TapPlayer("Tap On Player"),
}

enum class Screen(val title: String) {
    Paused("Paused"),
    Settings("Settings"),
    EndGame("Game Over"),
    MainMenu("Bomb Survival"),
    CustomGame("Custom Game"),
    AdvancedSettings("Advanced\nSettings"),
    Controls("Controls"),
    ChangeButtonPositions(""),
    Credits("Credits"),
}

/// Size of the screen in pixels
var WIDTH = 0.0

/// height of the screen in pixels
var HEIGHT = 0.0

/**
 * The number of tiles tall our big tiles array is
 * somewhat larger than what's visible on screen
 */
var SIMULATED_REGION_NUM_TILES_HIGH = 0

const val DEPTH_UNITS = " blocks"

const val PLAYER_PHYSICS_MASK: Short = 0x2
const val BOMB_PHYSICS_MASK: Short = 0x4
const val PICKUP_PHYSICS_MASK: Short = 0x8

val TEXT_SIDE_OFFSET
    get() = 0.05 * WIDTH
val TEXT_CHECKBOX_OFFSET_RIGHT
    get() = 0.03 * WIDTH

// For stuff that relies on runtime constants such as the height of the screen
fun setGlobalsThisFrame() {
    HEIGHT = Gdx.graphics.height.d
    WIDTH = Gdx.graphics.width.d
    SIMULATED_REGION_NUM_TILES_HIGH =
        (SCREEN_HEIGHT_IN_UNITS * TILES_PER_UNIT * TILES_EXTRA_FRACTION_STORED).i
}

fun setCameraGlobalsThisFrame() {
    requestNewLowestTileY((CAMERA_POS_Y * TILES_PER_UNIT - 1).i)
}

/** The size of 1m in pixels */
val UNIT_SIZE_IN_PIXELS: Double
    get() = WIDTH / UNITS_WIDE

/** In game units */
const val UNITS_WIDE = 10

/** The number of tiles across the world. */
const val NUM_TILES_ACROSS = UNITS_WIDE * TILES_PER_UNIT + 1

val SCREEN_HEIGHT_IN_UNITS: Double
    get() = HEIGHT / UNIT_SIZE_IN_PIXELS

val SAFE_BOMB_SPAWN_HEIGHT
    // Add 5 to be super duper safe.
    get() = CAMERA_POS_Y + SCREEN_HEIGHT_IN_UNITS * TILES_EXTRA_FRACTION_STORED + 5

val JUST_UP_OFF_SCREEN
    get() = CAMERA_POS_Y + SCREEN_HEIGHT_IN_UNITS

const val GRAVITY = 20.0 * GLOBAL_SCALE

val PLAYER_SPAWN_Y
    get() = SCREEN_HEIGHT_IN_UNITS * CURRENT_PLAYER_Y_FRACT + 5

val CAMERA_SPAWN_Y
    get() = SCREEN_HEIGHT_IN_UNITS * CURRENT_PLAYER_Y_FRACT - 10

const val TILE_SIZE_IN_UNITS = 1.0 / TILES_PER_UNIT

/** The player y fract most of the time */
const val BASE_PLAYER_Y_FRACT = 0.6

/** The player y fract when they're high in the air */
const val ELEVATED_PLAYER_Y_FRACT = 0.8

/**
 * The position of the player up the screen - where the camera likes to keep them.
 * This can change dynamically depending on what the player is up to.
 */
var CURRENT_PLAYER_Y_FRACT = BASE_PLAYER_Y_FRACT

var DEBUG_STRING = ""

const val TIME_STEP = 1 / 120.0f
var DELTA = 0.016
var PAUSED = !IS_DEBUG_ENABLED

/**
 * The camera Y position (bottom of the screen) in world coordinates.
 *
 * This takes screen shake into account - should be pixel accurate.
 */
val CAMERA_POS_Y: Double
    get() = camera.position.y.d - SCREEN_HEIGHT_IN_UNITS / 2
var CAMERA_LOCKED = true

var APP_FRAME = 0
var APP_TIME = 0.0

private val screenSize: Size = Size()
val SIZE: Size
    get() {
        screenSize.h = HEIGHT
        screenSize.w = WIDTH
        return screenSize
    }

private val LAST_HUNDRED_DELTAS = mutableListOf<Double>()
val AVERAGE_DELTA: Double
    get() = LAST_HUNDRED_DELTAS.average()

fun calculateGlobals() {
    DELTA = Gdx.graphics.deltaTime.d
    LAST_HUNDRED_DELTAS.add(DELTA)
    if (LAST_HUNDRED_DELTAS.size > 100) {
        LAST_HUNDRED_DELTAS.removeAt(0)
    }
    APP_FRAME++
    APP_TIME += DELTA

    calculateInputGlobals()
}