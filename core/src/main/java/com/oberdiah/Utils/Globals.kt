package com.oberdiah

import com.badlogic.gdx.Gdx
import com.oberdiah.Utils.calculateInputGlobals
import com.oberdiah.Utils.camera

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

/** The number of 'simples' per unit on the grid. */
const val SIMPLES_PER_UNIT = 5
const val SIMPLES_EXTRA_STORED = 1.3

var SHOW_FRAMERATE_DATA = false
var ENABLED_PARTICLES = true
var DO_PHYSICS_DEBUG_RENDER = false
var DEBUG_MOVEMENT_MODE = false
var LEFT_RIGHT_CONTROL = ControlScheme.LeftRightTap
var JUMP_CONTROL = ControlScheme.JumpButton
var ABILITY_CONTROL = ControlScheme.AbilityButton
var JUMP_UI_FRACT = 0.15
var LEFT_BUTTON_UI_FRACT = 0.45
var RIGHT_BUTTON_UI_FRACT = 0.55
var SCREEN_SHAKE_SETTING = ScreenShakeSettings.Normal

var RENDER_JUMP_BUTTON = true
var FOLLOW_FINGER = true

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
 * The number of simples tall our big simples array is
 * somewhat larger than what's visible on screen
 */
var SIMPLES_HEIGHT_STORED = 0

enum class ScreenShakeSettings(val text: String, val shakeAmount: Number) {
    Off("Off", 0.0),
    Low("Low", 1.0),
    Normal("Normal", 2.0),
    Extreme("Extreme", 5.0),
}

var SCREEN_SHAKE = 0.0
fun addScreenShake(amount: Number) {
    SCREEN_SHAKE = max(SCREEN_SHAKE, amount.d * SCREEN_SHAKE_SETTING.shakeAmount)
}

var DEPTH_UNITS = " blocks"

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
    SIMPLES_HEIGHT_STORED = (UNITS_TALL * SIMPLES_PER_UNIT * SIMPLES_EXTRA_STORED).i
}

fun setCameraGlobalsThisFrame() {
    requestNewLowestSimpleY((CAMERA_POS_Y * SIMPLES_PER_UNIT - 1).i)
}

/** The size of 1m in pixels */
val UNIT_SIZE_IN_PIXELS: Double
    get() = WIDTH / UNITS_WIDE

/** In game units */
const val UNITS_WIDE = 10

/** The number of simples across the world. */
const val NUM_SIMPLES_ACROSS = UNITS_WIDE * SIMPLES_PER_UNIT + 1

val UNITS_TALL: Double
    get() = HEIGHT / UNIT_SIZE_IN_PIXELS

var APP_FRAME = 0
var APP_TIME = 0.0

val SAFE_BOMB_SPAWN_HEIGHT
    // Add 5 to be super duper safe.
    get() = JUST_UP_OFF_SCREEN * SIMPLES_EXTRA_STORED + 5

val JUST_UP_OFF_SCREEN
    get() = CAMERA_POS_Y + UNITS_TALL

const val GRAVITY = 20.0 * GLOBAL_SCALE
val UI_MAX_FADE_IN = 0.9
val PLAYER_SPAWN_Y
    get() = UNITS_TALL * PLAYER_Y_FRACT + 15

val CAMERA_SPAWN_Y
    get() = UNITS_TALL * PLAYER_Y_FRACT

val LAND_SURFACE_Y
    get() = 10

val SIMPLE_SIZE_IN_WORLD
    get() = 1.0 / SIMPLES_PER_UNIT

/// The position of the player up the screen - where the camera likes to keep them.
const val PLAYER_Y_FRACT = 0.6

var DEBUG_STRING = ""

const val TIME_STEP = 1 / 120.0f
var DELTA = 0.016
var PAUSED = !IS_DEBUG_ENABLED

/**
 * The camera Y position (bottom of the screen) in world coordinates.
 */
val CAMERA_POS_Y: Number
    get() = camera.position.y.d - UNITS_TALL / 2
var CAMERA_LOCKED = true

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