package com.oberdiah.upgrades

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.Size
import com.oberdiah.abs
import com.oberdiah.easeInOutSine
import com.oberdiah.f
import com.oberdiah.fontSmallish
import com.oberdiah.fontTiny
import com.oberdiah.lerp
import com.oberdiah.max
import com.oberdiah.saturate
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.ui.cameraVelocity
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha

object UpgradeController {
    private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()
    var UPGRADES_SCREEN_HEIGHT_UNITS = 0.0 // Gets updated on init
    private val allUpgradeTextures = mutableMapOf<Upgrade, Sprite>()

    const val UPGRADE_ENTRY_HEIGHT = 4.0
    const val ICON_SIZE = 3.0
    const val UPGRADE_SCREEN_BORDER = 1.0

    fun init() {
        playerUpgradeStates.clear()
        Upgrade.values().forEach {
            playerUpgradeStates[it] = StatefulBoolean(it.name, false)
            allUpgradeTextures[it] = Sprite(Texture("Icons/${it.name}.png"))
        }
        // Eventually we can do something like filling in any gaps that have been
        // added by updates here. For now we'll keep it simple.

        UPGRADES_SCREEN_HEIGHT_UNITS =
            UPGRADE_ENTRY_HEIGHT * Upgrade.values().size + UPGRADE_SCREEN_BORDER * 2
    }


    val TOP_OF_UPGRADE_SCREEN_UNITS
        get() = UPGRADES_SCREEN_BOTTOM_Y + UPGRADES_SCREEN_HEIGHT_UNITS

    /**
     * An iterator of upgrade and its Y-position in the upgrade menu.
     */
    fun upgradesIterator(): Iterator<Pair<Upgrade, Double>> {
        return Upgrade.values().mapIndexed { index, upgrade ->
            upgrade to UPGRADES_SCREEN_BOTTOM_Y + UPGRADE_SCREEN_BORDER + UPGRADE_ENTRY_HEIGHT * index
        }.iterator()
    }

    fun renderUpgradeMenuWorldSpace(r: Renderer) {
        if (JUST_UP_OFF_SCREEN_UNITS < UPGRADES_SCREEN_BOTTOM_Y) {
            return
        }

        val iconXPos = 2.5
        var drawInBlue = false
        val transition =
            easeInOutSine(saturate((GameTime.APP_TIME - timeLastUpgradeSwitchOccurred) * 10.0))

        for ((upgrade, yPos) in upgradesIterator()) {
            drawInBlue = !drawInBlue

            val alphaForColor = if (currentlyTappingOnUpgrade == upgrade) 0.2 else 0.11

            r.color = (if (drawInBlue) Color.CYAN else Color.YELLOW).withAlpha(alphaForColor)
            r.rect(
                Point(0.0, yPos),
                Size(SCREEN_WIDTH_IN_UNITS, UPGRADE_ENTRY_HEIGHT),
            )

            // Separating line
            r.color = Color.BLACK.withAlpha(0.2)
            r.rect(
                Point(0.0, yPos),
                Size(SCREEN_WIDTH_IN_UNITS, 0.05),
            )

            val textXPosNotSelected = 4.75
            val textXPosSelected = 10.75

            val textXPos = if (currentlyOpenUpgrade == upgrade) {
                lerp(textXPosNotSelected, textXPosSelected, transition)
            } else if (lastOpenUpgrade == upgrade) {
                lerp(textXPosSelected, textXPosNotSelected, transition)
            } else {
                textXPosNotSelected
            }

            r.color = colorScheme.textColor
            r.text(
                fontSmallish,
                upgrade.title,
                Point(textXPos, yPos + UPGRADE_ENTRY_HEIGHT * 0.7),
                Align.left
            )

            r.text(
                fontTiny,
                upgrade.description,
                Point(textXPos, yPos + UPGRADE_ENTRY_HEIGHT * 0.375),
                Align.left
            )

            // Small line
            r.color = Color.BLACK.withAlpha(0.5)
            r.rect(
                Point(textXPos + 0.05, yPos + UPGRADE_ENTRY_HEIGHT * 0.5),
                Size(4.0, 0.05),
            )

            val sprite = allUpgradeTextures[upgrade] ?: break
            val p = Point(iconXPos, yPos + UPGRADE_ENTRY_HEIGHT / 2)
            // Draw the texture centered on the position, scaled as much as it can be without warping.
            val textureSize = Size(sprite.width.f, sprite.height.f)
            val scale = ICON_SIZE / max(textureSize.w, textureSize.h).f

            val shadowDirection = Point(0.1, -0.1)

            // Shadow
            r.centeredSprite(
                sprite,
                p + shadowDirection,
                Size(textureSize.w * scale, textureSize.h * scale),
                color = Color.BLACK.withAlpha(0.5)
            )
            // Main sprite
            r.centeredSprite(sprite, p, Size(textureSize.w * scale, textureSize.h * scale))
        }
    }

    private var currentlyOpenUpgrade: Upgrade? = null
    private var lastOpenUpgrade: Upgrade? = null
    private var currentlyTappingOnUpgrade: Upgrade? = null
    private var timeLastUpgradeSwitchOccurred = 0.0
    private var timeLastUpgradeFingerDownOccurred = 0.0
    private var downPoint = Point(0.0, 0.0)

    /**
     * The amount it's moved in units this frame, on the Y-axis.
     */
    fun cameraHasMoved(deltaUnits: Double) {
        if (deltaUnits.abs > 0.01) {
            currentlyTappingOnUpgrade = null
        }
    }

    fun tick() {
        if (cameraVelocity.abs < 0.1) {
            TOUCHES_WENT_DOWN.forEach { touch ->
                upgradesIterator().forEach { (upgrade, yPos) ->
                    if ((yPos + UPGRADE_ENTRY_HEIGHT / 2 - touch.wo.y).abs < UPGRADE_ENTRY_HEIGHT / 2) {
                        timeLastUpgradeFingerDownOccurred = GameTime.APP_TIME

                        currentlyTappingOnUpgrade = upgrade
                        downPoint = touch
                    }
                }
            }

            TOUCHES_WENT_UP.forEach { touch ->
                upgradesIterator().forEach { (upgrade, yPos) ->
                    if ((yPos + UPGRADE_ENTRY_HEIGHT / 2 - touch.wo.y).abs < UPGRADE_ENTRY_HEIGHT / 2) {
                        if (currentlyTappingOnUpgrade == upgrade &&
                            touch.distTo(downPoint) < 3.0 &&
                            currentlyOpenUpgrade != upgrade
                        ) {
                            timeLastUpgradeSwitchOccurred = GameTime.APP_TIME

                            lastOpenUpgrade = currentlyOpenUpgrade
                            currentlyOpenUpgrade = upgrade
                        }
                    }
                }
                currentlyTappingOnUpgrade = null
            }
        } else {
            currentlyTappingOnUpgrade = null
        }
    }

    fun playerHas(upgrade: Upgrade): Boolean {
        return playerUpgradeStates[upgrade]?.value ?: false
    }
}
