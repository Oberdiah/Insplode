package com.oberdiah

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.oberdiah.level.LASER_HEIGHT
import com.oberdiah.level.Level
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.camera

/** If this is true we add all the debug ui stuff, and also boot straight into the game. */
const val IS_DEBUG_ENABLED = false

/** If this is true, we do a bunch of extra debug checks to ensure things are behaving correctly. */
const val DEBUG_VERIFY = IS_DEBUG_ENABLED

/**
 * A multiplier on the scale of everything non-map related - Player size, bomb size, explosion size, etc.
 *
 * 2.0 is zoomed in, 0.5 is zoomed out.
 */
const val GLOBAL_SCALE = 1.0

val ON_DESKTOP = Gdx.app.type == Application.ApplicationType.Desktop

/** The number of 'tiles' per unit on the grid. */
const val TILES_PER_UNIT = 5
const val TILES_EXTRA_FRACTION_STORED = 1.3

var SHOW_FRAMERATE_DATA = false
var DO_PHYSICS_DEBUG_RENDER = false

val SHADOW_DIRECTION_UNITS = Point(0.1, -0.1)

enum class Screen(val title: String, val note: Char) {
    Paused("Paused", 'C'),
    Settings("Settings", 'G'),
    AdvancedSettings("Advanced\nSettings", 'C'),
    Controls("Controls", 'C'),
    Credits("Credits", 'G'),
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

/** Anything that doesn't specify a mask ends up with 0x1 */
const val WORLD_PHYSICS_MASK: Short = 0x1
const val PLAYER_PHYSICS_MASK: Short = 0x2
const val BOMB_PHYSICS_MASK: Short = 0x4
const val PICKUP_PHYSICS_MASK: Short = 0x8

// Just some random number we can use to detect this.
// Used for allowing points to be collected by the player despite the player
// being in ghost mode
const val PLAYER_DETECTOR_IDENTIFIER = 0x746

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
    Level.requestNewLowestTileY((CAMERA_POS_Y * TILES_PER_UNIT - 1).i)
}

/** The size of 1m in pixels */
val UNIT_SIZE_IN_PIXELS: Double
    get() = WIDTH / UNITS_WIDE

/** In game units */
const val UNITS_WIDE = 10

/** The number of tiles across the world. */
const val NUM_TILES_ACROSS = UNITS_WIDE * TILES_PER_UNIT + 1

val WALL_HEIGHT
    get() = SCREEN_HEIGHT_IN_UNITS * 50.0

val SCREEN_HEIGHT_IN_UNITS: Double
    get() = HEIGHT / UNIT_SIZE_IN_PIXELS

val SCREEN_WIDTH_IN_UNITS: Int
    get() = UNITS_WIDE

val SAFE_BOMB_SPAWN_HEIGHT
    // Add 5 to be super duper safe.
    get() = LASER_HEIGHT + 5

val JUST_UP_OFF_SCREEN_UNITS
    get() = CAMERA_POS_Y + SCREEN_HEIGHT_IN_UNITS

const val GRAVITY = 20.0 * GLOBAL_SCALE

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

enum class GameState {
    PausedPopup,
    TransitioningToDiegeticMenu,
    DiegeticMenu,
    InGame,
}

var LAST_APP_TIME_GAME_STATE_CHANGED = Double.NEGATIVE_INFINITY
    private set

var LAST_GAME_STATE = GameState.TransitioningToDiegeticMenu

var GAME_STATE = GameState.DiegeticMenu
    set(value) {
        if (field != value) {
            LAST_APP_TIME_GAME_STATE_CHANGED = GameTime.APP_TIME
            LAST_GAME_STATE = field
        }
        field = value
    }

val GAME_IS_RUNNING get() = GAME_STATE == GameState.InGame || GAME_STATE == GameState.TransitioningToDiegeticMenu

/**
 * The camera Y position (bottom of the screen) in world coordinates.
 *
 * This takes screen shake into account - should be pixel accurate.
 */
val CAMERA_POS_Y: Double
    get() = camera.position.y.d - SCREEN_HEIGHT_IN_UNITS / 2

private val screenSize: Size = Size()
val SCREEN_SIZE: Size
    get() {
        screenSize.h = HEIGHT
        screenSize.w = WIDTH
        return screenSize
    }