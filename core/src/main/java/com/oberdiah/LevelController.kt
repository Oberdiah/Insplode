package com.oberdiah

import kotlin.random.Random

var runTimeElapsed = 0.0
var gameMessage = ""
var currentPhase = 0

class Phase(val d: Number, val callback: () -> Unit) {
    var bombType: BombType? = null

    constructor(d: Number, bomb: BombType, callback: () -> Unit) : this(d, callback) {
        bombType = bomb
    }
}


//val phases = arrayOf(
//    Phase(5.0) {
//        startRandomBombs(BombType.StickyBomb, 6.0)
//        startRandomBombs(BombType.ImpactBomb, 6.0)
//        startRandomBombs(BombType.SpringBomb, 6.0)
//        startRandomBombs(BombType.SmallTimed, 6.0)
//        startRandomBombs(BombType.MediumTimed, 6.0)
//        startRandomBombs(BombType.LargeTimed, 6.0)
//        startRandomBombs(BombType.ClusterBomb, 6.0)
//        startRandomBombs(BombType.LineBomb, 6.0)
//    }
//)

val phases = arrayOf(
    Phase(4.0) {
        // Drop pod from above.
    },
    Phase(2.0) { gameMessage = "GAME START" },
    Phase(3.0) { gameMessage = "" },
    Phase(4.0) { spawnBomb(BombType.SmallTimed, 0.25) },
    Phase(10.0) { startRandomBombs(BombType.SmallTimed, 4.0) },
    Phase(5.0) { startRandomBombs(BombType.SmallTimed, 3.0) },
    Phase(4.0, BombType.MediumTimed) {
        stopAllBombs()
        gameMessage = "Medium Bomb"
        spawnBomb(BombType.MediumTimed, 0.5)
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.SmallTimed, 3.0)
        startRandomBombs(BombType.MediumTimed, 5.0)
    },
    Phase(4.0, BombType.LineBomb) {
        stopAllBombs()
        spawnBomb(BombType.LineBomb, 0.75)
        gameMessage = "Line Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 6.0)
        startRandomBombs(BombType.SmallTimed, 5.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
    },
    Phase(4.0, BombType.ImpactBomb) {
        stopAllBombs()
        spawnBomb(BombType.ImpactBomb, 0.5)
        gameMessage = "Impact Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.ImpactBomb, 6.0)
    },
    Phase(4.0, BombType.StickyBomb) {
        stopAllBombs()
        spawnBomb(BombType.StickyBomb, 0.25)
        gameMessage = "Sticky Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.StickyBomb, 6.0)
        startRandomBombs(BombType.ImpactBomb, 6.0)
    },
    Phase(4.0, BombType.SpringBomb) {
        stopAllBombs()
        spawnBomb(BombType.SpringBomb, 0.25)
        gameMessage = "Spring Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.StickyBomb, 6.0)
        startRandomBombs(BombType.ImpactBomb, 6.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
    },
    Phase(4.0, BombType.LargeTimed) {
        stopAllBombs()
        spawnBomb(BombType.LargeTimed, 0.5)
        gameMessage = "Large Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.StickyBomb, 6.0)
        startRandomBombs(BombType.ImpactBomb, 6.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
    },
    Phase(4.0, BombType.ClusterBomb) {
        stopAllBombs()
        spawnBomb(BombType.ClusterBomb, 0.75)
        gameMessage = "Cluster Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.StickyBomb, 6.0)
        startRandomBombs(BombType.ImpactBomb, 6.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
        startRandomBombs(BombType.ClusterBomb, 6.0)
    },
    Phase(4.0, BombType.MegaTimed) {
        stopAllBombs()
        spawnBomb(BombType.MegaTimed, 0.75)
        gameMessage = "Mega Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.StickyBomb, 6.0)
        startRandomBombs(BombType.ImpactBomb, 6.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
        startRandomBombs(BombType.ClusterBomb, 6.0)
        startRandomBombs(BombType.MegaTimed, 15.0)
    },
    Phase(4.0) {
        stopAllBombs()
        spawnBomb(BombType.MegaTimed, 0.75)
        gameMessage = "Extreme Phase 1"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 5.0)
        startRandomBombs(BombType.SmallTimed, 4.0)
        startRandomBombs(BombType.MediumTimed, 5.0)
        startRandomBombs(BombType.StickyBomb, 4.0)
        startRandomBombs(BombType.ImpactBomb, 4.0)
        startRandomBombs(BombType.SpringBomb, 4.0)
        startRandomBombs(BombType.LargeTimed, 8.0)
        startRandomBombs(BombType.ClusterBomb, 4.0)
        startRandomBombs(BombType.MegaTimed, 10.0)
    },
    Phase(4.0) {
        stopAllBombs()
        spawnBomb(BombType.MegaTimed, 0.75)
        gameMessage = "Extreme Phase 2"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 4.0)
        startRandomBombs(BombType.SmallTimed, 3.0)
        startRandomBombs(BombType.MediumTimed, 4.0)
        startRandomBombs(BombType.StickyBomb, 3.0)
        startRandomBombs(BombType.ImpactBomb, 3.0)
        startRandomBombs(BombType.SpringBomb, 3.0)
        startRandomBombs(BombType.LargeTimed, 6.0)
        startRandomBombs(BombType.ClusterBomb, 3.0)
        startRandomBombs(BombType.MegaTimed, 8.0)
    },
    Phase(4.0) {
        stopAllBombs()
        spawnBomb(BombType.MegaTimed, 0.75)
        gameMessage = "Final Phase"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 3.0)
        startRandomBombs(BombType.SmallTimed, 2.0)
        startRandomBombs(BombType.MediumTimed, 3.0)
        startRandomBombs(BombType.StickyBomb, 2.0)
        startRandomBombs(BombType.ImpactBomb, 2.0)
        startRandomBombs(BombType.SpringBomb, 2.0)
        startRandomBombs(BombType.LargeTimed, 4.0)
        startRandomBombs(BombType.ClusterBomb, 2.0)
        startRandomBombs(BombType.MegaTimed, 6.0)
    },
)

fun resetLevelController() {
    currentPhase = 0
    runTimeElapsed = 0.0
    gameMessage = ""
    maxDepthThisRun = 0.0
    currentDepthThisRun = 0.0
}

var currentDepthThisRun = 0.0
var maxDepthThisRun = 0.0

fun tickLevelController() {
    runTimeElapsed += DELTA

    currentDepthThisRun = max((LAND_SURFACE_Y - player.body.p.y), 0.0)
    maxDepthThisRun = max(currentDepthThisRun, maxDepthThisRun)
    if (maxDepthThisRun > HIGH_SCORE) {
        HIGH_SCORE = maxDepthThisRun
    }

    bombDropData.forEach { (bombType, bombData) ->
        if (runTimeElapsed > bombData.nextBombAt) {
            val minTime = bombData.delay / 2.0
            val maxTime = bombData.delay
            bombData.nextBombAt =
                runTimeElapsed + minTime + (maxTime - minTime) * Random.nextDouble()
            spawnBomb(bombType)
        }
    }

    var goalTime = 0.0
    phases.forEachIndexed { index, phase ->
        if (index == currentPhase) {
            if (runTimeElapsed > goalTime) {
                phase.callback()
                currentPhase++
            }
        }
        goalTime += phase.d.d
    }
}

fun spawnBomb(type: BombType, fraction: Number = Random.nextDouble()) {
    val pos = Point((fraction * 0.9 + 0.05) * SQUARES_WIDE, SAFE_SPAWN_HEIGHT)
    when (type) {
        BombType.SmallTimed -> TimedBomb(pos, type)
        BombType.MediumTimed -> TimedBomb(pos, type)
        BombType.LargeTimed -> TimedBomb(pos, type)
        BombType.MegaTimed -> TimedBomb(pos, type)
        BombType.LineBomb -> LineBomb(pos)
        BombType.ClusterBomb -> ClusterBomb(pos)
        BombType.StickyBomb -> StickyBomb(pos)
        BombType.ImpactBomb -> ImpactBomb(pos)
        BombType.SpringBomb -> SpringBomb(pos)
        else -> throw Exception()
    }
}

data class BombData(var delay: Number, var nextBombAt: Number)

val bombDropData = mutableMapOf<BombType, BombData>()

fun startRandomBombs(type: BombType, delay: Number) {
    bombDropData[type] = BombData(delay, runTimeElapsed)
}

fun stopAllBombs() {
    bombDropData.clear()
}

fun stopRandomBombs(type: BombType) {
    bombDropData.remove(type)
}