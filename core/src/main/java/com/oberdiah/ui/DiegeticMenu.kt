package com.oberdiah.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.HEIGHT
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.Size
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.WIDTH
import com.oberdiah.abs
import com.oberdiah.ceil
import com.oberdiah.clamp
import com.oberdiah.d
import com.oberdiah.f
import com.oberdiah.fontLarge
import com.oberdiah.fontMedium
import com.oberdiah.fontSmallish
import com.oberdiah.frameAccurateLerp
import com.oberdiah.lastScore
import com.oberdiah.lerp
import com.oberdiah.sin
import com.oberdiah.statefulHighScore
import com.oberdiah.toUISpace
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.setCameraY
import com.oberdiah.utils.startCameraToDiegeticMenuTransition
import com.oberdiah.withAlpha

const val MENU_ZONE_BOTTOM_Y = 6.0
val UPGRADES_SCREEN_BOTTOM_Y
    get() = ceil(MENU_ZONE_BOTTOM_Y) + ceil(SCREEN_HEIGHT_IN_UNITS)
val MENU_ZONE_TOP_Y
    get() = UPGRADES_SCREEN_BOTTOM_Y

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
fun renderDiegeticMenuWorldSpace(r: Renderer) {
    val H = SCREEN_HEIGHT_IN_UNITS
    val W = SCREEN_WIDTH_IN_UNITS.d

    UpgradeController.renderUpgradeMenuWorldSpace(r)

    r.color = colorScheme.textColor

    r.text(
        fontLarge,
        "BombVille",
        W / 2, MENU_ZONE_BOTTOM_Y + H * 3 / 4,
        Align.center
    )

    if (statefulHighScore.value != 0) {
        r.text(
            fontSmallish,
            "High Score: ${statefulHighScore.value}",
            W / 2, MENU_ZONE_BOTTOM_Y + H * 3 / 4 - 2.0,
            Align.center,
            shouldCache = false
        )
    }

    if (lastScore != null) {
        r.text(
            fontSmallish,
            "Score: $lastScore",
            W / 2, MENU_ZONE_BOTTOM_Y + H * 3 / 4 - 3.0,
            Align.center,
            shouldCache = false
        )
    }
}

private var isLaunchTapped = false

fun renderDiegeticMenuScreenSpace(r: Renderer) {
    val launchTextColor = colorScheme.textColor.cpy()

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

    val chevronDistanceBelow = SCREEN_HEIGHT_IN_UNITS / 15 - sin(GameTime.APP_TIME) * 0.15
    r.color = colorScheme.textColor
    drawChevron(r, MENU_ZONE_TOP_Y - chevronDistanceBelow)

    if (launchTextAlpha > 0.001) {
        r.color = colorScheme.backgroundA.withAlpha(launchTextAlpha * 0.75)
        r.centeredRect(launchButtonPos, launchButtonSize, 0.0)
        r.color = launchTextColor

        println(r.color)

        r.centeredHollowRect(launchButtonPos, launchButtonSize, WIDTH / 150)
        if (UpgradeController.playerHas(Upgrade.Slam)) {
            r.text(fontMedium, "Launch!", launchButtonPos, Align.center)
        } else {
            r.text(fontMedium, "Drop!", launchButtonPos, Align.center)
        }
    }
}

fun tickDiegeticMenu() {
    if (GAME_STATE == GameState.DiegeticMenu) {
        var newCameraY = cameraY

        isLaunchTapped = false

        TOUCHES_WENT_DOWN.forEach {
            if (!isInLaunchButton(it)) {
                cameraVelocity = 0.0
                isDragging = true
                lastFingerY = it.y
                delayedPreviousFingerY = it.y
            }
        }
        TOUCHES_DOWN.forEach {
            if (isDragging) {
                cameraVelocity = 0.0

                val dragDelta = (lastFingerY - it.y) / UNIT_SIZE_IN_PIXELS
                newCameraY += dragDelta
                lastFingerY = it.y
            }
            if (isInLaunchButton(it)) {
                isLaunchTapped = true
            }
        }
        TOUCHES_WENT_UP.forEach {
            if (isInLaunchButton(it) && !isDragging) {
                GAME_STATE = GameState.InGame
            }
            if (isDragging) {
                val fingerDist = delayedPreviousFingerY - it.y
                if (fingerDist.abs > 5.0) {
                    cameraVelocity =
                        1.25 * (fingerDist / UNIT_SIZE_IN_PIXELS) / clamp(
                            GameTime.GRAPHICS_DELTA,
                            0.005,
                            0.020
                        )
                }

                isDragging = false
            }
        }

        delayedPreviousFingerY = lerp(delayedPreviousFingerY, lastFingerY, 0.5)
        newCameraY += cameraVelocity * GameTime.GAMEPLAY_DELTA
        cameraVelocity *= 0.95
        val lowestCameraY = MENU_ZONE_BOTTOM_Y
        val highestCameraY =
            UpgradeController.TOP_OF_UPGRADE_SCREEN_UNITS + 1 - SCREEN_HEIGHT_IN_UNITS

        // Soft clamp the camera y and make it bounce

        if (newCameraY < lowestCameraY) {
            newCameraY = lerp(newCameraY, lowestCameraY, 0.1)
            cameraVelocity = 0.0
        } else if (newCameraY > highestCameraY) {
            newCameraY = lerp(newCameraY, highestCameraY, 0.1)
            cameraVelocity = 0.0
        }

        val cameraYDelta = newCameraY - cameraY
        UpgradeController.cameraHasMoved(cameraYDelta)

        cameraY = newCameraY
        setCameraY(cameraY)
    }
}

private val launchButtonSize
    get() = Size(WIDTH / 3, HEIGHT / 20)
private val launchButtonPos
    get() = Point(WIDTH / 2, HEIGHT / 10)

private fun isInLaunchButton(touch: Point): Boolean {
    return touch.x > launchButtonPos.x - launchButtonSize.w / 2 &&
            touch.x < launchButtonPos.x + launchButtonSize.w / 2 &&
            touch.y > launchButtonPos.y - launchButtonSize.h / 2 &&
            touch.y < launchButtonPos.y + launchButtonSize.h / 2
}

private fun drawChevron(r: Renderer, chevronUnitsY: Double) {
    val W = SCREEN_WIDTH_IN_UNITS.d

    // Left part of the arrow
    r.centeredRect(
        Point(
            W / 2 + W / 32,
            chevronUnitsY
        ).ui,
        Size(WIDTH / 10, WIDTH / 75),
        -Math.PI / 4
    )

    r.centeredRect(
        Point(
            W / 2 - W / 32,
            chevronUnitsY
        ).ui,
        Size(WIDTH / 10, WIDTH / 75),
        Math.PI / 4
    )
}