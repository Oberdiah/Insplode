package com.oberdiah.upgrades

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import com.oberdiah.CAMERA_POS_Y
import com.oberdiah.HEIGHT
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.Size
import com.oberdiah.WIDTH
import com.oberdiah.f
import com.oberdiah.fontMedium
import com.oberdiah.fontSmallish
import com.oberdiah.fontTiny
import com.oberdiah.max
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.ui.cameraVelocity
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha
import kotlin.math.PI

private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()
var UPGRADES_SCREEN_HEIGHT_UNITS = 0.0 // Gets updated on init
private val allUpgradeTextures = mutableMapOf<Upgrade, Sprite>()

const val UPGRADE_ENTRY_HEIGHT = 4.0
const val ICON_SIZE = 3.0
const val UPGRADE_SCREEN_BORDER = 1.0

fun initUpgradeController() {
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

fun renderUpgradeMenuWorldSpace(r: Renderer) {
    if (JUST_UP_OFF_SCREEN_UNITS < UPGRADES_SCREEN_BOTTOM_Y) {
        return
    }

    val iconXPos = 2.5
    var sectionYPos = UPGRADES_SCREEN_BOTTOM_Y + UPGRADE_SCREEN_BORDER

    var drawInBlue = false

    for (upgrade in Upgrade.values()) {
        drawInBlue = !drawInBlue
        r.color = (if (drawInBlue) Color.CYAN else Color.YELLOW).withAlpha(0.11)
        r.rect(
            Point(0.0, sectionYPos),
            Size(SCREEN_WIDTH_IN_UNITS, UPGRADE_ENTRY_HEIGHT),
        )

        // Separating line
        r.color = Color.BLACK.withAlpha(0.2)
        r.rect(
            Point(0.0, sectionYPos),
            Size(SCREEN_WIDTH_IN_UNITS, 0.05),
        )

        r.color = colorScheme.textColor
        r.text(
            fontSmallish,
            upgrade.title,
            Point(4.75, sectionYPos + UPGRADE_ENTRY_HEIGHT * 0.7),
            Align.left
        )

        r.text(
            fontTiny,
            upgrade.description,
            Point(4.75, sectionYPos + UPGRADE_ENTRY_HEIGHT * 0.375),
            Align.left
        )

        // Small line
        r.color = Color.BLACK.withAlpha(0.5)
        r.rect(
            Point(4.8, sectionYPos + UPGRADE_ENTRY_HEIGHT * 0.5),
            Size(4.0, 0.05),
        )

        val sprite = allUpgradeTextures[upgrade] ?: break
        val p = Point(iconXPos, sectionYPos + UPGRADE_ENTRY_HEIGHT / 2)
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

        sectionYPos += UPGRADE_ENTRY_HEIGHT
    }
}

fun renderUpgradeMenuScreenSpace(r: Renderer) {
    val openUpgrade = currentlyOpenUpgrade
    if (openUpgrade != null) {
        val windowSize = Size(WIDTH * 0.8, HEIGHT * 0.6)

        r.color = colorScheme.textColor.withAlpha(0.75)
        r.centeredRect(
            Point(WIDTH / 2, HEIGHT / 2),
            windowSize,
        )
        r.color = Color.WHITE.withAlpha(0.75)
        r.centeredRect(
            Point(WIDTH / 2, HEIGHT / 2),
            Size(windowSize - Size(5.0))
        )
        r.color = colorScheme.textColor
        r.text(
            fontMedium,
            openUpgrade.title,
            Point(WIDTH / 2, HEIGHT / 2 + windowSize.h / 2 - 5.0),
            Align.center
        )
    }
}

var currentlyOpenUpgrade: Upgrade? = null
var currentlyTappingOnUpgrade: Upgrade? = null
var downPoint = Point(0.0, 0.0)

fun tickUpgradeController() {
    if (true) {
        // For now, to stop people getting stuck
        return
    }

    if (cameraVelocity < 0.1) {
//        TOUCHES_WENT_DOWN.forEach { touch ->
//            allUpgradePucks.forEach { upgradePuck ->
//                if (upgradePuck.position.distTo(touch.wo) < upgradePuck.size / 2) {
//                    currentlyTappingOnUpgrade = upgradePuck.upgrade
//                    downPoint = touch
//                }
//            }
//        }
//
//        TOUCHES_WENT_UP.forEach { touch ->
//            val touchPoint = touch.wo
//            allUpgradePucks.forEach { upgradePuck ->
//                if (upgradePuck.position.distTo(touchPoint) < upgradePuck.size / 2) {
//                    if (currentlyTappingOnUpgrade == upgradePuck.upgrade && touch.distTo(downPoint) < 3.0) {
//                        currentlyOpenUpgrade = upgradePuck.upgrade
//                    } else {
//                        currentlyTappingOnUpgrade = null
//                    }
//                }
//            }
//        }
    } else {
        currentlyTappingOnUpgrade = null
    }
}

fun playerHas(upgrade: Upgrade): Boolean {
    return playerUpgradeStates[upgrade]?.value ?: false
}