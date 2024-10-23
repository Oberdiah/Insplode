package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.level.LASER_HEIGHT
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.level.playerHasSlammed
import com.oberdiah.player.PLAYER_SIZE
import com.oberdiah.player.player
import com.oberdiah.ui.MENU_ZONE_BOTTOM_Y
import com.oberdiah.ui.currentStarFillAmount
import com.oberdiah.ui.getMainMenuStarPosition
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.GameTime.APP_TIME
import com.oberdiah.utils.GameTime.GAMEPLAY_DELTA
import com.oberdiah.utils.GameTime.updateGameSpeed
import com.oberdiah.utils.StatefulInt
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.renderAwardedStars
import kotlin.math.pow
import kotlin.random.Random

object ScoreSystem {
    enum class StarsAwarded {
        Zero,
        One,
        Two,
        Three,
        OneBlue,
        TwoBlue,
        ThreeBlue,
        Beyond;

        val blueStars: Int
            get() = when (this) {
                Zero -> 0
                One -> 0
                Two -> 0
                Three -> 0
                OneBlue -> 1
                TwoBlue -> 2
                ThreeBlue -> 3
                Beyond -> 3
            }

        val stars: Int
            get() = when (this) {
                Zero -> 0
                One -> 1
                Two -> 2
                Three -> 3
                OneBlue -> 3
                TwoBlue -> 3
                ThreeBlue -> 3
                Beyond -> 3
            }

        val id: Int
            get() = when (this) {
                Zero -> 0
                One -> 1
                Two -> 2
                Three -> 3
                OneBlue -> 4
                TwoBlue -> 5
                ThreeBlue -> 6
                Beyond -> 6
            }
    }

    var lastScore: Int? = null
        private set

    var growingScore = 0
        private set

    var bounceDecayAccumulator = 0.0
        private set

    var lastScoreCollectionTime = RUN_TIME_ELAPSED
        private set

    var growingScoreStartedOn = RUN_TIME_ELAPSED
        private set

    const val GROWING_SCORE_REFRESH_COUNTDOWN = 1.5

    private var numConsecutiveBounces = 0

    /** The near-instant delay that causes the score to come in in small bits */
    private var scoreLeftToSound = 0
    private var lastTimeScoreSounded = 0.0

    private var playerScore = 0
    private var lastScoreGivenOn = 0.0

    // In score per second
    private var scoreGivingSpeed = 0.0
    var lastUpgrade: Upgrade? = null
        private set

    const val TIME_TO_GIVE_SCORE_PER_STAR = 1.0

    private val playerHighScores = mutableMapOf<Upgrade, StatefulInt>()
    fun init() {
        Upgrade.entries.forEach {
            playerHighScores[it] = StatefulInt("${it.name} High Score", 0)
        }
    }

    fun getPlayerScore(upgrade: Upgrade): Int {
        return playerHighScores[upgrade]?.value ?: 0
    }

    fun resetScores() {
        playerHighScores.values.forEach { it.value = 0 }
    }

    private var totalNumStarsCache = 0
    fun getPlayerTotalNumStars(): Int {
        if (totalNumStarsCache == 0) {
            totalNumStarsCache = Upgrade.entries.sumOf { getNumStarsOnUpgrade(it).stars }
        }
        return totalNumStarsCache
    }

    fun playerHasFinishedTheGame(): Boolean {
        return getPlayerScore(Upgrade.FinalRun) > 0
    }

    fun getNumStarsOnUpgrade(upgrade: Upgrade): StarsAwarded {
        return upgrade.getStarsFromScore(playerHighScores[upgrade]!!.value)
    }

    fun getCurrentMultiplier(): Double {
        if (!UpgradeController.playerHas(Upgrade.InfiniteMultiplier)) {
            return min(1 + (numConsecutiveBounces * 0.1), 2.0)
        }

        return if (numConsecutiveBounces < 15) {
            1 + (numConsecutiveBounces * 0.1)
        } else {
            1 + numConsecutiveBounces * 0.1 + ((numConsecutiveBounces - 15.0) * 0.1).pow(1.5)
        }
    }

    /** The amount we multiply time by to speed up the game */
    fun timeWarp(): Double {
        if (!UpgradeController.playerHas(Upgrade.InfiniteMultiplier)) {
            return 1.0
        }

        return 1 + max((numConsecutiveBounces - 15) * 0.05, 0)
    }

    /** Per game-second. */
    fun bounceDecayRate(): Double {
        return clamp((numConsecutiveBounces - 10) * 0.1, 0.0, 0.5)
    }

    fun canStartGame(): Boolean {
        // All score has become coins
        return playerScore == 0
    }

