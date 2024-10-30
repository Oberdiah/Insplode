package com.oberdiah.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.DEBUG_STRING
import com.oberdiah.DEPTH_UNITS
import com.oberdiah.DO_PHYSICS_DEBUG_RENDER
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.HEIGHT
import com.oberdiah.IS_DEBUG_ENABLED
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SHOW_FRAMERATE_DATA
import com.oberdiah.ScoreSystem
import com.oberdiah.Screen
import com.oberdiah.Size
import com.oberdiah.TEXT_CHECKBOX_OFFSET_RIGHT
import com.oberdiah.TEXT_SIDE_OFFSET
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.timerString
import com.oberdiah.WIDTH
import com.oberdiah.d
import com.oberdiah.fontLarge
import com.oberdiah.fontMedium
import com.oberdiah.fontSmall
import com.oberdiah.fontSmallish
import com.oberdiah.format
import com.oberdiah.level.hasShownAnyTutorials
import com.oberdiah.level.resetTutorialSettings
import com.oberdiah.next
import com.oberdiah.physicsDebugString
import com.oberdiah.playChordNote
import com.oberdiah.rainbowPlayerEnabled
import com.oberdiah.statefulPlayMusicSetting
import com.oberdiah.statefulPlaySoundSetting
import com.oberdiah.statefulRenderParticles
import com.oberdiah.statefulScreenShakeSetting
import com.oberdiah.statefulVibrationSetting
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.vibrate
import com.oberdiah.withAlpha
import java.util.*
import kotlin.math.PI
import kotlin.reflect.KMutableProperty0

private fun formatDepth(depth: Number): String {
    return "${depth.format(1)}$DEPTH_UNITS"
}

fun renderUIScreenSpace(r: Renderer) {
    r.color = colorScheme.textColor

    if (SHOW_FRAMERATE_DATA) {
        val firstLine = "FPS: ${Gdx.graphics.framesPerSecond} ${physicsDebugString()}"
        val secondLine = DEBUG_STRING
        val thirdLine = timerString
        r.text(
            fontSmall,
            "$firstLine\n$secondLine\n$thirdLine",
            10,
            HEIGHT * 0.6,
            shouldCache = false
        )
    }

    r.color = colorScheme.textColor

    renderDiegeticMenuScreenSpace(r)

    PauseButton.render(r)

    when (GAME_STATE) {
        GameState.DiegeticMenu -> {}
        GameState.TransitioningToDiegeticMenu -> {}
        GameState.PausedPopup -> {
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
                Screen.Paused -> PauseButton.pausedUI(r)
                Screen.Settings -> settingsUI(r)
                Screen.AdvancedSettings -> advancedSettingsUI(r)
                Screen.Controls -> controlsUI(r)
                Screen.Credits -> creditsUI(r)
            }
        }

        GameState.InGame -> {
        }
    }
}

private var SCREEN_STACK = mutableListOf(Screen.Paused)
private val GAME_SCREEN: Screen
    get() = SCREEN_STACK.last()

fun backAScreen() {
    SCREEN_STACK.removeLastOrNull()
}

fun switchScreen(screen: Screen) {
    SCREEN_STACK.add(screen)
}

