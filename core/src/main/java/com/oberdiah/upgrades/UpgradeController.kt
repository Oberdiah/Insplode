package com.oberdiah.upgrades

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import com.oberdiah.CAMERA_POS_Y
import com.oberdiah.HEIGHT
import com.oberdiah.IS_DEBUG_ENABLED
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.Point
import com.oberdiah.Rect
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.SHADOW_DIRECTION_UNITS
import com.oberdiah.ScoreSystem
import com.oberdiah.Size
import com.oberdiah.Sprites
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.WIDTH
import com.oberdiah.abs
import com.oberdiah.createRandomFacingPoint
import com.oberdiah.currentlyPlayingUpgrade
import com.oberdiah.d
import com.oberdiah.easeInOutSine
import com.oberdiah.f
import com.oberdiah.fontMedium
import com.oberdiah.fontSmall
import com.oberdiah.fontSmallish
import com.oberdiah.fontTiny
import com.oberdiah.get2DShake
import com.oberdiah.getOrZero
import com.oberdiah.lerp
import com.oberdiah.playMultiplierSound
import com.oberdiah.player.player
import com.oberdiah.saturate
import com.oberdiah.spawnSmoke
import com.oberdiah.startGame
import com.oberdiah.ui.PauseButton
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.ui.cameraVelocity
import com.oberdiah.ui.cameraYUnitsDeltaThisTick
import com.oberdiah.ui.isInLaunchButton
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.renderAwardedStars
import com.oberdiah.utils.renderStar
import com.oberdiah.utils.vibrate
import com.oberdiah.withAlpha

object UpgradeController {
    private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()

    const val UPGRADE_ENTRY_HEIGHT = 7.0

    fun init() {
        playerUpgradeStates.clear()
        Upgrade.entries.forEach {
            playerUpgradeStates[it] = StatefulBoolean("${it.name} Unlocked", false)
        }

        playerUpgradeStates[Upgrade.Movement]?.value = true
    }

    fun resetUpgradeStates() {
        playerUpgradeStates.forEach { (_, state) ->
            state.value = false
        }
        playerUpgradeStates[Upgrade.Movement]?.value = true
    }

    val TOP_OF_UPGRADE_SCREEN_UNITS
        get() = UPGRADES_SCREEN_BOTTOM_Y + UPGRADE_ENTRY_HEIGHT * Upgrade.entries.size

    /**
     * An iterator of upgrade and its Y-position in the upgrade menu.
     */
    fun upgradesIterator(): Iterator<Pair<Upgrade, Double>> {
        return Upgrade.entries.mapIndexed { index, upgrade ->
            upgrade to UPGRADES_SCREEN_BOTTOM_Y + UPGRADE_ENTRY_HEIGHT * index
        }.iterator()
    }

    fun getUpgradeYPos(upgrade: Upgrade?): Double {
        return (upgrade?.ordinal ?: 0) * UPGRADE_ENTRY_HEIGHT + UPGRADES_SCREEN_BOTTOM_Y
    }

    /**
     * A value from 0 to 1 representing how close we are to buying this upgrade.
     *
     * For animation.
     */
    fun purchasingFraction(): Double {
        if (currentlyPurchasingUpgrade == null) {
            return 0.0
        }

        val timeToPurchase = 1.5

        return easeInOutSine(
            saturate(
                (GameTime.APP_TIME - (timeOfLastUpgradeTap.getOrZero(UpgradeStatus.PURCHASABLE))) / timeToPurchase
            )
        )
    }

    fun currentUpgradeYRange(): ClosedFloatingPointRange<Double> {
        val upgradeYPos = getUpgradeYPos(currentlyPurchasingUpgrade)
        return upgradeYPos..(upgradeYPos + UPGRADE_ENTRY_HEIGHT)
    }

    fun getRedBackgroundRange(): ClosedFloatingPointRange<Double> {
        return UPGRADES_SCREEN_BOTTOM_Y.d..yPosOfTopOfHighestUpgrade()
    }

