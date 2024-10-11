package com.oberdiah.upgrades

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import com.oberdiah.CAMERA_POS_Y
import com.oberdiah.CURRENCY_DENOMINATION
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.Point
import com.oberdiah.Rect
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.Size
import com.oberdiah.abs
import com.oberdiah.createRandomFacingPoint
import com.oberdiah.d
import com.oberdiah.easeInOutSine
import com.oberdiah.f
import com.oberdiah.fontSmallish
import com.oberdiah.fontTiny
import com.oberdiah.get2DShake
import com.oberdiah.getOrZero
import com.oberdiah.max
import com.oberdiah.playMultiplierSound
import com.oberdiah.saturate
import com.oberdiah.spawnSmoke
import com.oberdiah.statefulCoinBalance
import com.oberdiah.ui.MENU_ZONE_BOTTOM_Y
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.ui.cameraVelocity
import com.oberdiah.ui.cameraYUnitsDeltaThisTick
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha

object UpgradeController {
    private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()
    private val allUpgradeTextures = mutableMapOf<Upgrade, Sprite>()

    const val UPGRADE_ENTRY_HEIGHT = 4.0

    fun init() {
        playerUpgradeStates.clear()
        Upgrade.entries.forEach {
            playerUpgradeStates[it] = StatefulBoolean(it.name, false)
            val path = "Icons/${it.name}.png"
            // Check if file exists
            if (UpgradeController::class.java.getResource("/$path") != null) {
                allUpgradeTextures[it] = Sprite(Texture(path))
            } else {
                println("Upgrade icon not found: $path")
                allUpgradeTextures[it] = Sprite(Texture("Icons/Not Found.png"))
            }
        }
        // Eventually we can do something like filling in any gaps that have been
        // added by updates here. For now we'll keep it simple.
    }

