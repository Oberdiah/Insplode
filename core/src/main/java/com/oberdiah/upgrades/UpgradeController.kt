package com.oberdiah.upgrades

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import com.oberdiah.CAMERA_POS_Y
import com.oberdiah.IS_DEBUG_ENABLED
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.Point
import com.oberdiah.Rect
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.ScoreSystem
import com.oberdiah.Size
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.abs
import com.oberdiah.createRandomFacingPoint
import com.oberdiah.currentlyPlayingUpgrade
import com.oberdiah.d
import com.oberdiah.easeInOutSine
import com.oberdiah.f
import com.oberdiah.fontSmallish
import com.oberdiah.fontTiny
import com.oberdiah.get2DShake
import com.oberdiah.getOrZero
import com.oberdiah.max
import com.oberdiah.playMultiplierSound
import com.oberdiah.player.player
import com.oberdiah.saturate
import com.oberdiah.spawnSmoke
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.ui.cameraVelocity
import com.oberdiah.ui.cameraYUnitsDeltaThisTick
import com.oberdiah.ui.isInLaunchButton
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.renderAwardedStars
import com.oberdiah.utils.renderStar
import com.oberdiah.utils.vibrate
import com.oberdiah.withAlpha

object UpgradeController {
    private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()
    private val allUpgradeTextures = mutableMapOf<Upgrade, Sprite>()
    lateinit var finalUpgradeSprite: Sprite

    const val UPGRADE_ENTRY_HEIGHT = 4.0

    fun init() {
        playerUpgradeStates.clear()
        Upgrade.entries.forEach {
            playerUpgradeStates[it] = StatefulBoolean("${it.name} Unlocked", false)
            val path = "Icons/${it.name}.png"
            // Check if file exists
            if (Gdx.files.internal(path).exists()) {
                allUpgradeTextures[it] = Sprite(Texture(path))
            } else {
                println("Upgrade icon not found: $path")
                allUpgradeTextures[it] = Sprite(Texture("Icons/Not Found.png"))
            }
        }

        finalUpgradeSprite = Sprite(Texture("Icons/Victory.png"))
        playerUpgradeStates[Upgrade.StarterUpgrade]?.value = true
    }