    fun getGreyscaleRange(): ClosedFloatingPointRange<Double> {
        return yPosOfTopOfHighestUpgrade()..TOP_OF_UPGRADE_SCREEN_UNITS
    }

    fun noFundsWarningFract(): Double {
        return 1.0 - saturate(
            (GameTime.APP_TIME - timeOfLastUpgradeTap[UpgradeStatus.TOO_EXPENSIVE]!!) / 0.5
        )
    }

    private fun selectedUpgradeFract(upgrade: Upgrade): Double {
        return easeInOutSine(
            if (currentlyPlayingUpgrade.value == upgrade && lastPlayingUpgrade == upgrade) {
                1.0
            } else if (currentlyPlayingUpgrade.value == upgrade) {
                saturate((GameTime.APP_TIME - timeSwitchedPlayingUpgrade) * 5.0)
            } else if (upgrade == lastPlayingUpgrade) {
                1.0 - saturate((GameTime.APP_TIME - timeSwitchedPlayingUpgrade) * 5.0)
            } else {
                0.0
            }
        )
    }

    private fun getUpgradeSectionColor(upgrade: Upgrade): Color {
        return if (upgrade.ordinal % 2 == 0) Color.CYAN else Color.YELLOW
    }

    fun getSpriteForUpgrade(upgrade: Upgrade): Sprite {
        if (upgrade == Upgrade.FinalRun) {
            if (ScoreSystem.playerHasFinishedTheGame()) {
                return Sprites.getSprite("Victory")
            }
        }

        return Sprites.getSprite(upgrade.name)
    }