private fun creditsUI(r: Renderer) {
    val titleSpacing = DIST_BETWEEN_WORDS * 1.1
    val titleGap = DIST_BETWEEN_WORDS * 1.5
    val textSpacing = DIST_BETWEEN_WORDS * 0.6
    var creditsHeight = HEIGHT / 2 + DIST_BETWEEN_WORDS * 3

    r.text(fontMedium, "Code, Art & Design", WIDTH / 2, creditsHeight, Align.center)
    creditsHeight -= titleSpacing
    r.text(fontSmallish, "Richard Mullender", WIDTH / 2, creditsHeight, Align.center)
    creditsHeight -= titleGap
    r.text(fontMedium, "Game Design", WIDTH / 2, creditsHeight, Align.center)
    creditsHeight -= titleSpacing
    r.text(fontSmallish, "David Mullender", WIDTH / 2, creditsHeight, Align.center)
    creditsHeight -= titleGap
    r.text(fontMedium, "Playtesters", WIDTH / 2, creditsHeight, Align.center)
    creditsHeight -= titleSpacing
    r.text(fontSmall, "Peter Nisbet", WIDTH / 8, creditsHeight, Align.left)
    r.text(fontSmall, "Thomas Mullender", WIDTH / 2, creditsHeight, Align.left)
    creditsHeight -= textSpacing
    r.text(fontSmall, "Marion Nisbet", WIDTH / 8, creditsHeight, Align.left)
    r.text(fontSmall, "Connor Hamilton-Smith", WIDTH / 2, creditsHeight, Align.left)
    creditsHeight -= textSpacing
    r.text(fontSmall, "Peter G", WIDTH / 8, creditsHeight, Align.left)
    r.text(fontSmall, "Matthew Parkes", WIDTH / 2, creditsHeight, Align.left)
    creditsHeight -= textSpacing
    r.text(fontSmall, "Ashley Seaton", WIDTH / 8, creditsHeight, Align.left)
    creditsHeight -= titleGap
    uiCurrentHeight = creditsHeight
    button(r, "Back") {
        backAScreen()
    }
}

private fun settingsUI(r: Renderer) {
    toggleButton(r, "Particles", statefulRenderParticles::value)
    toggleButton(r, "Vibration", statefulVibrationSetting::value)
    toggleButton(r, "Music", statefulPlayMusicSetting::value)
    toggleButton(r, "Sounds", statefulPlaySoundSetting::value)

    settingButton(r, "Screen Shake", {
        r.text(fontMedium, statefulScreenShakeSetting.value.text, it, Align.right)
    }, {
        statefulScreenShakeSetting.value = statefulScreenShakeSetting.value.next()
    })

    if (ScoreSystem.playerHasFinishedTheGame()) {
        toggleButton(r, "Rainbow Player", rainbowPlayerEnabled::value)
    }
    
    button(r, "Advanced Settings") {
        switchScreen(Screen.AdvancedSettings)
    }
    button(r, "Back") {
        backAScreen()
    }
}

const val NUM_RESET_TAPS = 5
var numResetTaps = 0
private fun advancedSettingsUI(r: Renderer) {
    toggleButton(r, "Profiling Data", ::SHOW_FRAMERATE_DATA)
    if (IS_DEBUG_ENABLED) {
        toggleButton(r, "Physics Render", ::DO_PHYSICS_DEBUG_RENDER)
    }

    if (hasShownAnyTutorials()) {
        button(r, "Reset Tutorial") {
            resetTutorialSettings()
        }
    }

    button(
        r,
        if (numResetTaps == 0) "Reset Everything" else if (numResetTaps >= NUM_RESET_TAPS + 1) "Done!" else "Tap ${NUM_RESET_TAPS + 1 - numResetTaps} times to reset"
    ) {
        if (numResetTaps == NUM_RESET_TAPS) {
            ScoreSystem.resetScores()
            UpgradeController.resetUpgradeStates()
            resetTutorialSettings()
            rainbowPlayerEnabled.value = false
            numResetTaps++
        } else {
            numResetTaps++
        }
    }

    button(r, "Back") {
        numResetTaps = 0
        backAScreen()
    }
}


val DIST_BETWEEN_WORDS = fontMedium.lineHeight * 1.25
var uiCurrentHeight = HEIGHT / 2

fun lineBreak() {
    uiCurrentHeight -= DIST_BETWEEN_WORDS
}


private fun startButtonsAt(v: Number) {
    uiCurrentHeight = v.d
}

fun toggleButton(
    r: Renderer,
    text: String,
    t: KMutableProperty0<Boolean>,
    enabled: Boolean = true,
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
                playChordNote(GAME_SCREEN.note)
                vibrate(10)
                callback()
            }
        }
    }
}