    fun resetUpgradeStates() {
        playerUpgradeStates.forEach { (_, state) ->
            state.value = false
        }
        playerUpgradeStates[Upgrade.StarterUpgrade]?.value = true
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

    private fun getSpriteForUpgrade(upgrade: Upgrade): Sprite {
        if (upgrade == Upgrade.FinalRun) {
            if (ScoreSystem.playerHasFinishedTheGame()) {
                return finalUpgradeSprite
            } else {
                return allUpgradeTextures[Upgrade.FinalRun]!!
            }
        }

        return allUpgradeTextures[upgrade]!!
    }

    fun renderUpgradeMenuWorldSpace(r: Renderer) {
        if (JUST_UP_OFF_SCREEN_UNITS < UPGRADES_SCREEN_BOTTOM_Y) {
            return
        }

        val textXPos = 4.5
        val iconXPos = 2.25

        for ((upgrade, yPos) in upgradesIterator()) {
            if (yPos > JUST_UP_OFF_SCREEN_UNITS) {
                break
            }
            if (yPos < CAMERA_POS_Y - UPGRADE_ENTRY_HEIGHT) {
                continue
            }

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
                Point(0.0, yPos),
                Size(SCREEN_WIDTH_IN_UNITS, UPGRADE_ENTRY_HEIGHT),
            )

            r.color = backgroundColor.add(-0.3f, -0.3f, -0.3f, 0.0f)

            r.rect(
                Point(0.0, yPos),
                Size(SCREEN_WIDTH_IN_UNITS * purchaseFract, UPGRADE_ENTRY_HEIGHT),
            )

            r.color = backgroundColor.add(1f, 1f, 1f, lateT.f * 0.5f)

            r.rect(
                Point(0.0, yPos),
                Size(SCREEN_WIDTH_IN_UNITS * lateT, UPGRADE_ENTRY_HEIGHT),
            )

            // Separating line
            r.color = Color.BLACK.withAlpha(0.2)
            r.rect(
                Point(0.0, yPos),
                Size(SCREEN_WIDTH_IN_UNITS, 0.05),
            )

            r.color = colorScheme.textColor.withAlpha(upgradeStatus.getTextAlpha())

            val titleShake = get2DShake(purchaseFract, 1)
            val descriptionShake = get2DShake(purchaseFract, 2)
            val priceShake = get2DShake(purchaseFract + noFundsWarningFract, 3)
            val lineShake = get2DShake(purchaseFract, 4)
            val iconShake = get2DShake(purchaseFract, 5)

            r.text(
                fontSmallish,
                if (upgradeStatus.isTextVisible()) {
                    upgrade.title
                } else {
                    upgrade.obfuscatedTitle
                },
                Point(textXPos, yPos + UPGRADE_ENTRY_HEIGHT * 0.7) + titleShake,
                Align.left
            )

            r.text(
                fontTiny,
                if (upgradeStatus.isTextVisible()) {
                    upgrade.description
                } else {
                    upgrade.obfuscatedDescription
                },
                Point(textXPos, yPos + UPGRADE_ENTRY_HEIGHT * 0.375) + descriptionShake,
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
                r.text(
                    fontSmallish,
                    "${ScoreSystem.getPlayerScore(upgrade)}",
                    Point(
                        10.0 - starSize + (1.0 - selectedUpgradeFract) * 3.0,
                        yPos + UPGRADE_ENTRY_HEIGHT * 0.7
                    ) + priceShake,
                    Align.right
                )

                renderAwardedStars(
                    r,
                    Point(
                        10.0 - starSize + selectedUpgradeFract * 3.0,
                        yPos + UPGRADE_ENTRY_HEIGHT * 0.7
                    ) + priceShake,
                    Align.right,
                    starSize,
                    ScoreSystem.getNumStarsOnUpgrade(upgrade)
                )
            }

            // Small line
            r.color = Color.BLACK.withAlpha(0.5)
            r.rect(
                Point(textXPos + 0.05, yPos + UPGRADE_ENTRY_HEIGHT * 0.5) + lineShake,
                Size(4.0, 0.05),
            )

            val sprite = getSpriteForUpgrade(upgrade)
            val p = Point(
                iconXPos - selectedUpgradeFract * 3.0,
                yPos + UPGRADE_ENTRY_HEIGHT / 2
            ) + iconShake
            // Draw the texture centered on the position, scaled as much as it can be without warping.
            val textureSize = Size(sprite.width.f, sprite.height.f)

            val iconScale =
                (upgradeStatus.getIconSize() - purchaseFract * 0.5) * (1.0 - selectedUpgradeFract) + 0.0001

            val scale = iconScale / max(textureSize.w, textureSize.h).f
            val shadowDirection = Point(0.1, -0.1)

            // Shadow
            r.centeredSprite(
                sprite,
                p + shadowDirection,
                Size(textureSize.w * scale, textureSize.h * scale),
                color = Color.BLACK.withAlpha(0.5)
            )

            if (upgradeStatus.isIconVisible()) {
                // Main sprite
                r.centeredSprite(
                    sprite,
                    p,
                    Size(textureSize.w * scale, textureSize.h * scale),
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

    fun renderUpgradeMenuWorldSpace2(r: Renderer) {
        val OVERLAY_WIDTH = 4.0

        for ((upgrade, yPos) in upgradesIterator()) {
            val selectedUpgradeFract = selectedUpgradeFract(upgrade)

            if (selectedUpgradeFract > 0.01) {
                val xPosOfInfoOverlay = -OVERLAY_WIDTH + selectedUpgradeFract * OVERLAY_WIDTH

                // Three lines, one of each of the three stars and their requirements.
                val starSize = fontSmallish.capHeight * 1.25 / UNIT_SIZE_IN_PIXELS

                val hasDevScore = ScoreSystem.getNumStarsOnUpgrade(upgrade).isDeveloperBest

                val startPos = if (hasDevScore) 0.8 else 0.7

                for (i in 0..if (hasDevScore) 3 else 2) {
                    val starYPos =
                        yPos + UPGRADE_ENTRY_HEIGHT * startPos - i * UPGRADE_ENTRY_HEIGHT * 0.2
                    val starXPos = 1.0 + xPosOfInfoOverlay

                    val haveThisUpgrade =
                        ScoreSystem.getNumStarsOnUpgrade(upgrade).stars >= i + 1 || hasDevScore

                    renderAwardedStars(
                        r,
                        Point(starXPos, starYPos),
                        Align.left,
                        starSize,
                        ScoreSystem.StarsAwarded.fromNumber(i + 1),
                        mainStarColor = if (haveThisUpgrade) colorScheme.starsColor else colorScheme.starsColor.cpy()
                            .mul(0.35f),
                    )

                    r.color =
                        if (haveThisUpgrade) colorScheme.textColor else colorScheme.textColor
                            .withAlpha(0.35f)
                    r.text(
                        fontSmallish,
                        "${upgrade.starsToScore(i + 1)}",
                        Point(starXPos + 1.65, starYPos),
                        Align.left
                    )
                }
            }
        }
    }

    private var currentlyPurchasingUpgrade: Upgrade? = null
    private var lastUpgradeTapped: MutableMap<UpgradeStatus, Upgrade?> = mutableMapOf(
        UpgradeStatus.HIDDEN to null,
        UpgradeStatus.TOO_EXPENSIVE to null,
        UpgradeStatus.PURCHASABLE to null,
        UpgradeStatus.PURCHASED to Upgrade.StarterUpgrade,
    )
    private var currentUpgradePurchaseSoundsPlayed = -1
    private val timeOfLastUpgradeTap = mutableMapOf(
        UpgradeStatus.HIDDEN to Double.NEGATIVE_INFINITY,
        UpgradeStatus.TOO_EXPENSIVE to Double.NEGATIVE_INFINITY,
        UpgradeStatus.PURCHASABLE to Double.NEGATIVE_INFINITY,
        UpgradeStatus.PURCHASED to Double.NEGATIVE_INFINITY,
    )

    private fun cancelUpgradePurchase() {
        currentlyPurchasingUpgrade = null
        currentUpgradePurchaseSoundsPlayed = -1
    }

    var isConsideringSwitchingPlayingUpgrade = false
    var lastPlayingUpgrade: Upgrade? = null
    var timeSwitchedPlayingUpgrade = Double.NEGATIVE_INFINITY

    fun tick() {
        if (cameraVelocity.abs < 0.1 && cameraYUnitsDeltaThisTick.abs < 0.05) {
            val purchasingFract = purchasingFraction()

            val soundToPlay = purchasingFract * 8
            if (soundToPlay.toInt() > currentUpgradePurchaseSoundsPlayed && purchasingFract > 0.1) {
                playMultiplierSound(soundToPlay.toInt())
                currentUpgradePurchaseSoundsPlayed++
            }

            TOUCHES_WENT_DOWN.forEach { touch ->
                if (isInLaunchButton(touch)) {
                    return@forEach
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

            if (purchasingFract >= 1.0) {
                val upgradePurchased = currentlyPurchasingUpgrade!!
                completeUpgradePurchase(upgradePurchased)
                cancelUpgradePurchase()
            }

            TOUCHES_WENT_UP.forEach { touch ->
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

        fun getIconSize(): Double {
            return when (this) {
                HIDDEN, TOO_EXPENSIVE, PURCHASABLE -> 2.0
                PURCHASED -> 3.0
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
