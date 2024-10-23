package com.oberdiah.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.ScoreSystem.StarsAwarded
import com.oberdiah.saturate
import com.oberdiah.statefulVibrationSetting
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