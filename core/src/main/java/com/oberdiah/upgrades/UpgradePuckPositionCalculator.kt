package com.oberdiah.upgrades

import com.oberdiah.Point
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y

const val PUCK_SIZE = 2.0
const val SPACING_BETWEEN_UPGRADES = 3.0

object UpgradePuckPositionCalculator {
    private data class DAGNode(val outgoing: List<Upgrade>, val lengthToEnd: Int)
    data class UpgradePuck(
        val upgrade: Upgrade,
        val position: Point,
        val pointsTo: List<Upgrade>
    )

    private val upgradePuckPositions = mutableListOf<UpgradePuck>()
    private val upgradeDAG = mutableMapOf<Upgrade, DAGNode>()

    fun getPuckPositions(): List<UpgradePuck> {
        return upgradePuckPositions
    }

    fun calculatePuckPositions() {
        upgradeDAG.clear()
        upgradePuckPositions.clear()

        // A map from each upgrade to any upgrades that directly depend on it.
        val upgradeNearDAG = mutableMapOf<Upgrade, List<Upgrade>>()

        Upgrade.values().forEachIndexed { index, it ->
            val thingsThatDependOnThis = mutableListOf<Upgrade>()
            Upgrade.values().forEachIndexed { innerIndex, upgrade ->
                // if the depends list is empty, it depends on the previous upgrade
                if (upgrade.dependsOn.isEmpty() && innerIndex == index + 1) {
                    thingsThatDependOnThis.add(upgrade)
                }
                if (upgrade.dependsOn.contains(it)) {
                    thingsThatDependOnThis.add(upgrade)
                }
            }
            upgradeNearDAG[it] = thingsThatDependOnThis
        }

        Upgrade.values().forEach {
            var length = 0
            var currentUpgrade = it
            while (true) {
                val nextUpgrades = upgradeNearDAG[currentUpgrade] ?: break
                if (nextUpgrades.isEmpty()) {
                    break
                }
                currentUpgrade = nextUpgrades[0]
                length++
            }
            upgradeDAG[it] = DAGNode(upgradeNearDAG[it]!!, length)
        }

        val currentUpgrade = Upgrade.values().first()
        val currentY = UPGRADES_SCREEN_BOTTOM_Y + SPACING_BETWEEN_UPGRADES

        buildUpgradeTower(currentUpgrade, Point(SCREEN_WIDTH_IN_UNITS / 2, currentY))
    }

    private fun buildUpgradeTower(upgradeIn: Upgrade, startPoint: Point) {
        val currentPoint = startPoint.cpy
        var currentUpgrade = upgradeIn

        val toBuildUpgradeTowerFor = mutableListOf<Pair<Upgrade, Point>>()

        // Make a column of upgrades
        while (true) {
            if (upgradePuckPositions.any { it.upgrade == currentUpgrade }) {
                break
            }

            upgradePuckPositions.add(
                UpgradePuck(
                    currentUpgrade,
                    currentPoint.cpy,
                    upgradeDAG[currentUpgrade]?.outgoing ?: emptyList()
                )
            )
            currentPoint.y += SPACING_BETWEEN_UPGRADES
            // Our next upgrade is the outgoing branch with the longest lengthToEnd
            val nextUpgrade = upgradeDAG[currentUpgrade]?.outgoing?.maxByOrNull {
                upgradeDAG[it]?.lengthToEnd ?: 0
            }
            if (nextUpgrade == null) {
                break
            }

            // All other upgrades that depend on this one get their own offset upgrade tower
            upgradeDAG[currentUpgrade]?.outgoing?.forEachIndexed { index, it ->
                if (it != nextUpgrade) {
                    toBuildUpgradeTowerFor.add(
                        Pair(
                            it,
                            currentPoint + Point(index * SPACING_BETWEEN_UPGRADES, 0.0)
                        )
                    )
                }
            }

            currentUpgrade = nextUpgrade
        }

        toBuildUpgradeTowerFor.forEach {
            buildUpgradeTower(it.first, it.second)
        }
    }
}
