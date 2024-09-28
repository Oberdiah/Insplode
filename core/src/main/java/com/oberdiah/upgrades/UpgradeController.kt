package com.oberdiah.upgrades

import com.badlogic.gdx.utils.Align
import com.oberdiah.JUST_UP_OFF_SCREEN
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.Size
import com.oberdiah.UNIT_SIZE_IN_PIXELS
import com.oberdiah.WIDTH
import com.oberdiah.fontSmall
import com.oberdiah.toUISpace
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.utils.StatefulBoolean

private val playerUpgradeStates = mutableMapOf<Upgrade, StatefulBoolean>()

fun initUpgradeController() {
    playerUpgradeStates.clear()
    Upgrade.values().forEach {
        playerUpgradeStates[it] = StatefulBoolean(it.name, false)
    }
    // Eventually we can do something like filling in any gaps that have been
    // added by updates here. For now we'll keep it simple.

    UpgradePuckPositionCalculator.calculatePuckPositions()
}

val UPGRADES_SCREEN_HEIGHT_UNITS
    get() = 4 * SPACING_BETWEEN_UPGRADES // Fake for now

val TOP_OF_UPGRADE_SCREEN_UNITS
    get() = UPGRADES_SCREEN_BOTTOM_Y + UPGRADES_SCREEN_HEIGHT_UNITS

fun renderUpgradeMenu(r: Renderer) {
    if (JUST_UP_OFF_SCREEN < UPGRADES_SCREEN_BOTTOM_Y) {
        return
    }

    UpgradePuckPositionCalculator.getPuckPositions().forEach {
        renderUpgradePuck(r, it.upgrade, it.position)
    }
}

fun renderUpgradePuck(r: Renderer, upgrade: Upgrade, p: Point) {
    r.text(
        fontSmall,
        upgrade.name,
        toUISpace(p),
        Align.center
    )
    r.centeredHollowRect(
        toUISpace(p),
        Size(UNIT_SIZE_IN_PIXELS * PUCK_SIZE, UNIT_SIZE_IN_PIXELS * PUCK_SIZE),
        WIDTH / 150
    )
}

fun playerHas(upgrade: Upgrade): Boolean {
    return playerUpgradeStates[upgrade]?.value ?: false
}