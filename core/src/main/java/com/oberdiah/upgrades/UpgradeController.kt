package com.oberdiah.upgrades

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.utils.Align
import com.oberdiah.HEIGHT
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.Size
import com.oberdiah.WIDTH
import com.oberdiah.f
import com.oberdiah.fontMedium
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
private val allUpgradePucks = mutableListOf<UpgradePuck>()
var UPGRADES_SCREEN_HEIGHT_UNITS = 0.0 // Gets updated on init
private val allUpgradeTextures = mutableMapOf<Upgrade, Sprite>()

const val UPGRADE_SCREEN_BORDER = 3.0
val EAT_ALL_OTHER_INPUTS: Boolean
    get() {
        return currentlyOpenUpgrade != null
    }

data class UpgradePuck(
    val upgrade: Upgrade,
    val position: Point,
    val size: Double,
    val linesPointBackTo: List<Pair<Point, Point>> = listOf()
)

fun initUpgradeController() {
    playerUpgradeStates.clear()
    Upgrade.values().forEach {
        playerUpgradeStates[it] = StatefulBoolean(it.name, false)
        allUpgradeTextures[it] = Sprite(Texture("Icons/${it.name}.png"))
    }
    // Eventually we can do something like filling in any gaps that have been
    // added by updates here. For now we'll keep it simple.

    val upgradesWithDependencies = mutableMapOf<Upgrade, List<Upgrade>>()
    Upgrade.values().forEachIndexed { index, upgrade ->
        if (upgrade.dependsOn.isEmpty() && index > 0) {
            upgradesWithDependencies[upgrade] = listOf(Upgrade.values()[index - 1])
        } else {
            upgradesWithDependencies[upgrade] = upgrade.dependsOn
        }
    }

    allUpgradePucks.clear()
    UPGRADES_SCREEN_HEIGHT_UNITS = 0.0

    for ((upgrade, dependsOn) in upgradesWithDependencies) {
        val position = upgrade.center
        UPGRADES_SCREEN_HEIGHT_UNITS =
            max(UPGRADES_SCREEN_HEIGHT_UNITS, position.y - UPGRADES_SCREEN_BOTTOM_Y)

        val size = upgrade.size
        val linesPointBackTo = dependsOn.map {
            val from = it.center + Point(0.0, it.size / 2)
            val to = position - Point(0.0, size / 2)
            Pair(from, to)
        }
        allUpgradePucks.add(
            UpgradePuck(
                upgrade,
                position,
                size,
                linesPointBackTo
            )
        )
    }

    UPGRADES_SCREEN_HEIGHT_UNITS += UPGRADE_SCREEN_BORDER
}


val TOP_OF_UPGRADE_SCREEN_UNITS
    get() = UPGRADES_SCREEN_BOTTOM_Y + UPGRADES_SCREEN_HEIGHT_UNITS

fun renderUpgradeMenuWorldSpace(r: Renderer) {
    if (JUST_UP_OFF_SCREEN_UNITS < UPGRADES_SCREEN_BOTTOM_Y) {
        return
    }

    allUpgradePucks.forEach {
        renderUpgradePuck(r, it)
    }
}

fun renderUpgradePuck(r: Renderer, upgradePuck: UpgradePuck) {
    r.color = colorScheme.textColor
//    r.text(
//        fontSmall,
//        upgradePuck.upgrade.title,
//        upgradePuck.position + Point(0.0, upgradePuck.size / 2 + 0.2),
//        Align.center
//    )

    // Draw lines to the upgrades this one depends on.
//    upgradePuck.linesPointBackTo.forEach {
//        r.line(it.first, it.second, UNITS_WIDE / 150.0)
//    }

    r.color = colorScheme.textColor.withAlpha(0.45)
    r.centeredRect(
        upgradePuck.position,
        Size(upgradePuck.size),
        PI / 4
    )
    r.color = Color.WHITE.withAlpha(0.15)
    r.centeredRect(
        upgradePuck.position,
        Size(upgradePuck.size * 0.95),
        PI / 4
    )

    val sprite = allUpgradeTextures[upgradePuck.upgrade] ?: return
    val p = upgradePuck.position
    val s = upgradePuck.size.f
    // Draw the texture centered on the position, scaled as much as it can be without warping.
    val textureSize = Size(sprite.width.f, sprite.height.f)
    val scale = s / max(textureSize.w, textureSize.h).f

    // Shadow
//    r.centeredSprite(
//        sprite,
//        p + Point(0.1, -0.1),
//        Size(textureSize.w * scale, textureSize.h * scale),
//        color = Color.BLACK.withAlpha(0.5)
//    )
    // Main sprite
    r.centeredSprite(sprite, p, Size(textureSize.w * scale, textureSize.h * scale))
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

    if (cameraVelocity < 0.1 && !EAT_ALL_OTHER_INPUTS) {
        TOUCHES_WENT_DOWN.forEach { touch ->
            allUpgradePucks.forEach { upgradePuck ->
                if (upgradePuck.position.distTo(touch.wo) < upgradePuck.size / 2) {
                    currentlyTappingOnUpgrade = upgradePuck.upgrade
                    downPoint = touch
                }
            }
        }

        TOUCHES_WENT_UP.forEach { touch ->
            val touchPoint = touch.wo
            allUpgradePucks.forEach { upgradePuck ->
                if (upgradePuck.position.distTo(touchPoint) < upgradePuck.size / 2) {
                    if (currentlyTappingOnUpgrade == upgradePuck.upgrade && touch.distTo(downPoint) < 3.0) {
                        currentlyOpenUpgrade = upgradePuck.upgrade
                    } else {
                        currentlyTappingOnUpgrade = null
                    }
                }
            }
        }
    } else {
        currentlyTappingOnUpgrade = null
    }
}

fun playerHas(upgrade: Upgrade): Boolean {
    return playerUpgradeStates[upgrade]?.value ?: false
}