    fun renderUpgradeMenuWorldSpace(r: Renderer) {
        if (JUST_UP_OFF_SCREEN_UNITS < UPGRADES_SCREEN_BOTTOM_Y) {
            return
        }

        val eh = UPGRADE_ENTRY_HEIGHT

        val topLeftOffset = Point(0.5, -0.5)
        val levelPos = Point(.0, eh * 6.25 / 7.0) + topLeftOffset
        val titlePos = Point(.0, eh * 5.5 / 7.0) + topLeftOffset
        val separatorLinePos = Point(.0, eh * 5.0 / 7.0) + topLeftOffset
        val descriptionPos = Point(.0, eh * 4.5 / 7.0) + topLeftOffset

        val deselectedTextOffset = Point(0.5, -1.0)

        val iconSelectedPos = Point(7.5, 5.0)
        val deselectedIconOffset = Point(0.0, -1.5)

        val selectedIconSize = 3.0
        val deselectedIconSize = 4.0

        for ((upgrade, yPos) in upgradesIterator()) {
            if (yPos > JUST_UP_OFF_SCREEN_UNITS) {
                break
            }
            if (yPos < CAMERA_POS_Y - UPGRADE_ENTRY_HEIGHT) {
                continue
            }

            val bottomLeft = Point(0.0, yPos)

            val upgradeStatus = getUpgradeStatus(upgrade)

            var alphaForColor = if (currentlyPurchasingUpgrade == upgrade) 0.2 else 0.11

            val purchaseFract = if (currentlyPurchasingUpgrade == upgrade) {
                purchasingFraction()
            } else {
                0.0
            }

            val noFundsWarningFract =
                if (lastUpgradeTapped[UpgradeStatus.TOO_EXPENSIVE] == upgrade) {
                    noFundsWarningFract()
                } else {
                    0.0
                }

            if (upgradeStatus != UpgradeStatus.PURCHASED) {
                alphaForColor = if (purchaseFract == 0.0) {
                    0.025
                } else {
                    0.05
                }
            }

            val transCutoff = 0.85
            val lateT = saturate((purchaseFract - transCutoff) / (1.0 - transCutoff))

            val backgroundColor = getUpgradeSectionColor(upgrade).withAlpha(alphaForColor)
            val selectedUpgradeFract = selectedUpgradeFract(upgrade)

            r.color = backgroundColor

            r.rect(
                bottomLeft,
                Size(SCREEN_WIDTH_IN_UNITS, UPGRADE_ENTRY_HEIGHT),
            )

            r.color = backgroundColor.add(-0.3f, -0.3f, -0.3f, 0.0f)

            r.rect(
                bottomLeft,
                Size(SCREEN_WIDTH_IN_UNITS * purchaseFract, UPGRADE_ENTRY_HEIGHT),
            )

            r.color = backgroundColor.add(1f, 1f, 1f, lateT.f * 0.5f)

            r.rect(
                bottomLeft,
                Size(SCREEN_WIDTH_IN_UNITS * lateT, UPGRADE_ENTRY_HEIGHT),
            )

            // Separating line
            r.color = Color.BLACK.withAlpha(0.2)
            r.rect(
                bottomLeft,
                Size(SCREEN_WIDTH_IN_UNITS, 0.05),
            )

            r.color = colorScheme.textColor.withAlpha(upgradeStatus.getTextAlpha())

            val levelShake = get2DShake(purchaseFract, 0)
            val titleShake = get2DShake(purchaseFract, 1)
            val descriptionShake = get2DShake(purchaseFract, 2)
            val priceShake = get2DShake(purchaseFract + noFundsWarningFract, 3)
            val lineShake = get2DShake(purchaseFract, 4)
            val iconShake = get2DShake(purchaseFract, 5)

            val textOffset = deselectedTextOffset * (1.0 - selectedUpgradeFract)
            val iconOffset = deselectedIconOffset * (1.0 - selectedUpgradeFract)

            r.text(
                fontSmall,
                upgrade.levelText,
                bottomLeft + levelPos + textOffset + levelShake,
                Align.left
            )

            r.text(
                fontSmallish,
                if (upgradeStatus.isTextVisible()) {
                    upgrade.title
                } else {
                    upgrade.obfuscatedTitle
                },
                bottomLeft + titlePos + textOffset + titleShake,
                Align.left
            )

            // Small line
            r.color = Color.BLACK.withAlpha(0.5)
            r.rect(
                bottomLeft + separatorLinePos + textOffset + lineShake,
                Size(4.0, 0.05),
            )

            r.text(
                fontTiny,
                if (upgradeStatus.isTextVisible()) {
                    upgrade.description
                } else {
                    upgrade.obfuscatedDescription
                },
                bottomLeft + descriptionPos + Point(
                    -8.0 * (1 - selectedUpgradeFract),
                    0.0
                ) + descriptionShake,
                Align.left
            )

            // Price
            val starSize = fontSmallish.capHeight * 1.25 / UNIT_SIZE_IN_PIXELS

            if (upgradeStatus.isPriceVisible()) {
                if (upgradeStatus.isPriceRed()) {
                    r.color.r = saturate(r.color.r + noFundsWarningFract + 0.5).f
                }

                r.text(
                    fontSmallish,
                    "${upgrade.starsToUnlock}",
                    Point(10.0 - starSize - 0.35, yPos + UPGRADE_ENTRY_HEIGHT * 0.7) + priceShake,
                    Align.right
                )

                renderStar(
                    r,
                    Point(
                        10.0 - starSize,
                        yPos + UPGRADE_ENTRY_HEIGHT * 0.7
                    ) + priceShake,
                    starSize
                )
            } else if (upgradeStatus.arePlayerRatingStarsVisible()) {
                val size = starSize * lerp(1.5, 2.0, selectedUpgradeFract)
                val xShift = -0.2
                val yShift = 0.1
                renderAwardedStars(
                    r,
                    Point(
                        lerp(3.0, 1.25 + xShift, selectedUpgradeFract),
                        yPos + lerp(2.75, 1.75 + yShift, selectedUpgradeFract)
                    ) + priceShake,
                    Align.left,
                    size * 0.9,
                    ScoreSystem.getNumStarsOnUpgrade(upgrade),
                    spacing = size * lerp(1.05, 1.2, selectedUpgradeFract)
                )

                r.color = Color.BLACK.withAlpha(0.4)
                val lineX =
                    2.4 + xShift + 2 * 1.2 + 8.0 * (1 - saturate(selectedUpgradeFract * 2 - 1.0))
                r.line(
                    Point(lineX, 1.0 - 0.2) + bottomLeft + priceShake,
                    Point(lineX, 2.0 + 0.2) + bottomLeft + priceShake,
                    0.065
                )

                for (i in 1..3) {
                    if (i > ScoreSystem.getNumStarsOnUpgrade(upgrade).stars) {
                        r.color = Color.BLACK.withAlpha(0.25)
                    } else {
                        r.color = colorScheme.textColor
                    }

                    r.text(
                        if (upgrade.starsToScore(3) < 1000) fontSmallish else fontSmall,
                        "${upgrade.starsToScore(i)}",
                        Point(
                            1.25 + xShift + (i - 1) * 1.2 + lerp(-6.0, 0.0, selectedUpgradeFract),
                            0.85 + yShift + yPos
                        ) + priceShake,
                        Align.center
                    )
                }
            }

            val sprite = getSpriteForUpgrade(upgrade)
            val p = bottomLeft + iconSelectedPos + iconOffset + iconShake
            // Draw the texture centered on the position, scaled as much as it can be without warping.

            val iconScale =
                (upgradeStatus.getIconScale() - purchaseFract * 0.5) * lerp(
                    deselectedIconSize,
                    selectedIconSize,
                    selectedUpgradeFract
                ) + 0.0001

            // Shadow
            r.centeredSprite(
                sprite,
                p + SHADOW_DIRECTION_UNITS,
                iconScale,
                color = Color.BLACK.withAlpha(0.5)
            )

            if (upgradeStatus.isIconVisible()) {
                // Main sprite
                r.centeredSprite(
                    sprite,
                    p,
                    iconScale,
                )
            }

            if (selectedUpgradeFract > 0.01) {
                val launchAreaRect = LAUNCH_AREA_RELATIVE_RECT.offsetBy(
                    bottomLeft + Point(
                        8.0 * (1 - selectedUpgradeFract),
                        0.0
                    )
                )

                r.color = colorScheme.textColor
                r.text(
                    fontSmall,
                    upgrade.bestText,
                    launchAreaRect.tl + Point(0.0, 0.25),
                )

                r.color = Color.BLACK.withAlpha(0.5)
                r.rect(launchAreaRect.offsetBy(SHADOW_DIRECTION_UNITS))

                val areaRect = if (holdingDownPlayButton) {
                    launchAreaRect.offsetBy(SHADOW_DIRECTION_UNITS)
                } else {
                    launchAreaRect
                }

                r.color = if (holdingDownPlayButton) {
                    Color.GRAY
                } else {
                    colorScheme.launchButtonColor
                }
                r.rect(areaRect)

                r.color = Color.BLACK
                r.hollowRect(areaRect, 0.1)

                val buttonText = if (upgrade == Upgrade.Slam) {
                    "Launch!"
                } else {
                    "Play!"
                }
                r.text(
                    fontMedium,
                    buttonText,
                    areaRect.center(),
                    Align.center
                )
            }
        }

        // Separating line
        r.color = Color.GOLD.withAlpha(0.8)
        r.rect(
            Point(0.0, getUpgradeYPos(currentlyPlayingUpgrade.value) + UPGRADE_ENTRY_HEIGHT),
            Size(SCREEN_WIDTH_IN_UNITS, 0.1),
        )
    }

