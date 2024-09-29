package com.oberdiah.ui

import com.badlogic.gdx.utils.Align
import com.oberdiah.Renderer
import com.oberdiah.fontMedium
import com.oberdiah.statefulPauseSide

fun controlsUI(r: Renderer) {
    settingButton(r, "Pause button side", {
        r.text(
            fontMedium,
            if (statefulPauseSide.value == Align.left) "Left" else "Right",
            it,
            Align.right
        )
    }, {
        statefulPauseSide.value = if (statefulPauseSide.value == Align.left) {
            Align.right
        } else {
            Align.left
        }
    })

    button(r, "Back") {
        backAScreen()
    }
}