    fun resetAllUpgrades() {
        playerUpgradeStates.values.forEach { it.value = false }
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
                (GameTime.APP_TIME - (timeOfAttemptedPurchase.getOrZero(UpgradeStatus.PURCHASABLE))) / timeToPurchase
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
            (GameTime.APP_TIME - timeOfAttemptedPurchase.getOrZero(UpgradeStatus.TOO_EXPENSIVE)) / 0.5
        )
    }

    private fun getUpgradeSectionColor(upgrade: Upgrade): Color {
        return if (upgrade.ordinal % 2 == 0) Color.CYAN else Color.YELLOW
    }

    fun renderUpgradeMenuWorldSpace(r: Renderer) {
        if (JUST_UP_OFF_SCREEN_UNITS < UPGRADES_SCREEN_BOTTOM_Y) {
            return
        }

        val textXPos = 4.5
        val costTextXPos = 9.5
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

            val noFundsWarningFract = if (lastAttemptedPurchase == upgrade) {
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
            if (upgradeStatus.isPriceVisible()) {
                if (upgradeStatus.isPriceRed()) {
                    r.color.r = saturate(r.color.r + noFundsWarningFract + 0.5).f
                }

                r.text(
                    fontSmallish,
                    "${upgrade.price}${CURRENCY_DENOMINATION}",
                    Point(costTextXPos, yPos + UPGRADE_ENTRY_HEIGHT * 0.7) + priceShake,
                    Align.right
                )
            }

            // Small line
            r.color = Color.BLACK.withAlpha(0.5)
            r.rect(
                Point(textXPos + 0.05, yPos + UPGRADE_ENTRY_HEIGHT * 0.5) + lineShake,
                Size(4.0, 0.05),
            )

            val sprite = allUpgradeTextures[upgrade] ?: break
            val p = Point(iconXPos, yPos + UPGRADE_ENTRY_HEIGHT / 2) + iconShake
            // Draw the texture centered on the position, scaled as much as it can be without warping.
            val textureSize = Size(sprite.width.f, sprite.height.f)

            val iconScale = upgradeStatus.getIconSize() - purchaseFract * 0.5

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
                r.centeredSprite(sprite, p, Size(textureSize.w * scale, textureSize.h * scale))
            }
        }

        // Separating line
        r.color = Color.GOLD.withAlpha(0.8)
        r.rect(
            Point(0.0, yPosOfTopOfHighestUpgrade()),
            Size(SCREEN_WIDTH_IN_UNITS, 0.1),
        )
    }

    private var currentlyPurchasingUpgrade: Upgrade? = null
    private var lastAttemptedPurchase: Upgrade? = null
    private var currentUpgradePurchaseSoundsPlayed = -1
    private val timeOfAttemptedPurchase = mutableMapOf(
        UpgradeStatus.HIDDEN to Double.NEGATIVE_INFINITY,
        UpgradeStatus.TOO_EXPENSIVE to Double.NEGATIVE_INFINITY,
        UpgradeStatus.PURCHASABLE to Double.NEGATIVE_INFINITY,
        UpgradeStatus.PURCHASED to Double.NEGATIVE_INFINITY,
    )

    private fun cancelUpgradePurchase() {
        currentlyPurchasingUpgrade = null
        currentUpgradePurchaseSoundsPlayed = -1
    }

    fun tick() {
        if (cameraVelocity.abs < 0.1 && cameraYUnitsDeltaThisTick.abs < 0.05) {
            val purchasingFract = purchasingFraction()

            val soundToPlay = purchasingFract * 8
            if (soundToPlay.toInt() > currentUpgradePurchaseSoundsPlayed && purchasingFract > 0.1) {
                playMultiplierSound(soundToPlay.toInt())
                currentUpgradePurchaseSoundsPlayed++
            }

            TOUCHES_WENT_DOWN.forEach { touch ->
                upgradesIterator().forEach { (upgrade, yPos) ->
                    if ((yPos + UPGRADE_ENTRY_HEIGHT / 2 - touch.wo.y).abs < UPGRADE_ENTRY_HEIGHT / 2) {
                        val upgradeStatus = getUpgradeStatus(upgrade)
                        timeOfAttemptedPurchase[upgradeStatus] = GameTime.APP_TIME
                        lastAttemptedPurchase = upgrade
                        if (upgradeStatus == UpgradeStatus.PURCHASABLE) {
                            currentlyPurchasingUpgrade = upgrade
                        }
                    }
                }
            }

            if (purchasingFract >= 1.0) {
                val upgradePurchased = currentlyPurchasingUpgrade!!

                // Spawn a pile of particles
                val upgradeYPos = getUpgradeYPos(upgradePurchased)
                val color =
                    getUpgradeSectionColor(upgradePurchased).cpy().add(0.9f, 0.9f, 0.9f, -0.1f)
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

                playerUpgradeStates[upgradePurchased]?.value = true
                statefulCoinBalance.value -= upgradePurchased.price
                cancelUpgradePurchase()
            }

            TOUCHES_WENT_UP.forEach { _ ->
                cancelUpgradePurchase()
            }
        } else {
            cancelUpgradePurchase()
        }
    }

    fun playerHas(upgrade: Upgrade): Boolean {
        return playerUpgradeStates[upgrade]?.value ?: false
    }

    fun getMovementSpeed(): Double {
        return if (playerHas(Upgrade.EvenFasterMovement)) {
            10.0
        } else if (playerHas(Upgrade.FasterMovement)) {
            5.0
        } else if (playerHas(Upgrade.Movement)) {
            2.5
        } else {
            0.0
        }
    }

    fun getLaserSpeed(): Double {
        return if (playerHas(Upgrade.StoppedVoid)) {
            0.0
        } else if (playerHas(Upgrade.MuchSlowerVoid)) {
            0.3
        } else if (playerHas(Upgrade.SlowerVoid)) {
            1.0
        } else {
            2.0
        }
    }

    fun getLaserStartHeight(): Double {
        return if (playerHas(Upgrade.VoidLift)) {
            20.0
        } else {
            10.0
        }
    }

    private fun highestIndexUnlockedSoFar(): Int {
        return Upgrade.entries.indexOfLast { playerHas(it) }
    }

    fun yPosOfTopOfHighestUpgrade(): Double {
        return getUpgradeYPos(highestUpgradeUnlockedSoFar()) + UPGRADE_ENTRY_HEIGHT
    }

    private fun highestUpgradeUnlockedSoFar(): Upgrade? {
        return Upgrade.entries.lastOrNull { playerHas(it) }
    }

    private fun getUpgradeStatus(upgrade: Upgrade): UpgradeStatus {
        return if (playerHas(upgrade)) {
            UpgradeStatus.PURCHASED
        } else if (upgrade.ordinal <= highestIndexUnlockedSoFar() + 1) {
            if (statefulCoinBalance.value >= upgrade.price) {
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
