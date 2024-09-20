package com.oberdiah.ui

import com.badlogic.gdx.utils.Align
import com.oberdiah.Renderer
import com.oberdiah.fontMedium
import com.oberdiah.statefulSwipeToSlam

fun controlsUI(r: Renderer) {
    toggleButton(r, "Swipe to slam", statefulSwipeToSlam::value)

    settingButton(r, "Pause button side", {
        r.text(fontMedium, if (PAUSE_SIDE == Align.left) "Left" else "Right", it, Align.right)
    }, {
        PAUSE_SIDE = if (PAUSE_SIDE == Align.left) {
            Align.right
        } else {
            Align.left
        }
    })

    button(r, "Back") {
        backAScreen()
    }
}