    fun registerBombSlam(bomb: Bomb) {
        if (UpgradeController.playerHas(Upgrade.SlamOrbs)) {
            val numToNormallySpawn = bomb.getPointsWorth()
            val numToActuallySpawn = (numToNormallySpawn * getCurrentMultiplier()).i
            PointOrbs.spawnOrbs(bomb.body.p, numToActuallySpawn, ensureEmptySpaceOnSpawn = false)
        }

        updateGameSpeed(timeWarp())
        bounceDecayAccumulator = 0.0

        if (UpgradeController.playerHas(Upgrade.Multiplier)) {
            numConsecutiveBounces++
            playMultiplierSound(numConsecutiveBounces)
        } else {
            playMultiplierSound(1)
        }

        playerHasSlammed(getCurrentMultiplier())
    }

    fun registerTileDestroyed(tile: Tile, reason: Tile.DematerializeReason) {
        if (reason == Tile.DematerializeReason.Laser) {
            return
        }

        var numToSpawn = when (tile.getTileType()) {
            TileType.OrbTile -> 1
            TileType.GoldenOrbTile -> 50
            else -> 0
        }

        if (reason == Tile.DematerializeReason.Collapse) {
            if (numToSpawn == 1) {
                if ((tile.getId().id + tile.y) % 2 == 0) {
                    return
                }
            }
            // Specifically don't want to halve golden orbs.
        }

        PointOrbs.spawnOrbs(
            tile.coord,
            (numToSpawn * getCurrentMultiplier()).i,
            canBePickedUpInstantly = true
        )
    }

    fun isFinalRun(): Boolean {
        return currentlyPlayingUpgrade.value == Upgrade.FinalRun
    }

    fun registerGameEnd(grabbedJewel: Boolean = false) {
        if (isFinalRun()) {
            if (!grabbedJewel) {
                playerScore = 0
            }
        }

        playerHighScores[currentlyPlayingUpgrade.value]!!.value =
            max(playerScore, playerHighScores[currentlyPlayingUpgrade.value]!!.value)
        lastScore = playerScore
        lastUpgrade = currentlyPlayingUpgrade.value
        lastScoreGivenOn = APP_TIME

        val timeToGiveScore =
            TIME_TO_GIVE_SCORE_PER_STAR * (currentlyPlayingUpgrade.value.getStarsFromScore(
                playerScore
            ).id + 1)
        scoreGivingSpeed = max(playerScore / timeToGiveScore, 4.0)
        growingScore = 0
        totalNumStarsCache = 0 // Force a regeneration of the cache
        numConsecutiveBounces = 0
        lastScoreCollectionTime = RUN_TIME_ELAPSED
        growingScoreStartedOn = RUN_TIME_ELAPSED
        scoreLeftToSound = 0
        lastTimeScoreSounded = 0.0

        for (i in 0..2) {
            currentStarFillAmount[i] = 0.0
        }
    }

    fun registerGameStart() {
        // Nothing at the moment
    }

    fun registerCasuallyLanded() {
        if (numConsecutiveBounces > 0) {
            playMultiplierLossSound()
        }
        bounceDecayAccumulator = 0.0
        numConsecutiveBounces = 0

        updateGameSpeed(1.0)
    }

    fun givePlayerScore(score: Int) {
        scoreLeftToSound += score
        lastTimeScoreSounded = 0.0
    }

    /**
     * We use this internally to actually give the player the score.
     * The public interface is different because we want to delay score-giving over time.
     */
    private fun givePlayerScoreInternal(score: Int) {
        playPickupSound(growingScore)
        if (growingScore == 0) {
            growingScoreStartedOn = RUN_TIME_ELAPSED
        }
        growingScore += score
        playerScore += score
        lastScoreCollectionTime = RUN_TIME_ELAPSED
    }

    fun isScoreGivingAnimationPlaying(): Boolean {
        return delayedReceivedScores.isNotEmpty() || playerScore > 0
    }

    private val delayedReceivedScores = mutableListOf<Pair<Double, Int>>()

