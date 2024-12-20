package com.oberdiah.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.GAME_STATE
import com.oberdiah.GameState
import com.oberdiah.HEIGHT
import com.oberdiah.MusicCoordinator
import com.oberdiah.Point
import com.oberdiah.Rect
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_HEIGHT_IN_UNITS
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.SHADOW_DIRECTION_UNITS
import com.oberdiah.ScoreSystem
import com.oberdiah.ScoreSystem.lastLevelPlayed
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
import com.oberdiah.fontSmall
import com.oberdiah.fontTiny
import com.oberdiah.frameAccurateLerp
import com.oberdiah.get2DShake
import com.oberdiah.lerp
import com.oberdiah.level.APP_TIME_GAME_STARTED
import com.oberdiah.level.LASER_HEIGHT_IN_MENU
import com.oberdiah.player.Player
import com.oberdiah.saturate
import com.oberdiah.sin
import com.oberdiah.startGame
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.upgrades.UpgradeController.getUpgradeYPos
import com.oberdiah.upgrades.UpgradeController.LAUNCH_AREA_RELATIVE_RECT
import com.oberdiah.upgrades.UpgradeController.noFundsWarningFract
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.renderButton
import com.oberdiah.utils.renderColoredStar
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
}

private var lastTimeUpdatedDelayedFingerY = 0.0
private var delayedPreviousFingerY = 0.0
private var launchTextAlpha = 1.0f
private var starsTextAlpha = 1.0f
private var lastFingerY = 0.0
private var draggingIndex: Int? = null
private val isDragging
    get() = draggingIndex != null

var diegeticCameraY = MENU_ZONE_BOTTOM_Y
private var appTimeEnteredDiegeticMenu = Double.NEGATIVE_INFINITY

var currentHintText = ""
fun registerGameEndDiegeticMenu(deathReason: Player.DeathReason) {
    appTimeEnteredDiegeticMenu = GameTime.APP_TIME
    if (deathReason == Player.DeathReason.QuitByChoice) {
        currentHintText = ""
        return
    }
    val lastUpgrade = ScoreSystem.lastLevelPlayed
    val lastScore = ScoreSystem.lastScore
    if (lastUpgrade != null && lastScore != null) {
        val stars = lastUpgrade.getStarsFromScore(lastScore)
        currentHintText = lastUpgrade.getHintText(stars, deathReason)
    }
}

fun getMainMenuStarPosition(star: Int): Point {
    val H = SCREEN_HEIGHT_IN_UNITS
    val W = SCREEN_WIDTH_IN_UNITS.d

    val spacing = W * 0.2

    return Point(
        W / 2 - spacing + spacing * (star - 1),
        MENU_ZONE_BOTTOM_Y + H * (0.36 + if (star == 2) 0.01 else 0.0)
    )
}

// Fractions between 0 and 2 (0-1 is regular fill, 1-2 is blue fill)
val currentStarFillAmount = mutableListOf(0.0, 0.0, 0.0)

var starsTextTransparency = 0.0
    private set

private val buttonOffset: Double
    get() {
        val offset =
            if (GAME_STATE == GameState.InGame || GAME_STATE == GameState.PausedPopup) {
                saturate((GameTime.APP_TIME - APP_TIME_GAME_STARTED) * 4.0)
            } else {
                saturate((1.0 - (GameTime.APP_TIME - appTimeEnteredDiegeticMenu)) * 4.0)
            }

        return offset * SCREEN_WIDTH_IN_UNITS
    }

private val buttonDistFromMiddle: Double
    get() = SCREEN_WIDTH_IN_UNITS / 4.4 + buttonOffset

val showANextLevelButton: Boolean
    get() {
        if (UpgradeController.boughtEveryUpgrade()) {
            return false
        }

        return UpgradeController.getNextUpgradeToPurchase() != null ||
                currentlyPlayingUpgrade.value != UpgradeController.highestUpgradeUnlockedSoFar()
    }

val playAgainButtonRect: Rect
    get() {
        val x = if (showANextLevelButton) {
            SCREEN_WIDTH_IN_UNITS / 2.0 - buttonDistFromMiddle
        } else {
            SCREEN_WIDTH_IN_UNITS / 2.0 - buttonOffset
        }

        return Rect.centered(
            Point(x, MENU_ZONE_BOTTOM_Y + 2.0),
            Size(SCREEN_WIDTH_IN_UNITS / 2.5, 1.6)
        )
    }

