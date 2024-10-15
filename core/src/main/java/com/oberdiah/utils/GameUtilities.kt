package com.oberdiah.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.ScoreSystem.StarsAwarded
import com.oberdiah.withAlpha

fun renderAwardedStars(r: Renderer, p: Point, align: Int, starSize: Double, stars: StarsAwarded) {
    val numStars = stars.stars
    assert(numStars in 0..3)

    val spacing = starSize * 1.05
    val startXPos = when (align) {
        Align.center -> -0.5 * spacing
        Align.right -> -2 * spacing
        else -> 0.0
    }

    for (starNum in 1..3) {
        r.color = if (starNum <= numStars) {
            if (stars.developerBest) colorScheme.developerStarsColor else colorScheme.starsColor
        } else Color.BLACK.withAlpha(0.5f)

        renderStar(
            r,
            p + Point((starNum - 1) * spacing + startXPos, 0.0),
            starSize
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