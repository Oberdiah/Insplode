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
        "Touch and drag left and right\nto move. The white line is\nwhere you're heading.",
        1,
    ),

    // ################ EARLY-GAME UPGRADES ################
    SlowerVoid(
        "Void Repellent",
        "Slow down the void's\napproach.",
        5,
    ),
    MediumTimedBomb(
        "Timed Bomb",
        "Occasionally spawns in\na bomb.\nMine for orbs!",
        5,
    ),
    Jump(
        "Jump",
        "Tap to jump.",
        10,
    ),
    SmallTimedBomb(
        "Small Bomb",
        "Helping out with the mining\neffort.",
        15,
    ),
    VoidLift(
        "Void Lift",
        "Haul that darkness away!\nThe void begins higher up.",
        15,
    ),
    LineBomb(
        "Line Bomb",
        "A harmless bomb that\nexplodes in a straight line.",
        25,
    ),
    FasterMovement(
        "Move quicker",
        "Zooooooom!",
        45,
    ),
    LowerGravity(
        "Player Micro-Gravity",
        "Less gravity = higher jump.",
        45,
    ),

    // ################ JUMP UPGRADES ################


    ComboOrbs(
        "Combo Orbs",
        "When a bomb explodes\nanother, orbs are spawned.",
        25,
    ),
    SlowerTimers(
        "Slower Timers",
        "Timers on bombs are\nslowed down.",
        50,
    ),
    GoldenNuggets(
        "Golden Nuggets",
        "Occasional golden tiles\nfull of treasure.",
        10,
    ),
    RapidBombs(
        "Insurgence",
        "Bombs spawn more\nfrequently.\nMore bombs, more combo.",
        100,
    ),
    LargeBombs(
        "Big Bombs",
        "Massive bombs that\nwreck the landscape.",
        75,
    ),
    Slam(
        "Slam",
        "Release finger while in the air\nto slam downwards. Slamming\na bomb makes you bounce back up.",
        150,
    ),

    // ################ SLAM UPGRADES ################

    SpringBomb(
        "Spring Bomb",
        "A bomb that jumps \n around before exploding.",
        75,
    ),
    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each\nsuccessful bomb slam.",
        100,
    ),
    ApexWings(
        "Apex Wings",
        "Hover slightly at the apex of\nyour jump",
        50,
        toggleable = true,
    ),
    SneakySlam(
        "Sneaky Slam",
        "When fuse is half-depleted,\nslam to get double orbs\nand no explosion.",
        10,
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
    StoppedVoid(
        "Void Glue",
        "Glue the void in place,\nnever to move again.",
        15,
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