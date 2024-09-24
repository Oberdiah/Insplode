package com.oberdiah.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.Screen
import com.oberdiah.fontLarge
import com.oberdiah.restartGame
import com.oberdiah.toUISpace

private var UPGRADE_ZONE_BOTTOM_Y = 0.0
private var UPGRADE_ZONE_HEIGHT = 0.0
val MENU_ZONE_BOTTOM_Y
    get() = UPGRADE_ZONE_BOTTOM_Y + UPGRADE_ZONE_HEIGHT

// The diegetic menu is rendered using in-world coordinates.
fun renderDiegeticMenu(r: Renderer) {
    val H = SCREEN_HEIGHT_IN_UNITS
    val W = SCREEN_WIDTH_IN_UNITS

    r.color = Color.BLACK
    r.text(
        fontLarge,
        "BombVille",
        toUISpace(Point(W / 2, MENU_ZONE_BOTTOM_Y + H * 3 / 4)),
        Align.center
    )

    button(r, "Start") {
        GAME_STATE = GameState.InGame
        restartGame()
    }

    button(r, "Settings") {
        switchScreen(Screen.Settings)
    }

    button(r, "Credits") {
        switchScreen(Screen.Credits)
    }
}