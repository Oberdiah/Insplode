package com.oberdiah.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.DEBUG_MOVEMENT_MODE
import com.oberdiah.DEBUG_STRING
import com.oberdiah.DEPTH_UNITS
import com.oberdiah.DO_PHYSICS_DEBUG_RENDER
import com.oberdiah.ENABLED_PARTICLES
import com.oberdiah.HEIGHT
import com.oberdiah.HIGH_SCORE
import com.oberdiah.IS_DEBUG_ENABLED
import com.oberdiah.JUMP_UI_FRACT
import com.oberdiah.PAUSED
import com.oberdiah.Point
import com.oberdiah.RENDER_JUMP_BUTTON
import com.oberdiah.RUN_TIME_ELAPSED
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_SHAKE_SETTING
import com.oberdiah.SHOW_FRAMERATE_DATA
import com.oberdiah.Screen
import com.oberdiah.Size
import com.oberdiah.TEXT_CHECKBOX_OFFSET_RIGHT
import com.oberdiah.TEXT_SIDE_OFFSET
import com.oberdiah.Utils.TOUCHES_DOWN
import com.oberdiah.Utils.TOUCHES_WENT_UP
import com.oberdiah.Utils.colorScheme
import com.oberdiah.Utils.nextColorScheme
import com.oberdiah.Utils.timerString
import com.oberdiah.WIDTH
import com.oberdiah.currentDepthThisRun
import com.oberdiah.d
import com.oberdiah.fontLarge
import com.oberdiah.fontMedium
import com.oberdiah.fontSmall
import com.oberdiah.fontSmallish
import com.oberdiah.format
import com.oberdiah.gameMessage
import com.oberdiah.max
import com.oberdiah.maxDepthThisRun
import com.oberdiah.next
import com.oberdiah.physicsDebugString
import com.oberdiah.player
import com.oberdiah.restartGame
import com.oberdiah.withAlpha
import java.util.*
import kotlin.math.PI
import kotlin.reflect.KMutableProperty0

var jumpUIFadeOff = 0.0
var pauseUIFadeOff = 0.0
var pauseHovered = false

private fun formatDepth(depth: Number): String {
    return "${depth.format(1)}$DEPTH_UNITS"
}

fun renderUI(r: Renderer) {
    r.color = colorScheme.textColor
    val firstLine = "FPS: ${Gdx.graphics.framesPerSecond} ${physicsDebugString()}"
    val secondLine = DEBUG_STRING
    val thirdLine = timerString
    if (SHOW_FRAMERATE_DATA) {
        r.text(fontSmall, "$firstLine\n$secondLine\n$thirdLine", 100, HEIGHT - 100)
    }

    if (PAUSED) {
        r.color = Color.BLACK.withAlpha(0.5)
        r.rect(0, 0, WIDTH, HEIGHT)


        r.color = Color.WHITE
        r.text(
            fontLarge,
            GAME_SCREEN.title.uppercase(Locale.ROOT),
            WIDTH / 2,
            HEIGHT * 3 / 4,
            Align.center
        )

        startButtonsAt(HEIGHT / 2)
        when (GAME_SCREEN) {
            Screen.Paused -> pausedUI(r)
            Screen.Settings -> settingsUI(r)
            Screen.EndGame -> endGameUI(r)
            Screen.MainMenu -> mainMenuUI(r)
            Screen.CustomGame -> customGameUI(r)
            Screen.AdvancedSettings -> advancedSettingsUI(r)
            Screen.Controls -> controlsUI(r)
            Screen.ChangeButtonPositions -> buttonPositionUI(r)
            Screen.Credits -> creditsUI(r)
        }
    } else {
        var text = "${RUN_TIME_ELAPSED.format(1)}s"
        if (gameMessage != "") {
            text = gameMessage
        }
        r.text(fontSmallish, text, WIDTH / 2, HEIGHT * 0.95, Align.center)
        r.text(
            fontMedium,
            "Depth: ${formatDepth(currentDepthThisRun)}",
            WIDTH / 2,
            HEIGHT * 0.9,
            Align.center
        )

        jumpUIFadeOff *= 0.95
        if (RENDER_JUMP_BUTTON) {
            if (player.canJump()) {
                r.color = Color.WHITE.withAlpha(max(jumpUIFadeOff, 0.5))
            } else {
                r.color = Color.WHITE.withAlpha(max(jumpUIFadeOff, 0.2))
            }
            r.rect(0, 0, WIDTH, HEIGHT * JUMP_UI_FRACT)

            val bigButton = "JUMP"

            r.text(
                fontLarge, bigButton, WIDTH / 2, HEIGHT * JUMP_UI_FRACT / 2, Align.center
            )
        } else {
            r.color = Color.WHITE.withAlpha(jumpUIFadeOff)
            r.rect(0, 0, WIDTH, HEIGHT * JUMP_UI_FRACT)
        }

        renderPauseButton(r)
    }
}

