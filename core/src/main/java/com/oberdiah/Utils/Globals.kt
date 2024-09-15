package com.oberdiah

import com.badlogic.gdx.Gdx
import com.oberdiah.Utils.calculateInputGlobals
import com.oberdiah.Utils.camera

/** If this is true we add all the debug ui stuff, and also boot straight into the game. */
const val IS_DEBUG_ENABLED = true

/**
 * A multiplier on the scale of everything non-map related - Player size, bomb size, explosion size, etc.
 *
 * 2.0 is zoomed in, 0.5 is zoomed out.
 */
const val GLOBAL_SCALE = 1.0

/** The number of 'simples' per unit on the grid. */
const val SIMPLES_RESOLUTION = 5
const val SIMPLES_EXTRA_STORED = 1.3
// Can make really large if you're having issues with negatives.
// But then again, other rendering gets weird enough we probably shouldn't.

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

/** The number of simples across the world. */
var SIMPLES_WIDTH = 0

/// Size of the screen in pixels
var WIDTH = 0.0

/// height fo the screen in pixels
var HEIGHT = 0.0
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

// For stuff that's too complex to evaluate each time it's needed.
fun setGlobalsThisFrame() {
    HEIGHT = Gdx.graphics.height.d
    WIDTH = Gdx.graphics.width.d
    SIMPLES_WIDTH = WORLD_WIDTH * SIMPLES_RESOLUTION + 1
    SIMPLES_HEIGHT_STORED = (SQUARES_TALL * SIMPLES_RESOLUTION * SIMPLES_EXTRA_STORED).i
}

fun setCameraGlobalsThisFrame() {
    requestNewLowestSimpleY((CAMERA_POS_Y * SIMPLES_RESOLUTION - 1).i)
}

val SQUARE_SIZE_IN_PIXELS: Double
    get() = WIDTH / WORLD_WIDTH

val SQUARES_WIDE: Int
    get() = WORLD_WIDTH

val SQUARES_TALL: Double
    get() = HEIGHT / SQUARE_SIZE_IN_PIXELS

var APP_FRAME = 0
var APP_TIME = 0.0
var WORLD_WIDTH = 10

val SAFE_BOMB_SPAWN_HEIGHT
    // Add 5 to be super duper safe.
    get() = JUST_UP_OFF_SCREEN * SIMPLES_EXTRA_STORED + 5

val JUST_UP_OFF_SCREEN
    get() = CAMERA_POS_Y + SQUARES_TALL

const val GRAVITY = 20.0 * GLOBAL_SCALE
val UI_MAX_FADE_IN = 0.9
val PLAYER_SPAWN_Y
    get() = SQUARES_TALL * PLAYER_Y_FRACT + 15

val CAMERA_SPAWN_Y
    get() = SQUARES_TALL * PLAYER_Y_FRACT

val LAND_SURFACE_Y
    get() = 10

val SIMPLE_SIZE_IN_WORLD
    get() = 1.0 / SIMPLES_RESOLUTION

/// The position of the player up the screen - where the camera likes to keep them.
const val PLAYER_Y_FRACT = 0.6

var DEBUG_STRING = ""

const val TIME_STEP = 1 / 120.0f
var DELTA = 0.016
var PAUSED = !IS_DEBUG_ENABLED

val CAMERA_POS_Y: Number
    get() = camera.position.y.d - SQUARES_TALL / 2
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