    fun tick() {
        bounceDecayAccumulator += bounceDecayRate() * GAMEPLAY_DELTA

        while (bounceDecayAccumulator >= 1) {
            bounceDecayAccumulator -= 1
            numConsecutiveBounces = max(numConsecutiveBounces - 1, 0)
            updateGameSpeed(timeWarp())
        }

        if (GAME_STATE == GameState.DiegeticMenu) {
            val delayBetweenScoreGiving = 1 / scoreGivingSpeed
            var scoreToMove = 1
            if (delayBetweenScoreGiving < GameTime.GRAPHICS_DELTA) {
                scoreToMove = min(
                    (GameTime.GRAPHICS_DELTA / delayBetweenScoreGiving).i,
                    playerScore
                )
            }
            if (playerScore > 0 && lastScoreGivenOn + delayBetweenScoreGiving < APP_TIME) {
                playerScore -= scoreToMove
                lastScoreGivenOn = APP_TIME
                val smokeSpawnPoint = Rect.centered(
                    Point(SCREEN_WIDTH_IN_UNITS / 2, endOfGameCoinsHeight),
                    Size(SCREEN_HEIGHT_IN_UNITS / 30, SCREEN_HEIGHT_IN_UNITS / 30)
                ).randomPointInside()

                val scoreGivenOutSoFar = (lastScore ?: 0) - playerScore

                val upgrade = lastUpgrade!!

                val starsAwarded = upgrade.getStarsFromScore(scoreGivenOutSoFar - 1)
                if (starsAwarded.blueStars != 3) {
                    val headingTo = getMainMenuStarPosition(starsAwarded.id % 3 + 1)
                    val velocity = headingTo - smokeSpawnPoint
                    velocity.len = 7.5

                    val color =
                        if (starsAwarded.stars == 3) colorScheme.developerStarsColor else colorScheme.starsColor
                    spawnSmoke(
                        smokeSpawnPoint,
                        velocity,
                        color = color.cpy().mul(Random.nextDouble(0.8, 1.2).f),
                        canCollide = false,
                        radiusScaling = 2.3,
                        gravityScaling = 0.0,
                        pulledTowards = headingTo
                    )
                    delayedReceivedScores.add(APP_TIME to scoreGivenOutSoFar)
                } else {
                    val velocity = createRandomFacingPoint() * 15.0

                    spawnSmoke(
                        smokeSpawnPoint,
                        velocity,
                        color = colorScheme.developerStarsColor.cpy()
                            .mul(Random.nextDouble(0.8, 1.2).f),
                        canCollide = false,
                        radiusScaling = 2.3,
                        gravityScaling = 0.0,
                    )
                }
            }


            val lastUpgrade = lastUpgrade
            val lastScore = lastScore

            if (lastUpgrade != null && lastScore != null) {
                if (delayedReceivedScores.isNotEmpty() && delayedReceivedScores[0].first < APP_TIME - 0.5) {
                    val (_, scoreGivenOutSoFar) = delayedReceivedScores.removeAt(0)

                    val starsAwarded = lastUpgrade.getStarsFromScore(scoreGivenOutSoFar)
                    val starFraction = lastUpgrade.getFractionToNextStar(scoreGivenOutSoFar)

                    val previousStarId = max((starsAwarded.id - 1) % 3, 0)
                    val thisStarId = starsAwarded.id % 3
                    val previousStarFractionToAssign = if (starsAwarded.blueStars > 0) 2.0 else 1.0

                    if (currentStarFillAmount[previousStarId] != previousStarFractionToAssign && previousStarId != thisStarId) {
                        playMultiplierSound(starsAwarded.id + 1)
                    }

                    currentStarFillAmount[previousStarId] = previousStarFractionToAssign
                    currentStarFillAmount[thisStarId] =
                        if (starsAwarded.stars == 3) starFraction + 1.0 else starFraction
                }

                if (delayedReceivedScores.isEmpty()) {
                    // Hacky bit of code to force all stars to blue if we're on one of the upgrades
                    // that have duplicate blue-star scores.
                    if (lastUpgrade.threeBlueStarsScore <= lastScore) {
                        for (i in 0..2) {
                            currentStarFillAmount[i] = 2.0
                        }
                    }
                    for (i in 0..2) {
                        if (currentStarFillAmount[i] % 1.0 != 0.0) {
                            val goal = currentStarFillAmount[i].i.d
                            currentStarFillAmount[i] = lerp(
                                currentStarFillAmount[i],
                                goal,
                                0.1
                            )
                        }
                    }
                }
            }
        }

        if (scoreLeftToSound > 0) {
            val timeToNext = Random.nextDouble(0.1, 0.12) * 0.98.pow(scoreLeftToSound)
            if (lastTimeScoreSounded + timeToNext < RUN_TIME_ELAPSED) {
                val scoreToMove = 1
                givePlayerScoreInternal(scoreToMove)
                scoreLeftToSound -= scoreToMove
                lastTimeScoreSounded = RUN_TIME_ELAPSED
            }
        }

        if (RUN_TIME_ELAPSED - lastScoreCollectionTime > GROWING_SCORE_REFRESH_COUNTDOWN) {
            growingScore = 0
        }
    }

    val endOfGameCoinsHeight
        get() = MENU_ZONE_BOTTOM_Y + SCREEN_HEIGHT_IN_UNITS * 0.47

