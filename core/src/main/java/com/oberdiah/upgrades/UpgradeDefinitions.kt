package com.oberdiah.upgrades

import com.oberdiah.ScoreSystem
import com.oberdiah.ScoreSystem.StarsAwarded
import com.oberdiah.ceil
import com.oberdiah.d
import com.oberdiah.i
import com.oberdiah.player.Player
import kotlin.random.Random

private val genericTwoBlueStarPraises = listOf(
    "The void is no match for you,\nit cowers in fear.",
    "Even the stars above bow in respect.",
    "At this height, the air gets thin\nand the stars get bright.",
    "The universe holds its breath\nin silent awe.",
    "The child gazed into the sky in wonder.\n'Look at all those blue stars!', they said.",
    "Breaking records, one star at a time.",
    "Time pauses to witness\nyour achievement.",
    "The void itself stares back in amazement.",
)

private val genericThreeBlueStarPraises = listOf(
    "A triple-blue score is the highest\nscore achieved before release.",
    "Three blue stars was never\nreally supposed to be obtainable and\nyou've managed anyway.",
    "Legend says only shooting stars\ncan reach this high.\nYou've proved them wrong.",
    "The mythical triple-blue!\nIt actually exists!",
    "The blue trinity shimmers with power.",
    "Three blue stars dance in perfect harmony.",
    "Three blue stars shine like\nbeacons in the dark.",
    "Ancient books talk of a score\nthat grazes infinity.\nI believe you've just found it.",
)

private val genericBeyondBlueStarPraises = listOf(
    "Up here is uncharted score territory.",
    "They said it couldn't be done\nand they were all wrong.",
    "Your score has ascended beyond\nwhat we thought possible.",
    "They'll tell stories about\nthis score for generations.",
    "Somewhere, an ancient prophecy\njust came true.",
    "You've found a score\nwe thought was just a myth.",
    "Some say these heights are lonely\nbut you've made it your home.",
)

private val genericDeathMessages = listOf(
    "Another red bean bites the dust.",
    "And everything was quiet once again.",
    "You've been promoted to particles!",
    "Remember: Small red beans and bombs don't mix.",
)

private val deathByVoidMessages = listOf(
    "The void sends its regards.",
    "Void: 1, Bean: 0",
    "'Into the void' wasn't meant to be taken literally.",
    "The void claims another victim.",
)

private val deathByBombMessages = listOf(
    "Top 10 ways NOT to dodge a bomb.",
)

private val deathByLavaMessages = listOf(
    "WARNING: Lava is hot.",
    "Lava: 1, Bean: 0",
)

