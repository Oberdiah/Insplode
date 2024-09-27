package com.oberdiah.upgrades

import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y

private val currentPlayerUpgrades: Set<Upgrade> = setOf()

const val SPACING_BETWEEN_UPGRADES = 2.0

val UPGRADES_SCREEN_HEIGHT_UNITS
    get() = 4 * SPACING_BETWEEN_UPGRADES // Fake for now

val TOP_OF_UPGRADE_SCREEN_UNITS
    get() = UPGRADES_SCREEN_BOTTOM_Y + UPGRADES_SCREEN_HEIGHT_UNITS

fun renderDiegeticMenu() {
    // Render the diegetic menu here
}

fun playerHas(upgrade: Upgrade): Boolean {
    return upgrade in currentPlayerUpgrades
}