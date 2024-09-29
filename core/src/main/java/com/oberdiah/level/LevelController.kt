package com.oberdiah.level

import com.oberdiah.BombType
import com.oberdiah.ClusterBomb
import com.oberdiah.DELTA
import com.oberdiah.ImpactBomb
import com.oberdiah.LineBomb
import com.oberdiah.Point
import com.oberdiah.SAFE_BOMB_SPAWN_HEIGHT
import com.oberdiah.SpringBomb
import com.oberdiah.StickyBomb
import com.oberdiah.TimedBomb
import com.oberdiah.UNITS_WIDE
import com.oberdiah.compareTo
import com.oberdiah.d
import com.oberdiah.max
import com.oberdiah.minus
import com.oberdiah.player.player
import com.oberdiah.plus
import com.oberdiah.times
import kotlin.random.Random

var RUN_TIME_ELAPSED = 0.0
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
//        startRandomBombs(BombType.MegaTimed, 6.0)
//    }
//)

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

// It's all going wrong in BombVille

// The values in a phase are how long we wait on that phase before moving on.
val phases = arrayOf(
    Phase(2.0) {
        // Drop pod from above.
    },
    Phase(7.0) {
        spawnBomb(BombType.SmallTimed, 0.25)
    },
    Phase(2.0) {
        spawnBomb(BombType.MediumTimed, 0.75)
    },
    Phase(2.5) {
        spawnBomb(BombType.SmallTimed, 0.25)
    },
    Phase(2.0) {
        spawnBomb(BombType.SmallTimed, 0.6)
    },
    Phase(2.0) {
        spawnBomb(BombType.SmallTimed, 0.2)
    },
    Phase(1.75) {
        startRandomBombs(BombType.SmallTimed, 5.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        spawnBomb(BombType.MediumTimed)
    },
    Phase(0.5, BombType.SpringBomb) {
        stopAllBombs()
        spawnBomb(BombType.SpringBomb, 0.25)
        gameMessage = "Spring Bomb"
    },
    Phase(1.75) {
        gameMessage = ""
        spawnBomb(BombType.SmallTimed)
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
    },
    Phase(2.0) {
        spawnBomb(BombType.SmallTimed, 0.25)
    },
    Phase(4.0, BombType.LargeTimed) {
        spawnBomb(BombType.LargeTimed, 0.5)
        gameMessage = "Large Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
    },
    Phase(4.0, BombType.MegaTimed) {
        spawnBomb(BombType.MegaTimed, 0.75)
        gameMessage = "Mega Bomb"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
        startRandomBombs(BombType.MegaTimed, 15.0)
    },
    Phase(4.0) {
        spawnBomb(BombType.MegaTimed, 0.75)
        gameMessage = "Extreme Phase 1"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 5.0)
        startRandomBombs(BombType.SmallTimed, 4.0)
        startRandomBombs(BombType.MediumTimed, 5.0)
        startRandomBombs(BombType.SpringBomb, 4.0)
        startRandomBombs(BombType.LargeTimed, 8.0)
        startRandomBombs(BombType.MegaTimed, 10.0)
    },
    Phase(4.0) {
        spawnBomb(BombType.MegaTimed, 0.75)
        gameMessage = "Extreme Phase 2"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 4.0)
        startRandomBombs(BombType.SmallTimed, 3.0)
        startRandomBombs(BombType.MediumTimed, 4.0)
        startRandomBombs(BombType.SpringBomb, 3.0)
        startRandomBombs(BombType.LargeTimed, 6.0)
        startRandomBombs(BombType.MegaTimed, 8.0)
    },
    Phase(4.0) {
        spawnBomb(BombType.MegaTimed, 0.25)
        gameMessage = "Final Phase"
    },
    Phase(15.0) {
        gameMessage = ""
        startRandomBombs(BombType.LineBomb, 3.0)
        startRandomBombs(BombType.SmallTimed, 2.0)
        startRandomBombs(BombType.MediumTimed, 3.0)
        startRandomBombs(BombType.SpringBomb, 2.0)
        startRandomBombs(BombType.LargeTimed, 4.0)
        startRandomBombs(BombType.MegaTimed, 6.0)
    },
)

fun resetLevelController() {
    currentPhase = 0
    RUN_TIME_ELAPSED = 0.0
    gameMessage = ""
    maxDepthThisRun = 0.0
    currentDepthThisRun = 0.0
}

var currentDepthThisRun = 0.0
var maxDepthThisRun = 0.0

fun tickLevelController() {
    RUN_TIME_ELAPSED += DELTA

    currentDepthThisRun = max(player.body.p.y, 0.0)
    maxDepthThisRun = max(currentDepthThisRun, maxDepthThisRun)

    bombDropData.forEach { (bombType, bombData) ->
        if (RUN_TIME_ELAPSED > bombData.nextBombAt) {
            bombData.randomiseNextBombAt()
            spawnBomb(bombType)
        }
    }

    var goalTime = 0.0
    phases.forEachIndexed { index, phase ->
        if (index == currentPhase) {
            if (RUN_TIME_ELAPSED > goalTime) {
                phase.callback()
                currentPhase++
            }
        }
        goalTime += phase.d.d
    }
}

fun spawnBomb(type: BombType, fraction: Number = Random.nextDouble(0.05, 0.95)) {
    val pos = Point(fraction * UNITS_WIDE, SAFE_BOMB_SPAWN_HEIGHT)
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

data class BombData(var delay: Number) {
    var nextBombAt: Number = 0.0

    init {
        randomiseNextBombAt()
    }

    fun randomiseNextBombAt() {
        val minTime = 0.0
        val maxTime = delay * 2.0
        nextBombAt = RUN_TIME_ELAPSED + minTime + (maxTime - minTime) * Random.nextDouble()
    }
}

val bombDropData = mutableMapOf<BombType, BombData>()

fun startRandomBombs(type: BombType, delay: Number) {
    bombDropData[type] = BombData(delay)
}

fun stopAllBombs() {
    bombDropData.clear()
}

fun stopRandomBombs(type: BombType) {
    bombDropData.remove(type)
}