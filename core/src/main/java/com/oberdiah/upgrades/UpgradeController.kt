package com.oberdiah.upgrades

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.oberdiah.JUST_UP_OFF_SCREEN_UNITS
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.Size
import com.oberdiah.f
import com.oberdiah.max
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.colorScheme
import com.oberdiah.withAlpha
import kotlin.math.PI

private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()
private val allUpgradePucks = mutableListOf<UpgradePuck>()
var UPGRADES_SCREEN_HEIGHT_UNITS = 0.0 // Gets updated on init
private val allUpgradeTextures = mutableMapOf<Upgrade, Texture>()

const val UPGRADE_SCREEN_BORDER = 3.0

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
        allUpgradeTextures[it] = Texture("Icons/${it.name}.png")
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

fun renderUpgradeMenu(r: Renderer) {
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

    r.color = colorScheme.textColor.withAlpha(0.5)
    r.centeredRect(
        upgradePuck.position,
        Size(upgradePuck.size),
        PI / 4
    )
    r.color = Color.WHITE.withAlpha(0.5)
    r.centeredRect(
        upgradePuck.position,
        Size(upgradePuck.size * 0.9),
        PI / 4
    )


    val texture = allUpgradeTextures[upgradePuck.upgrade] ?: return
    val p = upgradePuck.position
    val s = upgradePuck.size.f
    // Draw the texture centered on the position, scaled as much as it can be without warping.
    val textureSize = Size(texture.width.f, texture.height.f)
    val scale = s / max(textureSize.w, textureSize.h).f

    r.centeredImage(texture, p, Size(textureSize.w * scale, textureSize.h * scale))
}

fun playerHas(upgrade: Upgrade): Boolean {
    return playerUpgradeStates[upgrade]?.value ?: false
}