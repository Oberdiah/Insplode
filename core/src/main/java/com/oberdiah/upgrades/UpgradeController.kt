package com.oberdiah.upgrades

import com.badlogic.gdx.utils.Align
import com.oberdiah.JUST_UP_OFF_SCREEN
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.Size
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.WIDTH
import com.oberdiah.fontSmall
import com.oberdiah.max
import com.oberdiah.toUISpace
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.utils.StatefulBoolean

private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()
private val allUpgradePucks = mutableListOf<UpgradePuck>()
var UPGRADES_SCREEN_HEIGHT_UNITS = 0.0 // Gets updated on init

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
        allUpgradePucks.add(UpgradePuck(upgrade, position, size, linesPointBackTo))
    }

    UPGRADES_SCREEN_HEIGHT_UNITS += UPGRADE_SCREEN_BORDER
}


val TOP_OF_UPGRADE_SCREEN_UNITS
    get() = UPGRADES_SCREEN_BOTTOM_Y + UPGRADES_SCREEN_HEIGHT_UNITS

fun renderUpgradeMenu(r: Renderer) {
    if (JUST_UP_OFF_SCREEN < UPGRADES_SCREEN_BOTTOM_Y) {
        return
    }

    allUpgradePucks.forEach {
        renderUpgradePuck(r, it)
    }
}

fun renderUpgradePuck(r: Renderer, upgradePuck: UpgradePuck) {
    r.text(
        fontSmall,
        upgradePuck.upgrade.name,
        toUISpace(upgradePuck.position),
        Align.center
    )
    r.centeredHollowRect(
        toUISpace(upgradePuck.position),
        Size(UNIT_SIZE_IN_PIXELS * upgradePuck.size, UNIT_SIZE_IN_PIXELS * upgradePuck.size),
        WIDTH / 150
    )
    // Draw lines to the upgrades this one depends on.
    upgradePuck.linesPointBackTo.forEach {
        r.line(toUISpace(it.first), toUISpace(it.second), WIDTH / 150)
    }
}

fun playerHas(upgrade: Upgrade): Boolean {
    return playerUpgradeStates[upgrade]?.value ?: false
}