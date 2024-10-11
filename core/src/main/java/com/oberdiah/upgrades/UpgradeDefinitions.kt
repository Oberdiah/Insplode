package com.oberdiah.upgrades

import com.oberdiah.Point
import com.oberdiah.SCREEN_WIDTH_IN_UNITS
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y

const val P = 3.0
const val MAIN_UPGRADE_SIZE = 2.0

enum class Upgrade(
    val title: String,
    val description: String,
    val price: Int,
    val toggleable: Boolean = false,
) {
    Movement(
        "Movement",
        "Tap anywhere on the screen\nand drag left and right\nto move.",
        1,
    ),

    // ################ EARLY-GAME UPGRADES ################
    MediumTimedBomb(
        "Timed Bomb",
        "Occasionally drop a bomb\nthat explodes after a short\ndelay.",
        5,
    ),
    ComboOrbs(
        "Combo Orbs",
        "When a bomb explodes\nanother, orbs are spawned.",
        10,
    ),
    BombWizard(
        "Bomb Wizard",
        "Touching a bomb while its\nfuse is in the last quarter of\nits life replenishes it.",
        5,
    ),
    Jump(
        "Jump",
        "Tap to jump.",
        5,
    ),

    // ################ JUMP UPGRADES ################


    FasterMovement(
        "Greater Speed",
        "Move faster.",
        15,
    ),
    MoreSkyOrbs(
        "More Orbs",
        "More orbs fall from the sky.",
        10,
    ),
    SmallTimedBomb(
        "Small Bomb",
        "Extra tiny for easier combos",
        5,
    ),
    LineBomb(
        "Line Bomb",
        "A harmless bomb that\nexplodes in a straight line.",
        10,
    ),
    Slam(
        "Slam",
        "Slam down to the ground to\ncreate an explosion.",
        15,
    ),

    // ################ SLAM UPGRADES ################

    BombSlam(
        "Bomb Slam",
        "Get propelled into the air\nafter slamming into a bomb.",
        10,
    ),
    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each\nsuccessful bomb slam.",
        10,
    ),
    ApexWings(
        "Apex Wings",
        "Hover slightly at the apex of\nyour jump",
        15,
        toggleable = true,
    ),
    SneakySlam(
        "Sneaky Slam",
        "When fuse is half-depleted,\nslam to get double orbs\nand no explosion.",
        10,
    ),
    SlamAssist(
        "Slam Assist",
        "A line that indicates where\nthe player is moving to and\nlights up when they reach it.",
        10,
        toggleable = true,
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
        15,
    ),
    GhostMode(
        "Ghost Mode",
        "While you're in the air bombs\njust phase through you\nharmlessly.",
        20,
    ),
    Multiplier(
        "Multiplier",
        "Gain multiplier for every\nsuccessive bomb slam. Grows\nthe number of orbs spawned.",
        10,
    ),
    MegaMagnet(
        "Mega Magnet",
        "Covers most of the screen.",
        20,
    ),

    // ################ POST-GAME UPGRADES ################

    EvenFasterMovement(
        "Lightning Fast",
        "Move ridiculously fast.",
        25,
    ),
    GlobalMagnet(
        "Global Magnet",
        "The orbs simply cannot\nget away.",
        25,
    ),
    RainbowPlayer(
        "Rainbow Player",
        "Why not?",
        25,
        toggleable = true,
    ),
    ParticleBlackHole(
        "Black Hole",
        "Everything is attracted\nto you.",
        25,
        toggleable = true,
    );

    val obfuscatedTitle = title.replace(Regex("[A-Za-z]")) { "?" }
    val obfuscatedDescription = description.replace(Regex("[A-Za-z]")) { "?" }
}