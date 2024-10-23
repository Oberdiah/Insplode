package com.oberdiah.ui

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.HEIGHT
import com.oberdiah.Rect
import com.oberdiah.Renderer
import com.oberdiah.ScoreSystem
import com.oberdiah.Screen
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.isKeyJustPressed
import com.oberdiah.WIDTH
import com.oberdiah.max
import com.oberdiah.player.Player
import com.oberdiah.statefulPauseSide
import com.oberdiah.utils.endTheGame
import com.oberdiah.withAlpha

object PauseButton {
    private val PAUSE_SIZE_FRACT = 0.12
    private val PAUSE_FROM_TOP_FRACT = 0.05
    private val PAUSE_FROM_EDGE_FRACT = 0.05
    private val PAUSE_SIZE
        get() = PAUSE_SIZE_FRACT * WIDTH
    private val PAUSE_FROM_TOP
        get() = PAUSE_FROM_TOP_FRACT * WIDTH
    private val PAUSE_FROM_EDGE
        get() = PAUSE_FROM_EDGE_FRACT * WIDTH

    private val pauseRect = Rect()
    private val PAUSE_RECT: Rect
        get() {
            pauseRect.p.x =
                if (statefulPauseSide.value == Align.left) PAUSE_FROM_EDGE else WIDTH - PAUSE_SIZE - PAUSE_FROM_EDGE
            pauseRect.p.y = HEIGHT - PAUSE_SIZE - PAUSE_FROM_TOP
            pauseRect.s.w = PAUSE_SIZE
            pauseRect.s.h = PAUSE_SIZE
            return pauseRect
        }

    private var pauseUIFadeOff = 0.0
    private var pauseHovered = false

    private var gameStateBeforePauseMenu = GameState.DiegeticMenu

    private lateinit var settingsSprite: Sprite

    fun isEatingInputs(): Boolean {
        if (GAME_STATE == GameState.PausedPopup) {
            return true
        }

        var isEaten = false
        TOUCHES_DOWN.forEach {
            if (PAUSE_RECT.contains(it)) {
                isEaten = true
            }
        }

        return isEaten
    }

    fun init() {
        settingsSprite = Sprite(Texture("Icons/Settings.png"))
    }

    fun tick() {
        pauseHovered = false
        TOUCHES_DOWN.forEach {
            if (PAUSE_RECT.contains(it)) {
                pauseHovered = true
            }
        }
        TOUCHES_WENT_UP.forEach {
            if (PAUSE_RECT.contains(it)) {
                pauseHovered = true
                pauseUIFadeOff = 1.0
                gameStateBeforePauseMenu = GAME_STATE
                GAME_STATE = GameState.PausedPopup
                switchScreen(Screen.Paused)
            }
        }
        // Escape key pauses the game.
        if (isKeyJustPressed(Keys.ESCAPE)) {
            gameStateBeforePauseMenu = GAME_STATE
            GAME_STATE = GameState.PausedPopup
            switchScreen(Screen.Paused)
        }
    }

    fun pausedUI(r: Renderer) {
        if (gameStateBeforePauseMenu == GameState.InGame) {
            button(r, "Continue") {
                GAME_STATE = GameState.InGame
            }
        }
        button(r, "Settings") {
            switchScreen(Screen.Settings)
        }
        button(r, "Credits") {
            switchScreen(Screen.Credits)
        }
        lineBreak()
        lineBreak()
        if (gameStateBeforePauseMenu == GameState.InGame) {
            button(r, "Quit Run") {
                endTheGame(deathReason = Player.DeathReason.QuitByChoice)
            }
        } else {
            button(r, "Back") {
                GAME_STATE = gameStateBeforePauseMenu
            }
        }
    }

    fun render(r: Renderer) {
        pauseUIFadeOff *= 0.95
        if (pauseHovered) {
            r.color = Color.WHITE.withAlpha(max(pauseUIFadeOff, 0.9))
        } else {
            r.color = Color.WHITE.withAlpha(max(pauseUIFadeOff, 0.5))
        }

        if (GAME_STATE == GameState.InGame) {
            val p = PAUSE_RECT.p
            r.rect(
                p.x + PAUSE_SIZE * 0.2,
                p.y + PAUSE_SIZE * 0.1,
                PAUSE_SIZE * 0.2,
                PAUSE_SIZE * 0.8
            )
            r.rect(
                p.x + PAUSE_SIZE * 0.6,
                p.y + PAUSE_SIZE * 0.1,
                PAUSE_SIZE * 0.2,
                PAUSE_SIZE * 0.8
            )
        } else if (GAME_STATE == GameState.DiegeticMenu) {
            r.centeredSprite(settingsSprite, PAUSE_RECT.p + PAUSE_SIZE / 2, PAUSE_SIZE, r.color)
        }
    }
}