package com.oberdiah.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.APP_TIME
import com.oberdiah.DELTA
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.Size
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.WIDTH
import com.oberdiah.clamp
import com.oberdiah.d
import com.oberdiah.f
import com.oberdiah.fontLarge
import com.oberdiah.fontMedium
import com.oberdiah.fontSmallish
import com.oberdiah.frameAccurateLerp
import com.oberdiah.lastScore
import com.oberdiah.lerp
import com.oberdiah.playerScore
import com.oberdiah.sin
import com.oberdiah.statefulHighScore
import com.oberdiah.toUISpace
import com.oberdiah.toWorldSpace
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.setCameraY
import com.oberdiah.utils.startCameraToDiegeticMenuTransition

const val LAUNCH_TEXT_HEIGHT = 4.5
const val MENU_ZONE_BOTTOM_Y = 6.0
const val LOWEST_DIEGETIC_CAMERA_Y = -5.0

fun goToDiegeticMenu() {
    GAME_STATE = GameState.TransitioningToDiegeticMenu
    // On completion of the camera movement, we refresh the game.
    startCameraToDiegeticMenuTransition()
    cameraY = MENU_ZONE_BOTTOM_Y
}

var delayedPreviousFingerY = 0.0
var launchTextAlpha = 1.0f
var lastFingerY = 0.0
var isDragging = false
var cameraVelocity = 0.0
var cameraY = MENU_ZONE_BOTTOM_Y

// The diegetic menu is always there and rendered using in-world coordinates.
fun renderDiegeticMenu(r: Renderer) {
    val H = SCREEN_HEIGHT_IN_UNITS
    val W = SCREEN_WIDTH_IN_UNITS.d

    r.text(
        fontLarge,
        "BombVille",
        toUISpace(Point(W / 2, MENU_ZONE_BOTTOM_Y + H * 3 / 4)),
        Align.center
    )

    if (statefulHighScore.value != 0) {
        r.text(
            fontSmallish,
            "High Score: ${statefulHighScore.value}",
            toUISpace(Point(W / 2, MENU_ZONE_BOTTOM_Y + H * 3 / 4 - 2.0)),
            Align.center
        )
    }

    if (lastScore != null) {
        r.text(
            fontSmallish,
            "Score: $lastScore",
            toUISpace(Point(W / 2, MENU_ZONE_BOTTOM_Y + H * 3 / 4 - 3.0)),
            Align.center
        )
    }

    val launchTextColor = colorScheme.textColor.cpy()

    var isLaunchTapped = false
    if (GAME_STATE == GameState.DiegeticMenu) {
        TOUCHES_WENT_DOWN.forEach {
            if (!isInLaunchZone(it)) {
                isDragging = true
                lastFingerY = it.y
            }
        }
        TOUCHES_DOWN.forEach {
            if (isDragging) {
                val dragDelta = (lastFingerY - it.y) / UNIT_SIZE_IN_PIXELS
                cameraY += dragDelta
                lastFingerY = it.y
            }
            if (isInLaunchZone(it)) {
                isLaunchTapped = true
            }
        }
        TOUCHES_WENT_UP.forEach {
            if (isInLaunchZone(it) && !isDragging) {
                GAME_STATE = GameState.InGame
            }
            if (isDragging) {
                cameraVelocity =
                    ((delayedPreviousFingerY - it.y) / UNIT_SIZE_IN_PIXELS) / clamp(
                        DELTA,
                        0.005,
                        0.020
                    )
                isDragging = false
            }
        }

        delayedPreviousFingerY = lerp(delayedPreviousFingerY, lastFingerY, 0.5)
        cameraY += cameraVelocity * DELTA
        cameraVelocity *= 0.95
        val lowestCameraY = LOWEST_DIEGETIC_CAMERA_Y
        val highestCameraY = MENU_ZONE_BOTTOM_Y

        // Soft clamp the camera y and make it bounce

        if (cameraY < lowestCameraY) {
            cameraY = lerp(cameraY, lowestCameraY, 0.1)
            cameraVelocity = 0.0
        } else if (cameraY > highestCameraY) {
            cameraY = lerp(cameraY, highestCameraY, 0.1)
            cameraVelocity = 0.0
        }

        setCameraY(cameraY)
    }

    if (isLaunchTapped || GAME_STATE == GameState.InGame) {
        launchTextColor.add(Color(0.4f, 0.4f, 0.4f, 0.0f))
    }

    launchTextAlpha =
        if (GAME_STATE == GameState.DiegeticMenu || GAME_STATE == GameState.TransitioningToDiegeticMenu) {
            frameAccurateLerp(launchTextAlpha, 1.0f, 10.0).f
        } else {
            frameAccurateLerp(launchTextAlpha, 0.0f, 10.0).f
        }

    launchTextColor.a = launchTextAlpha

    r.color = launchTextColor

    r.text(
        fontMedium,
        "Launch!",
        toUISpace(Point(W / 2, MENU_ZONE_BOTTOM_Y + LAUNCH_TEXT_HEIGHT)),
        Align.center
    )

    val chevronDistanceBelow = 2.0 + sin(APP_TIME) * 0.3

    // Left part of the arrow
    r.centeredRect(
        toUISpace(
            Point(
                W / 2 + W / 32,
                MENU_ZONE_BOTTOM_Y + LAUNCH_TEXT_HEIGHT - chevronDistanceBelow
            )
        ),
        Size(WIDTH / 10, WIDTH / 75),
        Math.PI / 4
    )

    r.centeredRect(
        toUISpace(
            Point(
                W / 2 - W / 32,
                MENU_ZONE_BOTTOM_Y + LAUNCH_TEXT_HEIGHT - chevronDistanceBelow
            )
        ),
        Size(WIDTH / 10, WIDTH / 75),
        -Math.PI / 4
    )
}

private fun isInLaunchZone(screenSpaceTouch: Point): Boolean {
    val worldSpaceTouch = toWorldSpace(screenSpaceTouch)
    return worldSpaceTouch.y < MENU_ZONE_BOTTOM_Y + LAUNCH_TEXT_HEIGHT &&
            worldSpaceTouch.y > MENU_ZONE_BOTTOM_Y + 1.0 &&
            worldSpaceTouch.x > SCREEN_WIDTH_IN_UNITS * (1.0 / 4) &&
            worldSpaceTouch.x < SCREEN_WIDTH_IN_UNITS * (3.0 / 4)
}