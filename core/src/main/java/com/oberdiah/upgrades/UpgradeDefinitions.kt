package com.oberdiah.upgrades

import com.oberdiah.ScoreSystem
import com.oberdiah.ScoreSystem.StarsAwarded
import com.oberdiah.ceil
import com.oberdiah.d

enum class Upgrade(
    val title: String,
    val description: String,
    val threeStarsScore: Int = 5,
    val threeBlueStarsScore: Int = 9999,
    val twoStarsScore: Int = ceil(threeStarsScore * 0.65),
    val oneStarScore: Int = ceil(threeStarsScore * 0.25),
    val oneBlueStarScore: Int = ceil(threeStarsScore + (threeBlueStarsScore - threeStarsScore) * 0.33),
    val twoBlueStarsScore: Int = ceil(threeStarsScore + (threeBlueStarsScore - threeStarsScore) * 0.66),
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
        9,
        10
    ),
    MediumTimedBomb(
        "Timed Bomb",
        "Occasionally spawns in\na bomb.\nMine for orbs!",
        7,
        21,
    ),
    Jump(
        "Jump",
        "Swipe up to jump.",
        9,
        29
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
        "A bomb that jumps\naround before exploding.",
        25,
        40
    ),
    Slam(
        "Slam",
        "Swipe down to slam.\nSlamming bombs makes you\nbounce and repels the void.",
        30,
        53
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
        258
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
        150,
        334
    ),


    // ################ SLAM UPGRADES ################

    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each\nsuccessful bomb slam.",
        200,
        679
    ),
    Multiplier(
        "Multiplier",
        "Gain multiplier for each\nbomb slam. Affects orb\nspawning and void pushback.",
        400,
        930
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
        1500,
        2919,
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
            if (this == BlackHole) return this.ordinal * 3 - 3

            return this.ordinal * 2
        }

    val obfuscatedTitle = title.replace(Regex("\\S")) { "?" }
    val obfuscatedDescription = description.replace(Regex("\\S")) { "?" }
    val levelText = "Level ${this.ordinal + 1}:"
    val bestText: String
        get() = "Best: ${ScoreSystem.getPlayerScore(this)}"

    val bestTextEmptyIfZero: String
        get() = if (ScoreSystem.getPlayerScore(this) == 0) "" else bestText

    fun getFractionToNextStar(yourScore: Int): Double {
        return when {
            yourScore >= threeBlueStarsScore -> 1.0
            yourScore >= twoBlueStarsScore -> (yourScore - twoBlueStarsScore) / (threeBlueStarsScore - twoBlueStarsScore).d
            yourScore >= oneBlueStarScore -> (yourScore - oneBlueStarScore) / (twoBlueStarsScore - oneBlueStarScore).d
            yourScore >= threeStarsScore -> (yourScore - threeStarsScore) / (oneBlueStarScore - threeStarsScore).d
            yourScore >= twoStarsScore -> (yourScore - twoStarsScore) / (threeStarsScore - twoStarsScore).d
            yourScore >= oneStarScore -> (yourScore - oneStarScore) / (twoStarsScore - oneStarScore).d
            else -> yourScore / oneStarScore.d
        }
    }

    fun getStarsFromScore(yourScore: Int): StarsAwarded {
        return when {
            yourScore >= threeBlueStarsScore -> StarsAwarded.ThreeBlue
            yourScore >= twoBlueStarsScore -> StarsAwarded.TwoBlue
            yourScore >= oneBlueStarScore -> StarsAwarded.OneBlue
            yourScore >= threeStarsScore -> StarsAwarded.Three
            yourScore >= twoStarsScore -> StarsAwarded.Two
            yourScore >= oneStarScore -> StarsAwarded.One
            else -> StarsAwarded.Zero
        }
    }

    fun starsToScore(stars: Int): Int {
        return when (stars) {
            6 -> threeBlueStarsScore
            5 -> twoBlueStarsScore
            4 -> oneBlueStarScore
            3 -> threeStarsScore
            2 -> twoStarsScore
            1 -> oneStarScore
            else -> 0
        }
    }
}