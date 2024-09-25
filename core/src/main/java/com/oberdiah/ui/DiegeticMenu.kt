package com.oberdiah.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.APP_TIME
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.Size
import com.oberdiah.WIDTH
import com.oberdiah.d
import com.oberdiah.f
import com.oberdiah.fontLarge
import com.oberdiah.fontMedium
import com.oberdiah.frameAccurateLerp
import com.oberdiah.restartGame
import com.oberdiah.sin
import com.oberdiah.toUISpace
import com.oberdiah.toWorldSpace
import com.oberdiah.utils.ColorScheme
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.startCameraToDiegeticMenuTransition

const val MENU_ZONE_BOTTOM_Y = 6

fun goToDiegeticMenu() {
    GAME_STATE = GameState.TransitioningToDiegeticMenu
    // On completion of the camera movement, we refresh the game.
    startCameraToDiegeticMenuTransition()
}

var launchTextAlpha = 0.8f

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

    val launchTextColor = colorScheme.textColor.cpy()
    val launchTextHeight = H * 3 / 16.0

    var isLaunchTapped = false
    if (GAME_STATE == GameState.DiegeticMenu) {
        TOUCHES_DOWN.forEach {
            val worldSpaceTouch = toWorldSpace(it)
            if (worldSpaceTouch.y < MENU_ZONE_BOTTOM_Y + launchTextHeight && it.y > MENU_ZONE_BOTTOM_Y) {
                isLaunchTapped = true
            }
        }
        TOUCHES_WENT_UP.forEach {
            val worldSpaceTouch = toWorldSpace(it)
            if (worldSpaceTouch.y < MENU_ZONE_BOTTOM_Y + launchTextHeight && it.y > MENU_ZONE_BOTTOM_Y) {
                GAME_STATE = GameState.InGame
            }
        }
    }

    if (isLaunchTapped || GAME_STATE == GameState.InGame) {
        launchTextColor.add(Color(0.4f, 0.4f, 0.4f, 0.0f))
    }

    launchTextAlpha = if (GAME_STATE == GameState.InGame) {
        frameAccurateLerp(launchTextAlpha, 0.0f, 10.0).f
    } else {
        frameAccurateLerp(launchTextAlpha, 0.8f, 10.0).f
    }

    launchTextColor.a = launchTextAlpha

    r.color = launchTextColor

    r.text(
        fontMedium,
        "Launch!",
        toUISpace(Point(W / 2, MENU_ZONE_BOTTOM_Y + launchTextHeight)),
        Align.center
    )

    val chevronDistanceBelow = 2.0 + sin(APP_TIME) * 0.3

    // Left part of the arrow
    r.centeredRect(
        toUISpace(
            Point(
                W / 2 + W / 32,
                MENU_ZONE_BOTTOM_Y + launchTextHeight - chevronDistanceBelow
            )
        ),
        Size(WIDTH / 10, WIDTH / 75),
        Math.PI / 4
    )

    r.centeredRect(
        toUISpace(
            Point(
                W / 2 - W / 32,
                MENU_ZONE_BOTTOM_Y + launchTextHeight - chevronDistanceBelow
            )
        ),
        Size(WIDTH / 10, WIDTH / 75),
        -Math.PI / 4
    )
}