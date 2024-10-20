package com.oberdiah.upgrades

import com.oberdiah.ceil

enum class Upgrade(
    val title: String,
    val description: String,
    val threeStarsScore: Int = 5,
    val developerBest: Int = 9999,
    val twoStarsScore: Int = ceil(threeStarsScore * 0.65),
    val oneStarScore: Int = ceil(threeStarsScore * 0.25),
) {
    Movement(
        "Movement",
        "Touch and drag left and right\nto move. The white line is\nwhere you're heading.",
        3,
        4
    ),

    // ################ EARLY-GAME UPGRADES ################
    SlowerVoid(
        "Slower Void",
        "Slow down the void's\napproach.",
        7,
        9
    ),
    MediumTimedBomb(
        "Timed Bomb",
        "Occasionally spawns in\na bomb.\nMine for orbs!",
        7,
        18,
    ),
    Jump(
        "Jump",
        "Swipe up to jump.",
        9,
        27
    ),

    // ################ JUMP UPGRADES ################
    SmallTimedBomb(
        "Small Bomb",
        "Helping out with the mining\neffort.",
        12,
        19
    ),
    LineBomb(
        "Line Bomb",
        "Explodes in a straight line.\nWon't explode if pointing\nup or spinning fast.",
        15,
        37
    ),
    Mobility(
        "High Mobility",
        "Move quicker, jump higher.",
        20,
        34
    ),
    SpringBomb(
        "Spring Bomb",
        "A bomb that jumps \n around before exploding.",
        25,
        40
    ),
    Slam(
        "Slam",
        "Swipe down to slam.\nSlamming bombs makes you\nbounce and repels the void.",
        30,
        43
    ),
    LargeTimedBomb(
        "Large Bombs",
        "Bigger can only mean better.",
        35,
        72
    ),
    GoldenNuggets(
        "Gold Nuggets",
        "Occasional golden tiles\nfull of treasure.",
        50,
        110
    ),
    WiderLineBombs(
        "Longer Line",
        "Line bombs now stretch the\nentire way across the screen.",
        75,
        214
    ),
    MegaTimedBomb(
        "Mega Bombs",
        "Massive bombs that\nwreck the landscape.",
        100,
        218
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
        150,
        240
    ),


    // ################ SLAM UPGRADES ################

    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each\nsuccessful bomb slam.",
        200,
        497
    ),
    Multiplier(
        "Multiplier",
        "Gain multiplier for each\nbomb slam. Affects orb\nspawning and void pushback.",
        400,
        926
    ),
    RapidBombs(
        "Insurgence",
        "Bombs spawn more\nfrequently and have\nlonger fuses.",
        500,
        1054
    ),
    MegaMagnet(
        "Mega Magnet",
        "Covers most of the screen.",
        650,
        1200
    ),
    UltraTimedBomb(
        "Ultra Bombs",
        "Ok this is a bit\nridiculous.",
        700,
        1400
    ),
    InfiniteMultiplier(
        "Sky's the Limit",
        "Multiplier is no longer capped.\nAfter 2x it can decay.\nAfter 2.5x time accelerates.",
        800,
        2000
    ),
    EvenFasterMovement(
        "Lightning Fast",
        "Move ridiculously fast.",
        1000,
        2919
    ),
    FinalRun(
        "???",
        "Something's glowing way,\nway down there in the\ndepths.",
        2000,
        1925,
        oneStarScore = 1,
        twoStarsScore = 1000,
    ),

    // ################ POST-GAME UPGRADES ################
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

    fun starsToScore(stars: Int): Int {
        return when (stars) {
            4 -> developerBest
            3 -> threeStarsScore
            2 -> twoStarsScore
            1 -> oneStarScore
            else -> 0
        }
    }
}