val nextLevelButtonRect: Rect
    get() {
        val x = if (showANextLevelButton) {
            SCREEN_WIDTH_IN_UNITS / 2.0 + buttonDistFromMiddle
        } else {
            100.0
        }

        return Rect.centered(
            Point(x, MENU_ZONE_BOTTOM_Y + 2.0),
            Size(SCREEN_WIDTH_IN_UNITS / 2.5, 1.6)
        )
    }

var isPlayAgainButtonHeldDown = false
var isNextLevelButtonHeldDown = false


private var desiredCameraY: Double? = null
private var desiredCameraOnReached: (() -> Unit)? = null
private fun sendCameraTo(menuY: Double, onReached: () -> Unit) {
    desiredCameraY = menuY
    desiredCameraOnReached = onReached
}

private var wobbleRemaining = 0.0

private fun renderPlayAgainAndNextLevelButtons(r: Renderer) {
    renderButton(
        r,
        playAgainButtonRect,
        isPlayAgainButtonHeldDown,
        if (lastLevelPlayed == null) "Play" else "Play Again",
        buttonColor = colorScheme.playAgainColor
    )

    wobbleRemaining = maxOf(0.0, wobbleRemaining - GameTime.GRAPHICS_DELTA)

    val upgradeToMoveToNextLevel = UpgradeController.getNextLevel()
    val canDoNextLevel =
        upgradeToMoveToNextLevel != null && UpgradeController.getUpgradeStatus(
            upgradeToMoveToNextLevel
        ) != UpgradeController.UpgradeStatus.TOO_EXPENSIVE

    if (showANextLevelButton) {
        renderButton(
            r,
            nextLevelButtonRect,
            isNextLevelButtonHeldDown,
            if (canDoNextLevel) "Next Level" else "",
            buttonColor = if (canDoNextLevel) colorScheme.nextLevelColor else colorScheme.warningTextColor
        )

        if (upgradeToMoveToNextLevel != null && !canDoNextLevel) {
            r.color = colorScheme.textColor

            val wobble = get2DShake(wobbleRemaining, 0)

            val starSize = fontMedium.capHeight * 1.25 / UNIT_SIZE_IN_PIXELS
            r.text(
                fontMedium,
                "${ScoreSystem.getPlayerTotalNumStars()}/${upgradeToMoveToNextLevel.starsToUnlock}",
                nextLevelButtonRect.center() + Point(0.35, 0.0) + wobble,
                Align.right,
                shouldCache = false
            )

            renderStar(
                r,
                nextLevelButtonRect.center() + Point(1.0, 0.0) + wobble,
                starSize
            )
        }
    }

    TOUCHES_WENT_DOWN.forEach {
        if (PauseButton.isEatingInputs()) {
            return@forEach
        }
        isPlayAgainButtonHeldDown = playAgainButtonRect.contains(it.wo)
        if (canDoNextLevel) {
            isNextLevelButtonHeldDown = nextLevelButtonRect.contains(it.wo)
        } else {
            if (nextLevelButtonRect.contains(it.wo)) {
                wobbleRemaining = 0.5
                vibrate(10)
            }
        }

        if (isPlayAgainButtonHeldDown || isNextLevelButtonHeldDown) {
            vibrate(10)
        }
    }

    TOUCHES_DOWN.forEach {
        if (isPlayAgainButtonHeldDown && !playAgainButtonRect.contains(it.wo)) {
            isPlayAgainButtonHeldDown = false
        }
        if (isNextLevelButtonHeldDown && !nextLevelButtonRect.contains(it.wo)) {
            isNextLevelButtonHeldDown = false
        }
    }

    TOUCHES_WENT_UP.forEach {
        if (isPlayAgainButtonHeldDown && playAgainButtonRect.contains(it.wo)) {
            vibrate(10)
            startGame(false)
        }
        if (isNextLevelButtonHeldDown && nextLevelButtonRect.contains(it.wo)) {
            sendCameraTo(getUpgradeYPos(upgradeToMoveToNextLevel) - SCREEN_HEIGHT_IN_UNITS / 2) {
                UpgradeController.goToNextUpgrade()
            }
            vibrate(10)
        }
        isPlayAgainButtonHeldDown = false
        isNextLevelButtonHeldDown = false
    }
}