private var SCREEN_STACK = mutableListOf(Screen.MainMenu)
private val GAME_SCREEN: Screen
    get() = SCREEN_STACK.last()

fun backAScreen() {
    SCREEN_STACK.removeLast()
}

fun switchScreen(screen: Screen) {
    SCREEN_STACK.add(screen)
}

private fun creditsUI(r: Renderer) {
    r.text(fontMedium, "Game Design", WIDTH / 2, HEIGHT / 2, Align.center)
    r.text(
        fontSmallish,
        "Richard Mullender",
        WIDTH / 2,
        HEIGHT / 2 - DIST_BETWEEN_WORDS,
        Align.center
    )
    r.text(fontMedium, "Testing/QA", WIDTH / 2, HEIGHT / 2 - DIST_BETWEEN_WORDS * 3, Align.center)
    r.text(
        fontSmallish,
        "David Mullender",
        WIDTH / 2,
        HEIGHT / 2 - DIST_BETWEEN_WORDS * 4,
        Align.center
    )
    r.text(
        fontSmallish,
        "Peter Nisbet",
        WIDTH / 2,
        HEIGHT / 2 - DIST_BETWEEN_WORDS * 5,
        Align.center
    )
    uiCurrentHeight = HEIGHT / 2 - DIST_BETWEEN_WORDS * 7
    button(r, "Back") {
        backAScreen()
    }
}

private fun customGameUI(r: Renderer) {
    button(r, "Start game") {
        PAUSED = false
        restartGame()
    }
    button(r, "Back") {
        backAScreen()
    }
}

val SUBTITLE_HEIGHT
    get() = HEIGHT * 3 / 4.3

private fun settingsUI(r: Renderer) {
    toggleButton(r, "Render Jump Button", ::RENDER_JUMP_BUTTON)
    toggleButton(r, "Particles", ::ENABLED_PARTICLES)

    settingButton(r, "Color Scheme", {
        r.text(fontMedium, colorScheme.name, it, Align.right)
    }, {
        nextColorScheme()
    })

    settingButton(r, "Screen Shake", {
        r.text(fontMedium, SCREEN_SHAKE_SETTING.text, it, Align.right)
    }, {
        SCREEN_SHAKE_SETTING = SCREEN_SHAKE_SETTING.next()
    })
    button(r, "Controls") {
        switchScreen(Screen.Controls)
    }
    if (IS_DEBUG_ENABLED) {
        button(r, "Advanced Settings") {
            switchScreen(Screen.AdvancedSettings)
        }
    }
    button(r, "Back") {
        backAScreen()
    }
}

private fun advancedSettingsUI(r: Renderer) {
    toggleButton(r, "Profiling Data", ::SHOW_FRAMERATE_DATA)
    if (IS_DEBUG_ENABLED) {
        toggleButton(r, "Physics Render", ::DO_PHYSICS_DEBUG_RENDER)
        toggleButton(r, "Fly Mode", ::DEBUG_MOVEMENT_MODE)
    }
    button(r, "Back") {
        backAScreen()
    }
}

private fun mainMenuUI(r: Renderer) {
    r.text(
        fontSmallish,
        "High Score: ${formatDepth(HIGH_SCORE)}",
        WIDTH / 2,
        SUBTITLE_HEIGHT,
        Align.center
    )

    button(r, "Start") {
        PAUSED = false
        restartGame()
    }

    button(r, "Custom Game") {
        switchScreen(Screen.CustomGame)
    }

    button(r, "Settings") {
        switchScreen(Screen.Settings)
    }

    button(r, "Credits") {
        switchScreen(Screen.Credits)
    }
}

