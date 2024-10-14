package com.oberdiah.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.withAlpha

fun renderAwardedStars(r: Renderer, p: Point, align: Int, starSize: Double, stars: Int) {
    // 4 stars represents beating the developer score.
    assert(stars in 0..4)

    val spacing = starSize * 1.05
    val startXPos = when (align) {
        Align.center -> -0.5 * spacing
        Align.right -> -2 * spacing
        else -> 0.0
    }

    for (starNum in 1..3) {
        r.color = if (starNum <= stars) Color.GOLD else Color.BLACK.withAlpha(0.5f)

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