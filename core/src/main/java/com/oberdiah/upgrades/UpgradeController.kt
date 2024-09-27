package com.oberdiah.upgrades

private val currentPlayerUpgrades: Set<Upgrade> = setOf()

fun playerHas(upgrade: Upgrade): Boolean {
    return upgrade in currentPlayerUpgrades
}