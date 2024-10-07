package com.oberdiah.upgrades

import com.oberdiah.Point
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y

const val P = 3.0
const val MAIN_UPGRADE_SIZE = 2.0
const val SEMI_UPGRADE_SIZE = 1.5
const val SIDE_UPGRADE_SIZE = 1.0

enum class Upgrade(
    val title: String,
    val description: String,
    val price: Int,
    // 0 x for these positions is the center of the screen
    private val position: Point,
    val size: Double,
    // If dependsOn is empty, then this upgrade depends on the one above it and nothing else.
    val dependsOn: List<Upgrade> = listOf(),
    val toggleable: Boolean = false,
) {
    Movement(
        "Movement",
        "Tap anywhere on the screen\nand drag left and right\nto move.",
        1,
        Point(0, 0),
        MAIN_UPGRADE_SIZE
    ),

    // ################ EARLY-GAME UPGRADES ################
    MediumTimedBomb(
        "Timed Bomb",
        "Occasionally drop a bomb\nthat explodes after a short\ndelay.",
        5,
        Point(0, 1 * P),
        MAIN_UPGRADE_SIZE,
    ),
    ComboOrbs(
        "Combo Orbs",
        "When a bomb explodes\nanother, orbs are spawned.",
        10,
        Point(1, 2 * P),
        MAIN_UPGRADE_SIZE,
    ),
    BombMagician(
        "Bomb Magician",
        "Touching a bomb while its\nfuse is in the last quarter of\nits life replenishes it.",
        5,
        Point(-1, 3 * P),
        MAIN_UPGRADE_SIZE,
        listOf(MediumTimedBomb)
    ),
    Jump(
        "Jump",
        "Tap to jump.",
        5,
        Point(0, 4 * P),
        MAIN_UPGRADE_SIZE,
        listOf(ComboOrbs, BombMagician)
    ),

    // ################ JUMP UPGRADES ################


    FasterMovement(
        "Faster Movement",
        "Move faster.",
        15,
        Point(0, 5 * P),
        MAIN_UPGRADE_SIZE,
    ),
    MoreSkyOrbs(
        "More Orbs",
        "More orbs fall from the sky.",
        10,
        Point(0, 6 * P),
        MAIN_UPGRADE_SIZE,
    ),
    SmallTimedBomb(
        "Small Timed Bomb",
        "Extra tiny for easier combos",
        5,
        Point(0, 7 * P),
        MAIN_UPGRADE_SIZE,
    ),
    LineBomb(
        "Line Bomb",
        "A harmless bomb that\nexplodes in a straight line.",
        10,
        Point(0, 8 * P),
        MAIN_UPGRADE_SIZE,
    ),
    Slam(
        "Slam",
        "Slam down to the ground to\ncreate an explosion.",
        15,
        Point(0, 9 * P),
        MAIN_UPGRADE_SIZE,
    ),

    // ################ SLAM UPGRADES ################

    BombSlam(
        "Bomb Slam",
        "Get propelled into the air\nafter slamming into a bomb.",
        10,
        Point(0, 10 * P),
        MAIN_UPGRADE_SIZE,
    ),
    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each\nsuccessful bomb slam.",
        10,
        Point(0, 11 * P),
        MAIN_UPGRADE_SIZE,
    ),
    ApexWings(
        "Apex Wings",
        "Hover slightly at the apex of\nyour jump",
        15,
        Point(0, 12 * P),
        MAIN_UPGRADE_SIZE,
        toggleable = true,
    ),
    SneakySlam(
        "Sneaky Slam",
        "When fuse is half-depleted\nslam to get double orbs\nand no explosion.",
        10,
        Point(0, 13 * P),
        MAIN_UPGRADE_SIZE,
    ),
    SlamAssist(
        "Slam Assist",
        "A line that indicates where\nthe player is moving to and\nlights up when they reach it.",
        10,
        Point(0, 14 * P),
        MAIN_UPGRADE_SIZE,
        toggleable = true,
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
        15,
        Point(0, 15 * P),
        MAIN_UPGRADE_SIZE,
    ),
    GhostMode(
        "Ghost Mode",
        "While you're in the air bombs\njust phase through you\nharmlessly.",
        20,
        Point(0, 16 * P),
        MAIN_UPGRADE_SIZE,
    ),
    Multiplier(
        "Multiplier",
        "Gain multiplier for every\nsuccessive bomb slam. Grows\nthe number of orbs spawned.",
        10,
        Point(0, 17 * P),
        MAIN_UPGRADE_SIZE,
    ),
    MegaMagnet(
        "Mega Magnet",
        "Covers most of the screen.",
        20,
        Point(0, 18 * P),
        MAIN_UPGRADE_SIZE,
    ),

    // ################ POST-GAME UPGRADES ################

    EvenFasterMovement(
        "Fastest Movement",
        "Move ridiculously fast.",
        25,
        Point(0, 19 * P),
        MAIN_UPGRADE_SIZE,
    ),
    SingularityMagnet(
        "Singularity Magnet",
        "The orbs simply cannot\nget away.",
        25,
        Point(0, 20 * P),
        MAIN_UPGRADE_SIZE,
    ),
    RainbowPlayer(
        "Rainbow Player",
        "Why not?",
        25,
        Point(0, 21 * P),
        MAIN_UPGRADE_SIZE,
        toggleable = true,
    ),
    ParticleBlackHole(
        "Particle Black Hole",
        "All particles are\nattracted to you.",
        25,
        Point(0, 22 * P),
        MAIN_UPGRADE_SIZE,
        toggleable = true,
    );

    val center
        get() = position + Point(
            SCREEN_WIDTH_IN_UNITS / 2.0,
            UPGRADES_SCREEN_BOTTOM_Y + UpgradeController.UPGRADE_SCREEN_BORDER
        )
}