    fun renderDiegeticText(r: Renderer) {
        val H = SCREEN_HEIGHT_IN_UNITS
        val W = SCREEN_WIDTH_IN_UNITS.d

        r.color = colorScheme.textColor

        val textStartHeight = HEIGHT * 0.925

        val secondsTimerPos = Point(WIDTH / 2, textStartHeight)
        val scorePos = Point(WIDTH / 2, textStartHeight - HEIGHT * 0.05)


        // Seconds counter
        val alpha = saturate(-CAMERA_POS_Y * 0.5)
        r.color = colorScheme.textColor.withAlpha(alpha)
        if (secondsTimerPos.wo.y > LASER_HEIGHT) {
            r.color = Color.WHITE.withAlpha(alpha)
        }
        r.text(
            fontSmallish,
            "${RUN_TIME_ELAPSED.format(1)}s",
            secondsTimerPos,
            Align.center,
            shouldCache = false
        )

//        r.text(
//            fontSmallish,
//            "${player.body.p.y.format(1)}m",
//            secondsTimerPos - Point(0.0, HEIGHT * 0.1),
//            Align.center,
//            shouldCache = false
//        )

        if (RUN_TIME_ELAPSED > 0 && currentlyPlayingUpgrade.value != Upgrade.FinalRun) {
            val starSize = fontSmallish.capHeight * 1.25

            val transitionIfQuit = if (GAME_STATE == GameState.TransitioningToDiegeticMenu) {
                saturate((APP_TIME - LAST_APP_TIME_GAME_STATE_CHANGED) * 5.0)
            } else {
                0.0
            }

            val transition =
                easeInOutSine(saturate(RUN_TIME_ELAPSED * 2.0) - saturate(player.state.timeSinceDied * 2.0) - transitionIfQuit)
            val offset = (transition * 2.0 - 2.0) * starSize
            renderAwardedStars(
                r,
                Point(WIDTH / 15, HEIGHT - WIDTH / 15 - offset),
                Align.left,
                starSize,
                currentlyPlayingUpgrade.value.getStarsFromScore(playerScore)
            )
        }


        if (playerScore > 0) {
            r.color = colorScheme.textColor

            val worldSpaceCoords = Point(
                W / 2,
                min(endOfGameCoinsHeight, scorePos.wo.y)
            )

            if (worldSpaceCoords.y > LASER_HEIGHT) {
                r.color = Color.WHITE
            }

            r.text(
                fontLarge,
                "$playerScore",
                worldSpaceCoords.ui,
                Align.center,
                shouldCache = false
            )
        }
    }

    private const val FADE_IN_TIME = 0.05
    private const val FADE_OUT_TIME = 0.3
    private const val HEIGHT_ABOVE_HEAD = 0.5

    fun renderFloatingPlayerText(r: Renderer) {
        if (player.state.isDead) return

        val lineHeight = fontSmall.lineHeight / UNIT_SIZE_IN_PIXELS

        if (getCurrentMultiplier() > 1.0 && UpgradeController.playerHas(Upgrade.Multiplier)) {
            val textOffset = Point(
                0,
                PLAYER_SIZE.h + lineHeight * HEIGHT_ABOVE_HEAD
            )

            r.color = colorScheme.textColor

            r.text(
                fontSmall,
                "x${getCurrentMultiplier().format(1)}",
                player.body.p + textOffset,
                Align.center,
                shouldCache = false
            )
        }

        if (growingScore > 0) {
            val fadeInAlpha = (RUN_TIME_ELAPSED - growingScoreStartedOn) / FADE_IN_TIME
            val fadeOutAlpha =
                ((lastScoreCollectionTime + GROWING_SCORE_REFRESH_COUNTDOWN) - RUN_TIME_ELAPSED) / FADE_OUT_TIME
            val alpha = saturate(min(fadeInAlpha, fadeOutAlpha))

            val pickupMotion =
                1 - saturate((RUN_TIME_ELAPSED - lastScoreCollectionTime) / FADE_OUT_TIME)

            val textOffset = Point(
                0,
                PLAYER_SIZE.h +
                        pickupMotion * 0.15 +
                        lineHeight * if (UpgradeController.playerHas(Upgrade.Multiplier)) {
                    HEIGHT_ABOVE_HEAD + 1.0
                } else {
                    HEIGHT_ABOVE_HEAD
                }

            )

            r.color = colorScheme.textColor.withAlpha(alpha)
            r.text(
                fontSmall,
                "+$growingScore",
                player.body.p + textOffset,
                Align.center,
                shouldCache = false
            )
        }
    }
}