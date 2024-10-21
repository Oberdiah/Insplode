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
import com.oberdiah.ScoreSystem
import com.oberdiah.Size
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.WIDTH
import com.oberdiah.abs
import com.oberdiah.ceil
import com.oberdiah.clamp
import com.oberdiah.currentlyPlayingUpgrade
import com.oberdiah.d
import com.oberdiah.f
import com.oberdiah.fontLarge
import com.oberdiah.fontMedium
import com.oberdiah.frameAccurateLerp
import com.oberdiah.get2DShake
import com.oberdiah.lerp
import com.oberdiah.level.LASER_HEIGHT_IN_MENU
import com.oberdiah.sin
import com.oberdiah.startGame
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.upgrades.UpgradeController.getUpgradeYPos
import com.oberdiah.upgrades.UpgradeController.LAUNCH_AREA_RELATIVE_RECT
import com.oberdiah.upgrades.UpgradeController.noFundsWarningFract
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.renderStar
import com.oberdiah.utils.setCameraY
import com.oberdiah.utils.startCameraToDiegeticMenuTransition
import com.oberdiah.utils.vibrate
import com.oberdiah.withAlpha

// So that when the game ends and the camera pans back up the chance of us seeing
// the level reset is low.
const val MENU_ZONE_BOTTOM_Y = 3.0
val UPGRADES_SCREEN_BOTTOM_Y
    get() = ceil(MENU_ZONE_BOTTOM_Y) + ceil(SCREEN_HEIGHT_IN_UNITS)
val MENU_ZONE_TOP_Y
    get() = UPGRADES_SCREEN_BOTTOM_Y
var cameraVelocity = 0.0
    private set

fun goToDiegeticMenu() {
    GAME_STATE = GameState.TransitioningToDiegeticMenu
    // On completion of the camera movement, we refresh the game.
    startCameraToDiegeticMenuTransition()
    cameraY = MENU_ZONE_BOTTOM_Y
}

private var delayedPreviousFingerY = 0.0
private var launchTextAlpha = 1.0f
private var starsTextAlpha = 1.0f
private var lastFingerY = 0.0
private var draggingIndex: Int? = null
private val isDragging
    get() = draggingIndex != null

private var cameraY = MENU_ZONE_BOTTOM_Y

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
}

/**
 * In ui/screen-space
 */
fun isInLaunchButton(touch: Point): Boolean {
    val thisUpgradeYPos = getUpgradeYPos(currentlyPlayingUpgrade.value)
    return LAUNCH_AREA_RELATIVE_RECT.offsetBy(Point(0.0, thisUpgradeYPos))
        .contains(touch.wo)
}

private val coinAreaWidth
    get() = WIDTH / 4.0
private val coinAreaHeight
    get() = HEIGHT / 25
private val coinAreaTriangleWidth
    get() = WIDTH / 20
private val starsAreaPoints
    get() = listOf(
        Point(-coinAreaWidth / 2 - coinAreaTriangleWidth, 0),
        Point(-coinAreaWidth / 2, -coinAreaHeight),
        Point(coinAreaWidth / 2, -coinAreaHeight),
        Point(coinAreaWidth / 2 + coinAreaTriangleWidth, 0)
    )
val starsAreaPosition
    get() =
        Point(WIDTH / 4, HEIGHT + (1 - starsTextAlpha) * coinAreaHeight * 1.4)

fun renderDiegeticMenuScreenSpace(r: Renderer) {
    starsTextAlpha =
        if (GAME_STATE == GameState.DiegeticMenu || GAME_STATE == GameState.TransitioningToDiegeticMenu) {
            frameAccurateLerp(starsTextAlpha, 1.0f, 10.0).f
        } else {
            frameAccurateLerp(starsTextAlpha, 0.0f, 10.0).f
        }

    val chevronDistanceBelow = SCREEN_HEIGHT_IN_UNITS / 10 - sin(GameTime.APP_TIME) * 0.15
    r.color = colorScheme.textColor
    drawChevron(r, MENU_ZONE_TOP_Y - chevronDistanceBelow)

    if (starsTextAlpha > 0.001) {
        r.color = Color.DARK_GRAY.withAlpha(0.9)
        r.poly(starsAreaPoints, starsAreaPosition, 0.0)

        r.color = Color.DARK_GRAY
        r.polyLine(starsAreaPoints, starsAreaPosition, WIDTH / 150)

        val coinTextWobble = get2DShake(noFundsWarningFract() * UNIT_SIZE_IN_PIXELS * 0.5)

        val textPos = Point(
            starsAreaPosition.x - coinAreaWidth * 0.4 + coinTextWobble.x,
            starsAreaPosition.y - coinAreaHeight / 2 + coinTextWobble.y
        )

        r.color = Color.WHITE
        r.text(
            fontMedium,
            "${ScoreSystem.getPlayerTotalNumStars()}",
            textPos,
            Align.left,
            shouldCache = false
        )
        renderStar(
            r, Point(
                starsAreaPosition.x + coinAreaWidth * 0.4 + coinTextWobble.x,
                textPos.y
            ), fontMedium.capHeight * 1.25
        )
    }
}

var cameraYUnitsDeltaThisTick = 0.0
    private set

fun tickDiegeticMenu() {
    if (GAME_STATE == GameState.DiegeticMenu) {
        var newCameraY = cameraY

        TOUCHES_WENT_DOWN.forEach {
            if (!isInLaunchButton(it)) {
                cameraVelocity = 0.0
                draggingIndex = it.index
                lastFingerY = it.y
                delayedPreviousFingerY = it.y
            }
        }
        TOUCHES_DOWN.forEach {
            if (draggingIndex == it.index) {
                cameraVelocity = 0.0

                val dragDelta = (lastFingerY - it.y) / UNIT_SIZE_IN_PIXELS
                newCameraY += dragDelta
                lastFingerY = it.y
            }
        }
        TOUCHES_WENT_UP.forEach {
            if (draggingIndex == it.index) {
                val fingerDist = delayedPreviousFingerY - it.y
                if (fingerDist.abs > 5.0) {
                    cameraVelocity =
                        1.25 * (fingerDist / UNIT_SIZE_IN_PIXELS) / clamp(
                            GameTime.GRAPHICS_DELTA,
                            0.005,
                            0.020
                        )
                }

                draggingIndex = null
            }
        }

        delayedPreviousFingerY = lerp(delayedPreviousFingerY, lastFingerY, 0.5)
        newCameraY += cameraVelocity * GameTime.GAMEPLAY_DELTA
        cameraVelocity *= 0.95
        val lowestCameraY = MENU_ZONE_BOTTOM_Y
        val highestCameraY =
            LASER_HEIGHT_IN_MENU + 1.0 - SCREEN_HEIGHT_IN_UNITS

        // Soft clamp the camera y and make it bounce

        if (newCameraY < lowestCameraY) {
            newCameraY = lerp(newCameraY, lowestCameraY, 0.1)
            cameraVelocity = 0.0
        } else if (newCameraY > highestCameraY) {
            newCameraY = lerp(newCameraY, highestCameraY, 0.1)
            cameraVelocity = 0.0
        }

        cameraYUnitsDeltaThisTick = newCameraY - cameraY

        cameraY = newCameraY
        setCameraY(cameraY)
    }
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