// The diegetic menu is always there and rendered using in-world coordinates.
fun renderDiegeticMenuWorldSpace(r: Renderer) {
    val H = SCREEN_HEIGHT_IN_UNITS
    val W = SCREEN_WIDTH_IN_UNITS.d

    UpgradeController.renderUpgradeMenuWorldSpace(r)

    renderPlayAgainAndNextLevelButtons(r)

    r.color = colorScheme.textColor

    r.text(
        fontLarge,
        "Insplode",
        W / 2, MENU_ZONE_BOTTOM_Y + H * 0.8,
        Align.center
    )

    // Horizontal separator line

    val separatorLineY = H * 0.74
    r.color = Color.BLACK.withAlpha(0.5)
    r.line(
        Point(2.5, MENU_ZONE_BOTTOM_Y + separatorLineY),
        Point(W - 2.5, MENU_ZONE_BOTTOM_Y + separatorLineY),
        0.05
    )

    // Level information

    val sidePadding = 1.5

    r.color = colorScheme.textColor
    r.text(
        fontSmall,
        currentlyPlayingUpgrade.value.levelText,
        sidePadding,
        MENU_ZONE_BOTTOM_Y + H * 0.685,
        Align.left
    )

    r.text(
        fontMedium,
        currentlyPlayingUpgrade.value.title,
        sidePadding,
        MENU_ZONE_BOTTOM_Y + H * 0.645,
        Align.left
    )

    r.text(
        fontSmall,
        currentlyPlayingUpgrade.value.bestTextEmptyIfZero,
        sidePadding,
        MENU_ZONE_BOTTOM_Y + H * 0.605,
        Align.left,
        shouldCache = false
    )

    // Icon is right-aligned

    val iconSize = H * 0.1
    val iconP = Point(W - sidePadding - iconSize / 2, MENU_ZONE_BOTTOM_Y + H * 0.65)
    r.centeredSprite(
        UpgradeController.getSpriteForUpgrade(currentlyPlayingUpgrade.value),
        iconP + SHADOW_DIRECTION_UNITS,
        iconSize,
        color = Color.BLACK.withAlpha(0.5)
    )

    r.centeredSprite(
        UpgradeController.getSpriteForUpgrade(currentlyPlayingUpgrade.value),
        iconP,
        iconSize
    )

    starsTextTransparency = if (ScoreSystem.isScoreGivingAnimationPlaying()) {
        1.0
    } else {
        frameAccurateLerp(starsTextTransparency, 0.0, 5.0)
    }

    // Horizontal separator line

    val lastUpgrade = ScoreSystem.lastLevelPlayed
    val lastScore = ScoreSystem.lastScore

    val gameStateForStars =
        GAME_STATE == GameState.DiegeticMenu || GAME_STATE == GameState.TransitioningToDiegeticMenu

    if (lastUpgrade != null && lastScore != null && gameStateForStars) {
        val separatorLineY2 = H * 0.55
        r.color = Color.BLACK.withAlpha(0.5)
        r.line(
            Point(2.5, MENU_ZONE_BOTTOM_Y + separatorLineY2),
            Point(W - 2.5, MENU_ZONE_BOTTOM_Y + separatorLineY2),
            0.05
        )

        r.color = colorScheme.textColor.withAlpha(1.0 - starsTextTransparency)
        r.text(
            fontMedium,
            "Score: ${ScoreSystem.lastScore}",
            W / 2,
            ScoreSystem.endOfGameCoinsHeight,
            Align.center,
            shouldCache = false
        )

        if (starsTextTransparency < 0.001 && !MusicCoordinator.isPlayingMusic()) {
            // End of the game.
            MusicCoordinator.startPlayingMusic()
        }

        r.color = colorScheme.textColor.withAlpha(0.4)
        r.text(
            fontTiny,
            currentHintText,
            W / 2,
            MENU_ZONE_BOTTOM_Y + H * 0.22,
            Align.center,
        )

        for (i in listOf(1, 3, 2)) {
            val fillAmount = currentStarFillAmount[i - 1]

            val starSize = (if (fillAmount < 1.0) {
                1.5
            } else if (fillAmount < 2.0) {
                2.0
            } else {
                2.3
            }) * (if (i == 2) 1.0 else 0.8)

            val starPos = getMainMenuStarPosition(i)

            renderColoredStar(
                r,
                starPos,
                starSize,
                backgroundColor = Color.BLACK.withAlpha(0.65),
                phase = fillAmount
            )

            val text = if (fillAmount < 2.0) {
                "${lastUpgrade.starsToScore(i)}"
            } else {
                "${lastUpgrade.starsToScore(i + 3)}"
            }


            r.color = colorScheme.textColor.withAlpha(
                if (fillAmount < 1.0) {
                    0.5
                } else {
                    1.0
                }
            )
            r.text(
                fontMedium,
                text,
                starPos.x,
                starPos.y - H * 0.075,
                Align.center,
                shouldCache = false
            )
        }
    }
}

fun isInLaunchButton(touch: Point): Boolean {
    val thisUpgradeYPos = getUpgradeYPos(currentlyPlayingUpgrade.value)
    return LAUNCH_AREA_RELATIVE_RECT.offsetBy(Point(0.0, thisUpgradeYPos))
        .contains(touch.wo)
}