    val LAUNCH_AREA_RELATIVE_RECT = Rect(
        Point(5.5, 0.75),
        Size(4.0, 1.5),
    )

    const val TOTAL_TAP_WARNING_TIME = 5.0

    fun renderUpgradeMenuScreenSpace(r: Renderer) {
        val timeSinceTapWarning = GameTime.APP_TIME - lastTapWarningTime
        val animationTime = 0.5

        val heightOffOfTop = HEIGHT / 15

        val heightToMove = heightOffOfTop * 2

        val normalHeight = HEIGHT + heightOffOfTop

        if (timeSinceTapWarning < TOTAL_TAP_WARNING_TIME) {
            val tapWarningY = if (timeSinceTapWarning < animationTime) {
                normalHeight - saturate(timeSinceTapWarning / animationTime) * heightToMove
            } else if (timeSinceTapWarning < TOTAL_TAP_WARNING_TIME - animationTime) {
                normalHeight - heightToMove
            } else {
                normalHeight - (1.0 - saturate((timeSinceTapWarning - (TOTAL_TAP_WARNING_TIME - animationTime)) / animationTime)) * heightToMove
            }

            r.color = Color.WHITE.withAlpha(0.75)
            r.centeredRect(
                Point(WIDTH / 2, tapWarningY),
                Size(WIDTH, HEIGHT / 20),
            )

            r.color = Color.BLACK
            r.text(
                fontSmallish,
                "Touch and hold to unlock",
                WIDTH / 2,
                tapWarningY,
                Align.center
            )
        }
    }

