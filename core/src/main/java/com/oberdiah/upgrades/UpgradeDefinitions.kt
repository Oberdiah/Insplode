package com.oberdiah.upgrades

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
    ComboOrbs(
        "Combo Orbs",
        "When a bomb explodes\nanother, orbs are spawned.",
        25,
    ),
    FasterMovement(
        "Move quicker",
        "Zooooooom!",
        45,
    ),
    LowerGravity(
        "Micro-Gravity",
        "Only applies to the player.\nLess gravity = higher jump.",
        45,
    ),

    // ################ JUMP UPGRADES ################

    LineBomb(
        "Line Bomb",
        "A harmless bomb that\nexplodes in a straight line.",
        60,
    ),
    SlowerTimers(
        "Slower Timers",
        "Timers on bombs are\nslowed down.",
        50,
    ),
    SpringBomb(
        "Spring Bomb",
        "A bomb that jumps \n around before exploding.",
        75,
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
        100,
    ),
    Slam(
        "Slam",
        "Release finger while in the air\nto slam downwards. Slamming\na bomb makes you bounce back up.",
        75,
    ),

    // ################ SLAM UPGRADES ################

    LargeTimedBomb(
        "Large Bombs",
        "Bigger can only mean better.\nWorth more, too.",
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
        200,
        toggleable = true,
    ),
    MegaTimedBomb(
        "Mega Bombs",
        "Massive bombs that\nwreck the landscape.",
        75,
    ),
    GoldenNuggets(
        "Golden Nuggets",
        "Occasional golden tiles\nfull of treasure.",
        150,
    ),
    SneakySlam(
        "Sneaky Slam",
        "When fuse is half-depleted,\nslam to get double orbs\nand no explosion.",
        100,
    ),
    Multiplier(
        "Multiplier",
        "Gain multiplier for each\nbomb slam. Applies to\nthe number of orbs spawned.",
        200,
    ),
    RapidBombs(
        "Insurgence",
        "Bombs spawn more\nfrequently.\nMore bombs, more fun.",
        150,
    ),
    MegaMagnet(
        "Mega Magnet",
        "Covers most of the screen.",
        300,
    ),
    EvenFasterMovement(
        "Lightning Fast",
        "Move ridiculously fast.",
        350,
    ),

    // ################ POST-GAME UPGRADES ################

    UltraTimedBomb(
        "Ultra Bombs",
        "Ok this is a bit\nridiculous.",
        300,
    ),
    StoppedVoid(
        "Void Glue",
        "Glue the void in place,\nnever to move again.",
        500,
    ),
    GlobalMagnet(
        "Global Magnet",
        "The orbs simply cannot\nget away.",
        1000,
    ),
    RainbowPlayer(
        "Rainbow Player",
        "Why not?",
        2000,
        toggleable = true,
    ),
    ParticleBlackHole(
        "Black Hole",
        "Everything is attracted\nto you.",
        10000,
        toggleable = true,
    );

    val obfuscatedTitle = title.replace(Regex("[A-Za-z]")) { "?" }
    val obfuscatedDescription = description.replace(Regex("[A-Za-z]")) { "?" }
}