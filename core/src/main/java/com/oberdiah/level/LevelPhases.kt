package com.oberdiah.level

import com.oberdiah.BombType


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