    private var currentlyPurchasingUpgrade: Upgrade? = null
    private var lastUpgradeTapped: MutableMap<UpgradeStatus, Upgrade?> = mutableMapOf(
        UpgradeStatus.HIDDEN to null,
        UpgradeStatus.TOO_EXPENSIVE to null,
        UpgradeStatus.PURCHASABLE to null,
        UpgradeStatus.PURCHASED to Upgrade.Movement,
    )
    private var currentUpgradePurchaseSoundsPlayed = -1
    private val timeOfLastUpgradeTap = mutableMapOf(
        UpgradeStatus.HIDDEN to Double.NEGATIVE_INFINITY,
        UpgradeStatus.TOO_EXPENSIVE to Double.NEGATIVE_INFINITY,
        UpgradeStatus.PURCHASABLE to Double.NEGATIVE_INFINITY,
        UpgradeStatus.PURCHASED to Double.NEGATIVE_INFINITY,
    )

    private var lastTapWarningTime = -Double.MAX_VALUE

    private fun cancelUpgradePurchase() {
        if (currentlyPurchasingUpgrade != null) {
            if (purchasingFraction() < 0.1 && lastTapWarningTime < GameTime.APP_TIME - TOTAL_TAP_WARNING_TIME) {
                lastTapWarningTime = GameTime.APP_TIME
            }
        }

        currentlyPurchasingUpgrade = null
        currentUpgradePurchaseSoundsPlayed = -1
    }

    var isConsideringSwitchingPlayingUpgrade = false
    var lastPlayingUpgrade: Upgrade? = null
    var timeSwitchedPlayingUpgrade = Double.NEGATIVE_INFINITY
    var holdingDownPlayButton = false