private fun endGameUI(r: Renderer) {
    r.text(
        fontSmallish,
        "Depth Reached: ${formatDepth(maxDepthThisRun)}",
        WIDTH / 2,
        HEIGHT * (3.0 / 4 - 1.0 / 16),
        Align.center
    )

    button(r, "Try again") {
        PAUSED = false
        restartGame()
    }

    button(r, "Settings") {
        switchScreen(Screen.Settings)
    }

    button(r, "Main Menu") {
        switchScreen(Screen.MainMenu)
    }
}

private fun pausedUI(r: Renderer) {
    button(r, "Continue") {
        PAUSED = false
    }
    button(r, "Settings") {
        switchScreen(Screen.Settings)
    }
    button(r, "Restart") {
        restartGame()
        PAUSED = false
    }
    lineBreak()
    lineBreak()
    button(r, "Main Menu") {
        switchScreen(Screen.MainMenu)
    }
}


val DIST_BETWEEN_WORDS = fontMedium.lineHeight * 1.25
var uiCurrentHeight = HEIGHT / 2

private fun lineBreak() {
    uiCurrentHeight -= DIST_BETWEEN_WORDS
}


private fun startButtonsAt(v: Number) {
    uiCurrentHeight = v.d
}

fun toggleButton(
    r: Renderer,
    text: String,
    t: KMutableProperty0<Boolean>,
    enabled: Boolean = true
) {
    settingButton(r, text, {
        val boxSize = DIST_BETWEEN_WORDS * 0.8
        r.color = Color.WHITE.withAlpha(0.3)
        val checkboxPoint = it
        checkboxPoint.x -= boxSize / 2
        r.centeredRect(checkboxPoint, Size(boxSize, boxSize))
        if (t.get()) {
            r.color = Color.WHITE.withAlpha(0.85)
            r.centeredRect(
                checkboxPoint + Point(boxSize * 0.1, 0),
                Size(boxSize * 0.6, boxSize * 0.1),
                PI / 4
            )
            r.centeredRect(
                checkboxPoint + Point(-boxSize * 0.15, -boxSize * 0.1),
                Size(boxSize * 0.3, boxSize * 0.1),
                -PI / 4
            )
        }
    }, {
        t.set(!t.get())
    }, Align.left, enabled)
}

fun button(r: Renderer, text: String, enabled: Boolean = true, callback: () -> Unit) {
    settingButton(r, text, {}, callback, Align.center, enabled)
}

fun settingButton(
    r: Renderer,
    text: String,
    renderOnRight: (Point) -> Unit,
    callback: () -> Unit,
    textAlign: Int = Align.left,
    enabled: Boolean = true
) {
    r.color = Color.WHITE
    if (!enabled) {
        r.color = Color.WHITE.withAlpha(0.3)
    }

    if (textAlign == Align.left) {
        r.text(fontMedium, text, TEXT_SIDE_OFFSET, uiCurrentHeight, textAlign)
    } else if (textAlign == Align.center) {
        r.text(fontMedium, text, WIDTH / 2, uiCurrentHeight, textAlign)
    }

    val buttonTop = uiCurrentHeight + DIST_BETWEEN_WORDS / 2
    val buttonBottom = uiCurrentHeight - DIST_BETWEEN_WORDS / 2

    r.rect(0, buttonBottom, WIDTH, 4)
    r.rect(0, buttonTop, WIDTH, 4)

    renderOnRight(Point(WIDTH - TEXT_CHECKBOX_OFFSET_RIGHT, uiCurrentHeight))

    uiCurrentHeight -= DIST_BETWEEN_WORDS

    if (enabled) {
        TOUCHES_DOWN.forEach {
            if (it.y > buttonBottom && it.y < buttonTop) {
                r.color = colorScheme.overlay
                r.rect(0, buttonBottom, WIDTH, DIST_BETWEEN_WORDS)
            }
        }
        TOUCHES_WENT_UP.forEach {
            if (it.y > buttonBottom && it.y < buttonTop) {
                callback()
            }
        }
    }
}