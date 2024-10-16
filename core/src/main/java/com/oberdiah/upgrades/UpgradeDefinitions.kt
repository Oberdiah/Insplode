package com.oberdiah.upgrades

const val P = 3.0
const val MAIN_UPGRADE_SIZE = 2.0

enum class Upgrade(
    val title: String,
    val description: String,
    val threeStarsScore: Int = 5,
    val developerBest: Int = 9999,
) {
    StarterUpgrade(
        "Launch",
        "Spawn onto the map.",
        1,
        1,
    ),
    Movement(
        "Movement",
        "Touch and drag left and right\nto move. The white line is\nwhere you're heading.",
        3,
        4
    ),

    // ################ EARLY-GAME UPGRADES ################
    SlowerVoid(
        "Void Repellent",
        "Slow down the void's\napproach.",
        9,
        10
    ),
    MediumTimedBomb(
        "Timed Bomb",
        "Occasionally spawns in\na bomb.\nMine for orbs!",
    ),
    Jump(
        "Jump",
        "Swipe up to jump.",
    ),
    SmallTimedBomb(
        "Small Bomb",
        "Helping out with the mining\neffort.",
    ),
    VoidLift(
        "Void Lift",
        "Haul that darkness away!\nThe void begins higher up.",
        15,
    ),
    LineBomb(
        "Line Bomb",
        "A harmless bomb that\nexplodes in a straight line.",
    ),
    LowerGravity(
        "Micro-Gravity",
        "Only applies to the player.\nLess gravity = higher jump.",
    ),
    FasterMovement(
        "Move quicker",
        "Zooooooom!",
    ),
    // ################ JUMP UPGRADES ################

    SlowerTimers(
        "Slower Timers",
        "Timers on bombs are\nslowed down.",
    ),
    CombineOrbs(
        "Combine Orbs",
        "Orbs combine when they\nare close together, making\nthem easier to pick up.",
    ),
    GoldenNuggets(
        "Gold Nuggets",
        "Occasional golden tiles\nfull of treasure.",
    ),
    SpringBomb(
        "Spring Bomb",
        "A bomb that jumps \n around before exploding.",
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
    ),
    LargeTimedBomb(
        "Large Bombs",
        "Bigger can only mean better.\nWorth more, too.",
    ),
    Slam(
        "Slam",
        "Swipe down to slam.\nSlamming bombs is harmless\nand makes you bounce.",
    ),

    // ################ SLAM UPGRADES ################

    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each\nsuccessful bomb slam.",
    ),
    ApexWings(
        "Apex Wings",
        "Hover slightly at the apex of\nyour jump",
    ),
    MegaTimedBomb(
        "Mega Bombs",
        "Massive bombs that\nwreck the landscape.",
    ),

    //    SneakySlam(
//        "Sneaky Slam",
//        "When fuse is half-depleted,\nslam to get double orbs\nand no explosion.",
//        100,
//    ),
    Multiplier(
        "Multiplier",
        "Gain multiplier for each\nbomb slam. Applies to\nthe number of orbs spawned.",
    ),
    RapidBombs(
        "Insurgence",
        "Bombs spawn more\nfrequently.\nMore bombs, more points.",
    ),
    MegaMagnet(
        "Mega Magnet",
        "Covers most of the screen.",
    ),
    InfiniteMultiplier(
        "Sky's the Limit",
        "Multiplier is no longer capped.\nAfter 2x it can decay.\nAfter 2.5x time accelerates.",
    ),
    EvenFasterMovement(
        "Lightning Fast",
        "Move ridiculously fast.",
    ),

    // ################ POST-GAME UPGRADES ################

    UltraTimedBomb(
        "Ultra Bombs",
        "Ok this is a bit\nridiculous.",
    ),
    GlobalMagnet(
        "Global Magnet",
        "The orbs simply cannot\nget away.",
    ),
    DeadlyTouch(
        "Deadly Touch",
        "Simply touching a bomb\nwill cause it to self-destruct.",
    ),
    BlackHole(
        "Black Hole",
        "Everything must be consumed,\nand so it shall be.",
    );

    val starsToUnlock: Int
        get() {
            if (this == BlackHole) return this.ordinal * 3

            return this.ordinal * 2
        }

    val obfuscatedTitle = title.replace(Regex("\\S")) { "?" }
    val obfuscatedDescription = description.replace(Regex("\\S")) { "?" }

    val twoStarsScore = (threeStarsScore * 0.65).toInt()
    val oneStarScore = (threeStarsScore * 0.25).toInt()

    fun starsToScore(stars: Int): Int {
        return when (stars) {
            3 -> threeStarsScore
            2 -> twoStarsScore
            1 -> oneStarScore
            else -> 0
        }
    }
}