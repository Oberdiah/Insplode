package com.oberdiah.upgrades

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import com.oberdiah.CAMERA_POS_Y
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
import com.oberdiah.max
import com.oberdiah.playMultiplierSound
import com.oberdiah.saturate
import com.oberdiah.spawnSmoke
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.ui.cameraVelocity
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.addScreenShake
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha

object UpgradeController {
    private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()
    private val allUpgradeTextures = mutableMapOf<Upgrade, Sprite>()

    private const val UPGRADE_ENTRY_HEIGHT = 4.0

    fun init() {
        playerUpgradeStates.clear()
        Upgrade.values().forEach {
            playerUpgradeStates[it] = StatefulBoolean(it.name, false)
            allUpgradeTextures[it] = Sprite(Texture("Icons/${it.name}.png"))
        }
        // Eventually we can do something like filling in any gaps that have been
        // added by updates here. For now we'll keep it simple.
    }

    fun resetAllUpgrades() {
        playerUpgradeStates.values.forEach { it.value = false }
    }

    val TOP_OF_UPGRADE_SCREEN_UNITS
        get() = UPGRADES_SCREEN_BOTTOM_Y + UPGRADE_ENTRY_HEIGHT * Upgrade.values().size

    /**
     * An iterator of upgrade and its Y-position in the upgrade menu.
     */
    fun upgradesIterator(): Iterator<Pair<Upgrade, Double>> {
        return Upgrade.values().mapIndexed { index, upgrade ->
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

        return easeInOutSine(saturate((GameTime.APP_TIME - timePurchasingUpgradeStarted) / timeToPurchase))
    }

    fun currentUpgradeYRange(): ClosedFloatingPointRange<Double> {
        val upgradeYPos = getUpgradeYPos(currentlyPurchasingUpgrade)
        return upgradeYPos..(upgradeYPos + UPGRADE_ENTRY_HEIGHT)
    }

    fun getRedBackgroundRange(): ClosedFloatingPointRange<Double> {
        return UPGRADES_SCREEN_BOTTOM_Y.d..getUpgradeYPos(
            highestUpgradeUnlockedSoFar()
        ) + UPGRADE_ENTRY_HEIGHT
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
        val iconSize = 3.0
        val iconSizeBeforePurchase = 2.0

        for ((upgrade, yPos) in upgradesIterator()) {
            if (yPos > JUST_UP_OFF_SCREEN_UNITS) {
                break
            }
            if (yPos < CAMERA_POS_Y - UPGRADE_ENTRY_HEIGHT) {
                continue
            }

            val hasUpgrade = playerHas(upgrade)
            val isPurchasable = upgradeIsPurchasable(upgrade)
            val isAbleToSee = isPurchasable || hasUpgrade

            var alphaForColor = if (currentlyPurchasingUpgrade == upgrade) 0.2 else 0.11

            val purchaseFract = if (currentlyPurchasingUpgrade == upgrade) {
                purchasingFraction()
            } else {
                0.0
            }

            if (!hasUpgrade) {
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

            r.color = colorScheme.textColor

            if (!hasUpgrade) {
                if (isPurchasable) {
                    r.color = r.color.withAlpha(0.75)
                } else {
                    r.color = r.color.withAlpha(0.3)
                }
            }

            r.text(
                fontSmallish,
                if (isAbleToSee) {
                    upgrade.title
                } else {
                    upgrade.obfuscatedTitle
                },
                Point(textXPos, yPos + UPGRADE_ENTRY_HEIGHT * 0.7) + get2DShake(purchaseFract, 1),
                Align.left
            )

            // Price
            if (isPurchasable) {
                r.text(
                    fontSmallish,
                    "${upgrade.price}g",
                    Point(costTextXPos, yPos + UPGRADE_ENTRY_HEIGHT * 0.7) + get2DShake(
                        purchaseFract,
                        2
                    ),
                    Align.right
                )
            }

            r.text(
                fontTiny,
                if (isAbleToSee) {
                    upgrade.description
                } else {
                    upgrade.obfuscatedDescription
                },
                Point(textXPos, yPos + UPGRADE_ENTRY_HEIGHT * 0.375) + get2DShake(
                    purchaseFract,
                    3
                ),
                Align.left
            )

            // Small line
            r.color = Color.BLACK.withAlpha(0.5)
            r.rect(
                Point(textXPos + 0.05, yPos + UPGRADE_ENTRY_HEIGHT * 0.5) + get2DShake(
                    purchaseFract, 4
                ),
                Size(4.0, 0.05),
            )

            val sprite = allUpgradeTextures[upgrade] ?: break
            val p =
                Point(iconXPos, yPos + UPGRADE_ENTRY_HEIGHT / 2) + get2DShake(purchaseFract, 5)
            // Draw the texture centered on the position, scaled as much as it can be without warping.
            val textureSize = Size(sprite.width.f, sprite.height.f)

            val iconScale = if (hasUpgrade) {
                iconSize
            } else {
                iconSizeBeforePurchase - purchaseFract * 0.5
            }

            val scale = iconScale / max(textureSize.w, textureSize.h).f

            val shadowDirection = Point(0.1, -0.1)

            // Shadow
            r.centeredSprite(
                sprite,
                p + shadowDirection,
                Size(textureSize.w * scale, textureSize.h * scale),
                color = Color.BLACK.withAlpha(0.5)
            )

            if (isAbleToSee) {
                // Main sprite
                r.centeredSprite(sprite, p, Size(textureSize.w * scale, textureSize.h * scale))
            }
        }

        // Separating line
        r.color = Color.GOLD.withAlpha(0.8)
        r.rect(
            Point(0.0, getUpgradeYPos(highestUpgradeUnlockedSoFar()) + UPGRADE_ENTRY_HEIGHT),
            Size(SCREEN_WIDTH_IN_UNITS, 0.1),
        )
    }

    private var currentlyPurchasingUpgrade: Upgrade? = null
    private var timePurchasingUpgradeStarted = 0.0
    private var currentUpgradePurchaseSoundsPlayed = -1

    private fun cancelUpgradePurchase() {
        currentlyPurchasingUpgrade = null
        currentUpgradePurchaseSoundsPlayed = -1
    }

    /**
     * The amount it's moved in units this frame, on the Y-axis.
     */
    fun cameraHasMoved(deltaUnits: Double) {
        if (deltaUnits.abs > 0.05) {
            cancelUpgradePurchase()
        }
    }

    fun tick() {
        if (cameraVelocity.abs < 0.1) {
            val purchasingFract = purchasingFraction()

            val soundToPlay = purchasingFract * 8
            if (soundToPlay.toInt() > currentUpgradePurchaseSoundsPlayed && purchasingFract > 0.1) {
                playMultiplierSound(soundToPlay.toInt())
                currentUpgradePurchaseSoundsPlayed++
            }

            TOUCHES_WENT_DOWN.forEach { touch ->
                upgradesIterator().forEach { (upgrade, yPos) ->
                    if ((yPos + UPGRADE_ENTRY_HEIGHT / 2 - touch.wo.y).abs < UPGRADE_ENTRY_HEIGHT / 2) {
                        if (upgradeIsPurchasable(upgrade)) {
                            timePurchasingUpgradeStarted = GameTime.APP_TIME
                            currentlyPurchasingUpgrade = upgrade
                        }
                    }
                }
            }

            if (purchasingFract >= 1.0) {
                addScreenShake(0.5)
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

    private fun highestIndexUnlockedSoFar(): Int {
        return Upgrade.values().indexOfLast { playerHas(it) }
    }

    private fun highestUpgradeUnlockedSoFar(): Upgrade? {
        return Upgrade.values().lastOrNull { playerHas(it) }
    }

    private fun upgradeIsPurchasable(upgrade: Upgrade): Boolean {
        val index = upgrade.ordinal
        return (index <= highestIndexUnlockedSoFar() + 1) && !playerHas(upgrade)
    }
}
