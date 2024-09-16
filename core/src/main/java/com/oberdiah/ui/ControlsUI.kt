package com.oberdiah.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.oberdiah.ControlScheme
import com.oberdiah.HEIGHT
import com.oberdiah.JUMP_CONTROL
import com.oberdiah.JUMP_UI_FRACT
import com.oberdiah.LEFT_BUTTON_UI_FRACT
import com.oberdiah.LEFT_RIGHT_CONTROL
import com.oberdiah.RIGHT_BUTTON_UI_FRACT
import com.oberdiah.Renderer
import com.oberdiah.Screen
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.WIDTH
import com.oberdiah.fontMedium
import com.oberdiah.fontSmallish
import com.oberdiah.max
import com.oberdiah.min
import com.oberdiah.withAlpha

private enum class ControlButton {
    Left,
    Right,
    Jump
}

private var currentlyChangingControl = ControlButton.Jump

fun buttonPositionUI(r: Renderer) {
    r.text(
        fontSmallish,
        "Drag to move the button boundary\nLet go to save and quit",
        WIDTH / 2,
        SUBTITLE_HEIGHT,
        Align.center
    )

    TOUCHES_DOWN.forEach {
        when (currentlyChangingControl) {
            ControlButton.Left -> {
                LEFT_BUTTON_UI_FRACT = it.x / WIDTH
                RIGHT_BUTTON_UI_FRACT = max(LEFT_BUTTON_UI_FRACT, RIGHT_BUTTON_UI_FRACT)
            }

            ControlButton.Right -> {
                RIGHT_BUTTON_UI_FRACT = it.x / WIDTH
                LEFT_BUTTON_UI_FRACT = min(LEFT_BUTTON_UI_FRACT, RIGHT_BUTTON_UI_FRACT)
            }

            ControlButton.Jump -> JUMP_UI_FRACT = it.y / HEIGHT
        }
    }

    r.color = Color.WHITE.withAlpha(0.5)
    if (JUMP_CONTROL == ControlScheme.JumpButton) {
        r.rect(0, JUMP_UI_FRACT * HEIGHT, WIDTH, 5)
    }
    if (LEFT_RIGHT_CONTROL == ControlScheme.LeftRightTap) {
        var y = 0.0
        if (JUMP_CONTROL == ControlScheme.JumpButton) {
            y = max(y, JUMP_UI_FRACT * HEIGHT)
        }
        r.rect(WIDTH * LEFT_BUTTON_UI_FRACT, y, 5, HEIGHT)
        r.rect(WIDTH * RIGHT_BUTTON_UI_FRACT, y, 5, HEIGHT)
    }

    TOUCHES_WENT_UP.forEach { _ ->
        backAScreen()
    }
}

fun controlsUI(r: Renderer) {
    settingButton(r, "Movement", {
        r.text(fontMedium, LEFT_RIGHT_CONTROL.title, it, Align.right)
    }, {
        LEFT_RIGHT_CONTROL =
            if (LEFT_RIGHT_CONTROL == ControlScheme.LeftRightTap) ControlScheme.MoveToFinger else ControlScheme.LeftRightTap
    })

    button(r, "Change Jump Button Height", JUMP_CONTROL == ControlScheme.JumpButton) {
        currentlyChangingControl = ControlButton.Jump
        switchScreen(Screen.ChangeButtonPositions)
    }

    settingButton(r, "Pause button side", {
        r.text(fontMedium, if (PAUSE_SIDE == Align.left) "Left" else "Right", it, Align.right)
    }, {
        PAUSE_SIDE = if (PAUSE_SIDE == Align.left) {
            Align.right
        } else {
            Align.left
        }
    })

//    settingButton(r, "Jumping", {
//        r.text(fontMedium, JUMP_CONTROL.title, it, Align.right)
//    }, {
//        JUMP_CONTROL = if (JUMP_CONTROL == ControlScheme.JumpButton) ControlScheme.SwipeUp else ControlScheme.JumpButton
//    })
//
//    settingButton(r, "Use Ability", {
//        r.text(fontMedium, ABILITY_CONTROL.title, it, Align.right)
//    }, {
//        ABILITY_CONTROL = if (ABILITY_CONTROL == ControlScheme.AbilityButton) ControlScheme.TapPlayer else ControlScheme.AbilityButton
//    })
//
//    button(r, "Change Button Positions") {
//        switchScreen(Screen.ChangeButtonPositionsLanding)
//    }

    button(r, "Back") {
        backAScreen()
    }
}