    fun tick() {
        if (cameraVelocity.abs < 0.1 && cameraYUnitsDeltaThisTick.abs < 0.05) {
            val purchasingFract = purchasingFraction()

            val soundToPlay = purchasingFract * 8
            if (soundToPlay.toInt() > currentUpgradePurchaseSoundsPlayed && purchasingFract > 0.1) {
                playMultiplierSound(soundToPlay.toInt())
                currentUpgradePurchaseSoundsPlayed++
            }

            TOUCHES_WENT_DOWN.forEach { touch ->
                if (PauseButton.isEatingInputs()) {
                    return@forEach
                }
                if (isInLaunchButton(touch)) {
                    vibrate(10)
                }

                upgradesIterator().forEach { (upgrade, yPos) ->
                    if ((yPos + UPGRADE_ENTRY_HEIGHT / 2 - touch.wo.y).abs < UPGRADE_ENTRY_HEIGHT / 2) {
                        val upgradeStatus = getUpgradeStatus(upgrade)
                        timeOfLastUpgradeTap[upgradeStatus] = GameTime.APP_TIME
                        lastUpgradeTapped[upgradeStatus] = upgrade
                        if (upgradeStatus == UpgradeStatus.PURCHASABLE) {
                            currentlyPurchasingUpgrade = upgrade
                            vibrate(10)
                        } else if (upgradeStatus == UpgradeStatus.PURCHASED) {
                            isConsideringSwitchingPlayingUpgrade = true
                        } else if (upgradeStatus == UpgradeStatus.TOO_EXPENSIVE) {
                            vibrate(10)
                        }
                    }
                }
            }

            TOUCHES_DOWN.forEach { touch ->
                if (PauseButton.isEatingInputs()) {
                    return@forEach
                }

                holdingDownPlayButton = isInLaunchButton(touch)
            }

            if (purchasingFract >= 1.0) {
                val upgradePurchased = currentlyPurchasingUpgrade!!
                completeUpgradePurchase(upgradePurchased)
                cancelUpgradePurchase()
            }

            TOUCHES_WENT_UP.forEach { touch ->
                if (PauseButton.isEatingInputs()) {
                    return@forEach
                }

                if (holdingDownPlayButton) {
                    holdingDownPlayButton = false
                    vibrate(10)
                    startGame()
                    return@forEach
                }

                cancelUpgradePurchase()
                if (isConsideringSwitchingPlayingUpgrade) {
                    val toSwitchTo = lastUpgradeTapped[UpgradeStatus.PURCHASED]!!
                    if (currentlyPlayingUpgrade.value != toSwitchTo) {
                        vibrate(10)
                    }
                    lastPlayingUpgrade = currentlyPlayingUpgrade.value
                    currentlyPlayingUpgrade.value = toSwitchTo
                    isConsideringSwitchingPlayingUpgrade = false
                    timeSwitchedPlayingUpgrade = GameTime.APP_TIME
                }
            }
        } else {
            cancelUpgradePurchase()
            isConsideringSwitchingPlayingUpgrade = false
        }
    }

    private fun completeUpgradePurchase(upgrade: Upgrade) {
        // Spawn a pile of particles
        val upgradeYPos = getUpgradeYPos(upgrade)
        val color =
            getUpgradeSectionColor(upgrade).cpy().add(0.9f, 0.9f, 0.9f, -0.1f)
        for (i in 0..1000) {
            spawnSmoke(
                Rect(
                    Point(0.0, upgradeYPos),
                    Size(SCREEN_WIDTH_IN_UNITS, UPGRADE_ENTRY_HEIGHT),
                ).randomPointInside(),
                createRandomFacingPoint(),
                color = color,
                canCollide = false
            )
        }

        currentlyPlayingUpgrade.value = upgrade
        playerUpgradeStates[upgrade]?.value = true
        vibrate(10)

        // Make sure the player moves up if they need to
        player.reset()
    }

    private val FORCE_UPGRADES_UNTIL: Upgrade? = null // Upgrade.FinalRun

    /**
     * Whether the player is playing with this upgrade this game.
     */
    fun playerHas(upgrade: Upgrade): Boolean {
        if (IS_DEBUG_ENABLED) {
            FORCE_UPGRADES_UNTIL?.let {
                return upgrade.ordinal <= it.ordinal
            }
        }

        return upgrade.ordinal <= currentlyPlayingUpgrade.value.ordinal
    }

    fun playerHasTheAbilityToPlayWith(upgrade: Upgrade): Boolean {
        if (IS_DEBUG_ENABLED) {
            FORCE_UPGRADES_UNTIL?.let {
                return upgrade.ordinal <= it.ordinal
            }
        }

        return playerUpgradeStates[upgrade]?.value == true
    }

