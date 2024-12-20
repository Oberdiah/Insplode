package com.oberdiah.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.Point
import com.oberdiah.Rect
import com.oberdiah.Renderer
import com.oberdiah.SHADOW_DIRECTION_UNITS
import com.oberdiah.ScoreSystem
import com.oberdiah.ScoreSystem.StarsAwarded
import com.oberdiah.fontMedium
import com.oberdiah.fontSmall
import com.oberdiah.player.Player
import com.oberdiah.saturate
import com.oberdiah.statefulVibrationSetting
import com.oberdiah.ui.Banner
import com.oberdiah.ui.goToDiegeticMenu
import com.oberdiah.ui.registerGameEndDiegeticMenu
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController.holdingDownPlayButton
import com.oberdiah.withAlpha

fun renderAwardedStars(
    r: Renderer,
    p: Point,
    align: Int,
    starSize: Double,
    stars: StarsAwarded,
    backgroundColor: Color = Color.BLACK.withAlpha(0.5f),
    noStarColor: Color = Color.BLACK.withAlpha(0.5f),
    mainStarColor: Color = colorScheme.starsColor,
    blueStarColor: Color = colorScheme.developerStarsColor,
    spacing: Double = starSize * 1.05,
) {
    val numStars = stars.stars
    val numBlueStars = stars.blueStars
    assert(numStars in 0..3)

    val startXPos = when (align) {
        Align.center -> -0.5 * spacing
        Align.right -> -2 * spacing
        else -> 0.0
    }

    for (starNum in 1..3) {
        // IMO it looks slightly better down just a touch.
        renderColoredStar(
            r,
            p + Point((starNum - 1) * spacing + startXPos, -starSize / 25),
            starSize,
            backgroundColor = backgroundColor,
            starColor1 = if (starNum <= numStars) {
                if (starNum <= numBlueStars) {
                    blueStarColor
                } else {
                    mainStarColor
                }
            } else {
                noStarColor
            }
        )
    }
}

/**
 * A phase between 0 and 1 is slowly growing in,
 * a phase between 1 and 2 is the second color growing in.
 */
fun renderColoredStar(
    r: Renderer,
    p: Point,
    starSize: Double,
    backgroundColor: Color = Color.BLACK.withAlpha(0.5f),
    starColor1: Color = colorScheme.starsColor,
    starColor2: Color = colorScheme.developerStarsColor,
    phase: Double = 1.0,
) {
    r.color = backgroundColor
    r.star(
        p,
        starSize / 1.8,
    )

    r.color = starColor1
    r.star(
        p,
        starSize / 2.6 * saturate(phase),
    )

    if (phase > 1) {
        r.color = starColor2
        r.star(
            p,
            starSize / 2.6 * (phase - 1),
        )
    }
}

fun renderStar(r: Renderer, center: Point, starSize: Double) {
    r.star(
        // IMO it looks slightly better down just a touch.
        center - Point(0.0, starSize / 25),
        starSize / 2,
    )
}

fun vibrate(milliseconds: Int) {
    if (statefulVibrationSetting.value) {
        Gdx.input.vibrate(milliseconds)
    }
}

fun endTheGame(grabbedJewel: Boolean = false, deathReason: Player.DeathReason) {
    goToDiegeticMenu()
    Banner.dismissBanner()
    ScoreSystem.registerGameEnd(grabbedJewel)
    registerGameEndDiegeticMenu(deathReason = deathReason)
}

fun renderButton(
    r: Renderer,
    rect: Rect,
    isHeldDown: Boolean,
    text: String,
    buttonColor: Color = colorScheme.launchButtonColor
) {
    r.color = Color.BLACK.withAlpha(0.5)
    r.rect(rect.offsetBy(SHADOW_DIRECTION_UNITS))

    val areaRect = if (isHeldDown) {
        rect.offsetBy(SHADOW_DIRECTION_UNITS)
    } else {
        rect
    }

    r.color = if (isHeldDown) {
        Color.GRAY
    } else {
        buttonColor
    }
    r.rect(areaRect)

    r.color = Color.BLACK
    r.hollowRect(areaRect, 0.1)

    r.text(
        fontMedium,
        text,
        areaRect.center(),
        Align.center
    )
}