package com.oberdiah.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.HEIGHT
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.Size
import com.oberdiah.WIDTH
import com.oberdiah.fontSmallish
import com.oberdiah.saturate
import com.oberdiah.utils.GameTime
import com.oberdiah.withAlpha

object Banner {
    private const val ANIMATION_TIME = 0.5
    private const val TOTAL_TAP_WARNING_TIME = 5.0
    private var lastTapWarningTime = -Double.MAX_VALUE
    private var currentBannerText = ""
    private var keepBannerAlive = false

    fun showBanner(bannerText: String, keepAliveUntilDismissed: Boolean = false) {
        if (lastTapWarningTime < GameTime.APP_TIME - TOTAL_TAP_WARNING_TIME || bannerText != currentBannerText) {
            lastTapWarningTime = GameTime.APP_TIME
            currentBannerText = bannerText
            keepBannerAlive = keepAliveUntilDismissed
        }
    }

    fun dismissBanner() {
        keepBannerAlive = false
    }

    fun isBannerShowing(): Boolean {
        return lastTapWarningTime > GameTime.APP_TIME - TOTAL_TAP_WARNING_TIME
    }

    fun renderBanners(r: Renderer) {
        if (keepBannerAlive && lastTapWarningTime < GameTime.APP_TIME - ANIMATION_TIME) {
            // Pull the animation right to the end of the banner period.
            lastTapWarningTime = GameTime.APP_TIME - TOTAL_TAP_WARNING_TIME + ANIMATION_TIME
        }

        val timeSinceTapWarning = GameTime.APP_TIME - lastTapWarningTime

        val heightOffOfTop = HEIGHT / 15

        val heightToMove = heightOffOfTop * 2

        val normalHeight = HEIGHT + heightOffOfTop

        if (timeSinceTapWarning < TOTAL_TAP_WARNING_TIME) {
            val tapWarningY = if (timeSinceTapWarning < ANIMATION_TIME) {
                normalHeight - saturate(timeSinceTapWarning / ANIMATION_TIME) * heightToMove
            } else if (timeSinceTapWarning < TOTAL_TAP_WARNING_TIME - ANIMATION_TIME) {
                normalHeight - heightToMove
            } else {
                normalHeight - (1.0 - saturate((timeSinceTapWarning - (TOTAL_TAP_WARNING_TIME - ANIMATION_TIME)) / ANIMATION_TIME)) * heightToMove
            }

            r.color = Color.WHITE.withAlpha(0.75)
            r.centeredRect(
                Point(WIDTH / 2, tapWarningY),
                Size(WIDTH, HEIGHT / 20),
            )

            r.color = Color.BLACK
            r.text(
                fontSmallish,
                currentBannerText,
                WIDTH / 2,
                tapWarningY,
                Align.center
            )
        }
    }
}