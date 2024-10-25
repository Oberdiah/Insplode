package com.oberdiah.level

import com.oberdiah.BombType

/**
 * If we reach the expected depth, we flashforward to this phase if we're not there yet.
 *
 * Important note: The flashforward does not run any previous phases.
 *
 * We will also still wait until the appropriate time before moving on, it just runs
 * a single phase early, rather than pushing the clock forward.
 */
class Phase(val d: Number, val expectedDepthUnits: Double? = null, val callback: () -> Unit)

//val phases = arrayOf(
//    Phase(5.0) {
//        startRandomBombs(BombType.LineBomb, 3.0)
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

// The values in a phase are how long we wait on that phase before moving on.
val phases = arrayOf(
    Phase(TIME_FOR_LASER_TO_DESCEND) {},
    Phase(1.5) {
        spawnBomb(BombType.SmallTimed, 0.6)
        spawnBomb(BombType.PointOrb, 0.1)
    },
    Phase(1.0) {
        spawnBomb(BombType.SmallTimed, 0.2)
        spawnBomb(BombType.PointOrb, 0.51)
    },
    Phase(2.4) {
        startRandomBombs(BombType.SmallTimed, 5.0)
        spawnBomb(BombType.MediumTimed, 0.75)
        spawnBomb(BombType.PointOrb, 0.65)
        startRandomBombs(BombType.PointOrb, 3.5)
    },
    Phase(0.05) {
        spawnBomb(BombType.SpringBomb, 0.25)
        spawnBomb(BombType.PointOrb, 0.49)
    },
    Phase(1.65) {
        spawnBomb(BombType.SmallTimed)
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 8.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
    },
    Phase(2.0) {
        spawnBomb(BombType.SmallTimed, 0.25)
        spawnBomb(BombType.LineBomb, 0.4)
    },
    Phase(1.5) {
        spawnBomb(BombType.LargeTimed, 0.5)
    },
    Phase(3.0) {
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 6.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
    },
    Phase(3.0) {
        spawnBomb(BombType.MediumTimed, 0.7)
    },
    Phase(2.0) {
        spawnBomb(BombType.MediumTimed, 0.3)
    },
    Phase(7.0) {
        spawnBomb(BombType.PointOrb, 0.49)
        spawnBomb(BombType.MediumTimed, 0.25)
    },
    // 25.1s here.
    Phase(7.0) {
        spawnBomb(BombType.MediumTimed, 0.25)
        spawnBomb(BombType.MediumTimed, 0.75)

        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 5.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
    },
    Phase(4.0) {
        spawnBomb(BombType.SpringBomb, 0.85)
    },
    Phase(4.0) {
        spawnBomb(BombType.SpringBomb, 0.2)
    },
    Phase(2.0) {
        spawnBomb(BombType.MegaTimed, 0.75)
    },
    Phase(15.0) {
        startRandomBombs(BombType.LineBomb, 7.0)
        startRandomBombs(BombType.SmallTimed, 6.0)
        startRandomBombs(BombType.MediumTimed, 7.0)
        startRandomBombs(BombType.SpringBomb, 6.0)
        startRandomBombs(BombType.LargeTimed, 10.0)
        startRandomBombs(BombType.MegaTimed, 15.0)
    },
    Phase(2.0) {
        spawnBomb(BombType.MegaTimed, 0.75)
    },
    Phase(15.0) {
        startRandomBombs(BombType.LineBomb, 5.0)
        startRandomBombs(BombType.SmallTimed, 4.0)
        startRandomBombs(BombType.MediumTimed, 5.0)
        startRandomBombs(BombType.SpringBomb, 4.0)
        startRandomBombs(BombType.LargeTimed, 8.0)
        startRandomBombs(BombType.MegaTimed, 10.0)
    },
    Phase(2.0) {
        spawnBomb(BombType.MegaTimed, 0.75)
    },
    Phase(15.0) {
        startRandomBombs(BombType.LineBomb, 4.0)
        startRandomBombs(BombType.SmallTimed, 3.0)
        startRandomBombs(BombType.MediumTimed, 4.0)
        startRandomBombs(BombType.SpringBomb, 3.0)
        startRandomBombs(BombType.LargeTimed, 6.0)
        startRandomBombs(BombType.MegaTimed, 8.0)
    },
    Phase(2.0) {
        spawnBomb(BombType.MegaTimed, 0.25)
    },
    Phase(15.0) {
        startRandomBombs(BombType.LineBomb, 3.0)
        startRandomBombs(BombType.SmallTimed, 2.0)
        startRandomBombs(BombType.MediumTimed, 3.0)
        startRandomBombs(BombType.SpringBomb, 2.0)
        startRandomBombs(BombType.LargeTimed, 4.0)
        startRandomBombs(BombType.MegaTimed, 8.0)
    },
    Phase(2.0) {
        spawnBomb(BombType.MegaTimed, 0.25)
    },
    Phase(15.0) {
        startRandomBombs(BombType.LineBomb, 2.0)
        startRandomBombs(BombType.SmallTimed, 1.3)
        startRandomBombs(BombType.MediumTimed, 2.0)
        startRandomBombs(BombType.SpringBomb, 1.5)
        startRandomBombs(BombType.LargeTimed, 3.0)
        startRandomBombs(BombType.MegaTimed, 6.0)
        startRandomBombs(BombType.UltraTimed, 10.0)
    },
)
