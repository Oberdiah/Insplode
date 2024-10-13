package com.oberdiah.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.utils.Align
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.withAlpha

fun renderAwardedStars(r: Renderer, p: Point, align: Int, starSize: Double, stars: Int) {
    // 4 stars represents beating the developer score.
    assert(stars in 0..4)

    val spacing = starSize * 1.25
    val startXPos = when (align) {
        Align.center -> -1.5 * spacing
        Align.right -> -3 * spacing
        else -> 0.0
    }

    for (i in 0 until 3) {
        r.color = if (i <= stars) Color.GOLD else Color.BLACK.withAlpha(0.5f)

        renderStar(
            r,
            p + Point(i * spacing + startXPos, 0.0),
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