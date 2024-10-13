package com.oberdiah.upgrades

const val P = 3.0
const val MAIN_UPGRADE_SIZE = 2.0

enum class Upgrade(
    val title: String,
    val description: String,
    val starsToUnlock: Int,
    val threeStarsScore: Int = 100000,
    val developerBest: Int = 999999,
) {
    StarterUpgrade(
        "Launch",
        "Spawn onto the map.",
        0, // You start with this
        1,
        1,
    ),
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
        15,
    ),
    LowerGravity(
        "Micro-Gravity",
        "Only applies to the player.\nLess gravity = higher jump.",
        30,
    ),
    ComboOrbs(
        "Combo Orbs",
        "When a bomb explodes\nanother, orbs are spawned.",
        30,
    ),
    FasterMovement(
        "Move quicker",
        "Zooooooom!",
        45,
    ),
    // ################ JUMP UPGRADES ################

    SlowerTimers(
        "Slower Timers",
        "Timers on bombs are\nslowed down.",
        50,
    ),
    CombineOrbs(
        "Combine Orbs",
        "Orbs combine when they\nare close together, making\nthem easier to pick up.",
        30,
    ),
    GoldenNuggets(
        "Gold Nuggets",
        "Occasional golden tiles\nfull of treasure.",
        60,
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
    LargeTimedBomb(
        "Large Bombs",
        "Bigger can only mean better.\nWorth more, too.",
        75,
    ),
    Slam(
        "Slam",
        "Release finger while in the air\nto slam downwards. Slamming\nbombs makes you bounce.",
        75,
    ),

    // ################ SLAM UPGRADES ################

    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each\nsuccessful bomb slam.",
        100,
    ),
    ApexWings(
        "Apex Wings",
        "Hover slightly at the apex of\nyour jump",
        200,
    ),
    MegaTimedBomb(
        "Mega Bombs",
        "Massive bombs that\nwreck the landscape.",
        75,
    ),

    //    SneakySlam(
//        "Sneaky Slam",
//        "When fuse is half-depleted,\nslam to get double orbs\nand no explosion.",
//        100,
//    ),
    Multiplier(
        "Multiplier",
        "Gain multiplier for each\nbomb slam. Applies to\nthe number of orbs spawned.",
        200,
    ),
    RapidBombs(
        "Insurgence",
        "Bombs spawn more\nfrequently.\nMore bombs, more points.",
        150,
    ),
    MegaMagnet(
        "Mega Magnet",
        "Covers most of the screen.",
        300,
    ),
    InfiniteMultiplier(
        "Sky's the Limit",
        "Multiplier is no longer capped.\nAfter 2x it can decay.\nAfter 2.5x time accelerates.",
        400,
    ),
    EvenFasterMovement(
        "Lightning Fast",
        "Move ridiculously fast.",
        500,
    ),

    // ################ POST-GAME UPGRADES ################

    UltraTimedBomb(
        "Ultra Bombs",
        "Ok this is a bit\nridiculous.",
        600,
    ),
    StoppedVoid(
        "Void Glue",
        "Glue the void in place,\nnever to move again.",
        1000,
    ),
    GlobalMagnet(
        "Global Magnet",
        "The orbs simply cannot\nget away.",
        2000,
    ),
    RainbowPlayer(
        "Rainbow Player",
        "Why not?",
        5000,
    ),
    DeadlyTouch(
        "Deadly Touch",
        "Simply touching a bomb\nwill cause it to self-destruct.",
        10000,
    ),
    BlackHole(
        "Black Hole",
        "Everything must be consumed,\nand so it shall be.",
        25000,
    );

    val obfuscatedTitle = title.replace(Regex("\\S")) { "?" }
    val obfuscatedDescription = description.replace(Regex("\\S")) { "?" }
}