    fun getMovementSpeed(): Double {
        return if (playerHas(Upgrade.EvenFasterMovement)) {
            10.0
        } else if (playerHas(Upgrade.Mobility)) {
            5.0
        } else if (playerHas(Upgrade.Movement)) {
            2.5
        } else {
            0.0
        }
    }

    fun getLaserSpeed(): Double {
        return if (playerHas(Upgrade.SlowerVoid)) {
            0.3
        } else {
            1.5
        }
    }

    fun getLaserStartHeight(): Double {
        return if (playerHas(Upgrade.SpringBomb)) {
            13.0
        } else if (playerHas(Upgrade.Mobility)) {
            12.0
        } else if (playerHas(Upgrade.LineBomb)) {
            11.0
        } else {
            10.0
        }
    }

    fun getBombFuseMultiplier(): Double {
        return if (playerHas(Upgrade.RapidBombs)) {
            0.75
        } else {
            0.5
        }
    }

    fun getPlayerMagnetRadius(): Double {
        return if (playerHas(Upgrade.GlobalMagnet)) {
            30.0
        } else if (playerHas(Upgrade.MegaMagnet)) {
            4.5
        } else if (playerHas(Upgrade.Magnet)) {
            1.5
        } else {
            0.0
        }
    }

    fun getLineBombWidth(): Double {
        return if (playerHas(Upgrade.WiderLineBombs)) {
            14.0
        } else {
            6.0
        }
    }

    private fun highestIndexUnlockedSoFar(): Int {
        return Upgrade.entries.indexOfLast { playerHasTheAbilityToPlayWith(it) }
    }

    fun yPosOfTopOfHighestUpgrade(): Double {
        return getUpgradeYPos(highestUpgradeUnlockedSoFar()) + UPGRADE_ENTRY_HEIGHT
    }

    private fun highestUpgradeUnlockedSoFar(): Upgrade? {
        return Upgrade.entries.lastOrNull { playerHasTheAbilityToPlayWith(it) }
    }

    private fun getUpgradeStatus(upgrade: Upgrade): UpgradeStatus {
        return if (playerHasTheAbilityToPlayWith(upgrade)) {
            UpgradeStatus.PURCHASED
        } else if (upgrade.ordinal <= highestIndexUnlockedSoFar() + 1) {
            if (ScoreSystem.getPlayerTotalNumStars() >= upgrade.starsToUnlock) {
                UpgradeStatus.PURCHASABLE
            } else {
                UpgradeStatus.TOO_EXPENSIVE
            }
        } else {
            UpgradeStatus.HIDDEN
        }
    }

    private enum class UpgradeStatus {
        HIDDEN,
        TOO_EXPENSIVE,
        PURCHASABLE,
        PURCHASED;

        fun isPriceVisible(): Boolean {
            return this == PURCHASABLE || this == TOO_EXPENSIVE
        }

        fun isPriceRed(): Boolean {
            return this == TOO_EXPENSIVE
        }

        fun isIconVisible(): Boolean {
            return this != HIDDEN
        }

        fun arePlayerRatingStarsVisible(): Boolean {
            return this == PURCHASED
        }

        fun isTextVisible(): Boolean {
            return this != HIDDEN
        }

        fun shouldFadeBackground(): Boolean {
            return this != PURCHASED
        }

        fun getIconScale(): Double {
            return when (this) {
                HIDDEN, TOO_EXPENSIVE, PURCHASABLE -> 2.0 / 3.0
                PURCHASED -> 1.0
            }
        }

        fun getTextAlpha(): Double {
            return when (this) {
                HIDDEN -> 0.3
                TOO_EXPENSIVE -> 0.75
                PURCHASABLE -> 0.75
                PURCHASED -> 1.0
            }
        }
    }
}
