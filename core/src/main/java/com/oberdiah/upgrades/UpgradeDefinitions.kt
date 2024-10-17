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
    StarterUpgrade(
        "Launch",
        "Spawn onto the map.",
        1,
        2,
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
        7,
        9
    ),
    MediumTimedBomb(
        "Timed Bomb",
        "Occasionally spawns in\na bomb.\nMine for orbs!",
        7,
        13,
    ),
    Jump(
        "Jump",
        "Swipe up to jump.",
        9,
        13
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
        20,
        37
    ),
    Mobility(
        "High Mobility",
        "Move quicker, jump higher.",
        30,
        106
    ),
    SpringBomb(
        "Spring Bomb",
        "A bomb that jumps \n around before exploding.",
        100,
        71
    ),
    Slam(
        "Slam",
        "Swipe down to slam.\nSlamming bombs makes you\nbounce and repels the void.",
        65,
        71
    ),
    LargeTimedBomb(
        "Large Bombs",
        "Bigger can only mean better.",
        150,
        295
    ),
    GoldenNuggets(
        "Gold Nuggets",
        "Occasional golden tiles\nfull of treasure.",
        90,
        71
    ),
    WiderLineBombs(
        "Wider Line",
        "Line bombs now stretch the\nentire way across the screen.",
    ),
    MegaTimedBomb(
        "Mega Bombs",
        "Massive bombs that\nwreck the landscape.",
    ),
    CombineOrbs(
        "Combine Orbs",
        "Orbs combine when they\nare close together, making\nthem easier to pick up.",
        45,
        67
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
        125,
        160
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