enum class Upgrade(
    val idealTitle: String,
    val description: String,
    val threeStarsScore: Int,
    val threeBlueStarsScore: Int,
    // These appear on zero and one-star scores.
    val levelSpecificFailureHints: List<String>,
    val twoBlueStarPraises: List<String> = genericTwoBlueStarPraises,
    val threeBlueStarPraises: List<String> = genericThreeBlueStarPraises + genericTwoBlueStarPraises,
    val beyondBlueStarPraises: List<String> = genericBeyondBlueStarPraises + genericThreeBlueStarPraises,
    val twoStarsScore: Int = ceil(threeStarsScore * 0.65),
    val oneStarScore: Int = ceil(threeStarsScore * 0.25),
    val oneBlueStarScore: Int = ceil(threeStarsScore + (threeBlueStarsScore - threeStarsScore) * 0.33),
    val twoBlueStarsScore: Int = ceil(threeStarsScore + (threeBlueStarsScore - threeStarsScore) * 0.66),
) {
    Movement(
        "Movement",
        "Touch and drag left and right\nto move. The white line is\nwhere you're heading.",
        3,
        4,
        levelSpecificFailureHints = listOf(
            "A single touch-and-drag can be\nall that's needed.",
            "The void is always moving,\nso should you.",
        ),
        threeBlueStarPraises = listOf(
            "Who'd have thought your hitbox gets\nsmaller when crouching?",
            "'Duck, duck, void'",
            "Limbo champion skills finally\npaying off.",
            "'Just stand still!'\n- The Void, probably."
        )
    ),

    // ################ EARLY-GAME UPGRADES ################
    SlowerVoid(
        "Slower Void",
        "Slow down the void's\napproach.",
        9,
        10,
        // I'm fine with there being nothing here
        levelSpecificFailureHints = listOf(),
        threeBlueStarPraises = listOf(
            "Crouching to duck the void was\nadded for this exact reason.",
            "Do you think the void gets angry\nwhen you duck it like that?",
            "The void files a complaint about\nunfair hitbox manipulation."
        )
    ),
    MediumTimedBomb(
        "Medium Bomb",
        "Occasionally spawns in\na bomb.\nMine for orbs!",
        7,
        30,
        levelSpecificFailureHints = listOf(
            "You can push bombs over the orb patches\non the ground to mine the orbs.",
            "It can be good to wait until the bombs\nhave gone in and mined out a hole\nbefore diving in.",
        ),
    ),
    Jump(
        "Jump",
        "Swipe up to jump.",
        9,
        30,
        levelSpecificFailureHints = listOf(
            "Push bombs over the orb patches on the\nground to mine the orbs.",
            "It can be good to wait until the bombs\nhave gone in and mined out a hole first.",
            "Another red bean bites the dust."
        ),
    ),

    // ################ JUMP UPGRADES ################
    SmallTimedBomb(
        "Small Bomb",
        "Helping out with the mining\neffort.",
        12,
        29,
        levelSpecificFailureHints = listOf(
            "Small bombs have a short fuse,\nso be quick to get out of the way.",
            "Push bigger bombs over the orb patches on the\nground to mine the orbs.",
            "It can be good to wait until the bombs\nhave gone in and mined out a hole first.",
            "Another red bean bites the dust."
        ),
    ),
    LineBomb(
        "Line Bomb",
        "Explodes in a straight line.\nWon't explode if pointing\nup or spinning fast.",
        15,
        52,
        levelSpecificFailureHints = listOf(
            "Line bombs are your ally.\nAngle them right and they'll do wonders.",
            "Try nudging line bombs onto a slope\nto angle them into the ground.",
            "Line bombs exploding underground\nis a fantastic way to get down quicker.",
            "Nudge line bombs into deep holes\nfor massive collapses.",
            "Green orb patches only drop 50% if\nthey disappear due to land collapse.",
        )
    ),
    Mobility(
        "High Mobility",
        "Move quicker, jump higher.",
        20,
        34,
        levelSpecificFailureHints = listOf(
            "Line bombs are your ally.\nAngle them right and they'll do wonders.",
            "Try nudging line bombs onto a slope\nto angle them into the ground.",
            "Line bombs exploding underground\nis a fantastic way to get down quicker.",
            "Nudge line bombs into deep holes\nfor massive collapses.",
            "Green orb patches only drop 50% if\nthey disappear due to land collapse.",
        )
    ),
    SpringBomb(
        "Spring Bomb",
        "A bomb that jumps\naround before exploding.",
        25,
        59,
        levelSpecificFailureHints = listOf(
            "Line bombs are your ally.\nAngle them right and they'll do wonders.",
            "Try nudging line bombs onto a slope\nto angle them into the ground.",
            "Line bombs exploding underground\nis a fantastic way to get down quicker.",
            "Nudge line bombs into deep holes\nfor massive collapses.",
            "Green orb patches only drop 50% if\nthey disappear due to land collapse.",
            "Spring bombs can be a bit unpredictable,\nso be ready to jump.",
            "The outer ring on a spring bomb tells you\nhow long it has until it jumps."
        )
    ),
    Slam(
        "Slam",
        "Swipe down to slam.\nSlamming bombs makes you\nbounce and repels the void.",
        30,
        53,
        levelSpecificFailureHints = listOf(
            "Line bombs are your ally.\nAngle them right and they'll do wonders.",
            "Line bombs exploding underground\nis a fantastic way to get down quicker.",
            "Nudge line bombs into deep holes\nfor massive collapses.",
            "Green orb patches only drop 50% if\nthey disappear due to land collapse.",
            "Slamming repels the void,\nso it's critical for longer runs",
        )
    ),
    LargeTimedBomb(
        "Large Bombs",
        "Bigger can only mean better.",
        35,
        72,
        levelSpecificFailureHints = listOf(
            "Slamming repels the void,\nso it's critical for longer runs",
            "Slamming a gray line bomb makes it\nexplode perfectly horizontally.",
            "The outer ring on a spring bomb tells you\nhow long it has until it jumps."
        )
    ),
    GoldenNuggets(
        "Gold Nuggets",
        "Occasional golden tiles\nfull of treasure.",
        50,
        193,
        levelSpecificFailureHints = listOf(
            "Slamming repels the void,\nso it's critical for longer runs",
            "Golden nuggets in the ground are worth\n50 points each.",
            "Slamming a gray line bomb makes it\nexplode perfectly horizontally."
        )
    ),
    WiderLineBombs(
        "Longer Line",
        "Line bombs now stretch the\nentire way across the screen.",
        75,
        299,
        levelSpecificFailureHints = listOf(
            "Line bombs are your ally.\nAngle them right and they'll do wonders.",
            "Try nudging line bombs to angle them down\nfor maximum ground collapse.",
            "Slamming a gray line bomb makes it\nexplode perfectly horizontally.",
            "Slamming repels the void,\nso it's critical for longer runs",
        )
    ),
    MegaTimedBomb(
        "Mega Bombs",
        "Massive bombs that\nwreck the landscape.",
        100,
        337,
        levelSpecificFailureHints = listOf(
            "You can slam while ascending to reverse\ndirection if you get too close to the void",
            "Slamming is absolutely vital at this point.\nDon't forget to use it."
        )
    ),
    Magnet(
        "Magnet",
        "Attract orbs to you.",
        150,
        474,
        levelSpecificFailureHints = listOf(
            "Slamming bombs is super important at this point.\nDon't forget to use it.",
        )
    ),


    // ################ SLAM UPGRADES ################

    SlamOrbs(
        "Slam Orbs",
        "Orbs are spawned for each\nsuccessful bomb slam.",
        200,
        897,
        levelSpecificFailureHints = listOf(
            "Larger bombs give far larger payouts on slam.",
            "Slamming bombs is super important at this point.\nDon't forget to use it.",
        )
    ),
    Multiplier(
        "Multiplier",
        "Gain multiplier for each\nbomb slam. Affects orb\nspawning and void pushback.",
        400,
        993,
        levelSpecificFailureHints = listOf(
            "Multiplier affects the number of orbs you\nget from orb patches as well.",
            "Having a high multiplier is key\ngoing forward.",
            "Delaying in the air before slamming can help\nkeep a multiplier over a bomb drought.",
            "Larger bombs give far larger payouts on slam.",
        )
    ),
    RapidBombs(
        "Insurgence",
        "Bombs spawn more\nfrequently and have\nlonger fuses.",
        500,
        1054,
        levelSpecificFailureHints = listOf(
            "Multiplier affects the number of orbs you\nget from orb patches as well.",
            "The longer fuses here really help\nwith chaining slams.",
            "Even a 1.5x multiplier can\nreally help out here.",
            "Delaying in the air before slamming can help\nkeep a multiplier over a bomb drought.",
            "Larger bombs give far larger payouts on slam.",
        )
    ),
    MegaMagnet(
        "Mega Magnet",
        "Covers most of the screen.",
        650,
        1200,
        levelSpecificFailureHints = listOf(
            "Delaying in the air before slamming can help\nkeep a multiplier over a bomb drought.",
            "Larger bombs give far larger payouts on slam.",
            "The void's acceleration doubles after 100s.",
            "Even a 1.5x multiplier can\nreally help out here.",
        )
    ),
    UltraTimedBomb(
        "Ultra Bombs",
        "Ok this is a bit\nridiculous.",
        700,
        1400,
        levelSpecificFailureHints = listOf(
            "You can slam while ascending to reverse\ndirection if you get too close to the void",
            "Larger bombs give far larger payouts on slam.",
            "Multiplier affects the number of orbs you\nget from orb patches as well.",
            "Delaying in the air before slamming can help\nkeep a multiplier over a bomb drought.",
            "Once you reach 2x or even higher\nyour score racks up quickly.",
            "A high multiplier is absolutely key at this point.",
        )
    ),
    InfiniteMultiplier(
        "Sky's the Limit",
        "Multiplier is no longer capped.\nAfter 2x it can decay.\nAfter 2.5x time accelerates.",
        800,
        2000,
        levelSpecificFailureHints = listOf(
            "Multiplier affects the number of orbs you\nget from orb patches as well.",
            "Delaying in the air before slamming can help\nkeep a multiplier over a bomb drought.",
            "Once you reach 2x or even higher\nyour score racks up quickly.",
            "A high multiplier is absolutely key at this point.",
            "You can slam while ascending to reverse\ndirection if you get too close to the void",
            "Larger bombs give far larger payouts on slam.",
            "The void's acceleration doubles after 100s.",
        )
    ),
    EvenFasterMovement(
        "Lightning Fast",
        "Move ridiculously fast.",
        1000,
        2919,
        levelSpecificFailureHints = listOf(
            "Delaying in the air before slamming can help\nkeep a multiplier over a bomb drought.",
            "Once you reach 2x or even higher\nyour score racks up quickly.",
            "A high multiplier is absolutely key at this point.",
            "You can slam while ascending to reverse\ndirection if you get too close to the void",
            "Larger bombs give far larger payouts on slam.",
            "The void's acceleration doubles after 100s.",
        )
    ),
    FinalRun(
        "???",
        "Something's glowing way,\nway down there in the\ndepths.",
        1500,
        1925,
        oneStarScore = 1,
        twoStarsScore = 1000,
        levelSpecificFailureHints = listOf(
            "Many people complain this final level\nis too hard. They're not wrong.",
            "This final level can be a bit of a\nstep up in difficulty.",
            "Once the hot rocks start in earnest\nit can be quite a challenging journey.",
            "The void's acceleration doubles after 100s.",
        )
    ),

    // ################ POST-GAME UPGRADES ################
    GlobalMagnet(
        "Global Magnet",
        "The orbs simply cannot\nget away.",
        1500,
        2919,
        levelSpecificFailureHints = listOf(
            "This is squarely in post-game content now.\nIt's bound to be a little challenging",
        )
    ),
    BlackHole(
        "Black Hole",
        "Everything must be consumed,\nand so it shall be.\nWhy would you want this?",
        250,
        858,
        levelSpecificFailureHints = listOf(
            "Even black holes themselves can\nbe blown up, apparently?",
            "This is a bit of a joke level,\ndon't take it too seriously.",
            "I'm not sure this actually made the game any easier...",
        )
    );

    val title: String
        get() {
            if (this == FinalRun && ScoreSystem.playerHasFinishedTheGame()) {
                return "The final dive"
            }

            return idealTitle
        }

    val starsToUnlock: Int
        get() {
            if (this == BlackHole) return this.ordinal * 3 - 3

            return (this.ordinal * 2.5).i
        }

    val obfuscatedTitle = title.replace(Regex("\\S")) { "?" }
    val obfuscatedDescription = description.replace(Regex("\\S")) { "?" }
    val levelText = "Level ${this.ordinal + 1}:"
    val bestText: String
        get() = "Best: ${ScoreSystem.getPlayerScore(this)}"

    val bestTextEmptyIfZero: String
        get() = if (ScoreSystem.getPlayerScore(this) == 0) "" else bestText

    fun getHintText(stars: StarsAwarded, deathReason: Player.DeathReason): String {
        if (stars.stars > 0 && this == FinalRun) {
            return "Thank you for playing!\nRainbow player is now unlocked"
        }

        var hintCollection = when (stars) {
            StarsAwarded.Zero -> levelSpecificFailureHints
            StarsAwarded.One -> levelSpecificFailureHints
            StarsAwarded.Two -> return ""
            StarsAwarded.Three -> return ""
            StarsAwarded.OneBlue -> return ""
            StarsAwarded.TwoBlue -> return twoBlueStarPraises.random()
            StarsAwarded.ThreeBlue -> return threeBlueStarPraises.random()
            StarsAwarded.Beyond -> return beyondBlueStarPraises.random()
        }

        var deathHintCollection = when (deathReason) {
            Player.DeathReason.Void -> deathByVoidMessages + genericDeathMessages
            Player.DeathReason.Bomb -> deathByBombMessages + genericDeathMessages
            Player.DeathReason.Lava -> deathByLavaMessages + genericDeathMessages
            else -> emptyList()
        }

        if (hintCollection.isEmpty()) return deathHintCollection.random()

        return if (Random.nextDouble() < 0.35) {
            deathHintCollection.random()
        } else {
            hintCollection.random()
        }
    }

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
            yourScore > threeBlueStarsScore -> StarsAwarded.Beyond
            yourScore == threeBlueStarsScore -> StarsAwarded.ThreeBlue
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