/**
 * In ui/screen-space
 */
fun isInAnyButtons(touch: Point): Boolean {
    val isInPlayAgainButton = playAgainButtonRect.contains(touch.wo)
    val isInNextLevelButton = nextLevelButtonRect.contains(touch.wo)

    return isInLaunchButton(touch) || isInPlayAgainButton || isInNextLevelButton
}

private val coinAreaWidth
    get() = WIDTH / 4.5
private val coinAreaHeight
    get() = HEIGHT / 25
private val coinAreaTriangleWidth
    get() = WIDTH / 20

private fun starsAreaPoints(): List<Point> {
    return listOf(
        Point(-coinAreaWidth / 2 - coinAreaTriangleWidth, 0),
        Point(-coinAreaWidth / 2, -coinAreaHeight),
        Point(coinAreaWidth / 2, -coinAreaHeight),
        Point(coinAreaWidth / 2 + coinAreaTriangleWidth, 0)
    )
}

val starsAreaPosition
    get() =
        Point(WIDTH / 6, HEIGHT + (1 - starsTextAlpha) * coinAreaHeight * 1.4)

fun renderDiegeticMenuScreenSpace(r: Renderer) {
    starsTextAlpha =
        if (GAME_STATE == GameState.DiegeticMenu || GAME_STATE == GameState.TransitioningToDiegeticMenu) {
            frameAccurateLerp(starsTextAlpha, 1.0f, 10.0)
        } else {
            frameAccurateLerp(starsTextAlpha, 0.0f, 10.0)
        }

    val chevronDistanceBelow = SCREEN_HEIGHT_IN_UNITS / 10 - sin(GameTime.APP_TIME) * 0.15
    r.color = colorScheme.textColor
    drawChevron(r, MENU_ZONE_TOP_Y - chevronDistanceBelow)

    val starsAreaPoints = starsAreaPoints()

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
                starsAreaPosition.x + coinAreaWidth * 0.375 + coinTextWobble.x,
                textPos.y
            ), fontMedium.capHeight * 1.25
        )
    }
}

var cameraYUnitsDeltaThisTick = 0.0
    private set

fun tickDiegeticMenu() {
    if (GAME_STATE == GameState.DiegeticMenu) {
        var newCameraY = diegeticCameraY

        TOUCHES_WENT_DOWN.forEach {
            cameraVelocity = 0.0
            if (!isInAnyButtons(it.p)) {
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
                        1.25 * (fingerDist / UNIT_SIZE_IN_PIXELS) * 60.0
                }

                draggingIndex = null
            }
        }

        // A fun little hack as I was happy with the dragging and only then I realised
        // it wasn't framerate-independent.
        if (lastTimeUpdatedDelayedFingerY < GameTime.APP_TIME - 1.0 / 60.0) {
            lastTimeUpdatedDelayedFingerY = GameTime.APP_TIME
            delayedPreviousFingerY = lerp(delayedPreviousFingerY, lastFingerY, 0.5)
        }

        newCameraY += cameraVelocity * GameTime.GAMEPLAY_DELTA
        cameraVelocity = frameAccurateLerp(cameraVelocity, 0.0, 10.0)
        val lowestCameraY = MENU_ZONE_BOTTOM_Y
        val highestCameraY =
            LASER_HEIGHT_IN_MENU + 1.0 - SCREEN_HEIGHT_IN_UNITS

        // Soft clamp the camera y and make it bounce

        if (newCameraY < lowestCameraY) {
            newCameraY = frameAccurateLerp(newCameraY, lowestCameraY, 10.0)
            cameraVelocity = 0.0
        } else if (newCameraY > highestCameraY) {
            newCameraY = frameAccurateLerp(newCameraY, highestCameraY, 10.0)
            cameraVelocity = 0.0
        }

        val desiredY = desiredCameraY
        if (desiredY != null) {
            // Completely overwrite newCameraY and lerp our own thing
            newCameraY = frameAccurateLerp(diegeticCameraY, desiredY, 10.0)
            // if we're close-sh, call the onReached function
            if ((newCameraY - desiredY).abs < 2.0) {
                desiredCameraOnReached?.invoke()
                desiredCameraOnReached = null
            }


            // If we're really close, cancel the move.
            if ((newCameraY - desiredY).abs < 0.01) {
                desiredCameraY = null
            }
        }
        cameraYUnitsDeltaThisTick = newCameraY - diegeticCameraY
        diegeticCameraY = newCameraY
        setCameraY(diegeticCameraY)
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