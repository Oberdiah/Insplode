package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align

private enum class ControlButton {
    Left,
    Right,
    Jump
}

private var currentlyChangingControl = ControlButton.Jump

fun buttonPositionUI(r: Renderer) {
    r.text(fontSmallish, "Drag to move the button boundary\nLet go to save and quit", WIDTH/2, SUBTITLE_HEIGHT, Align.center)

    TOUCHES_DOWN.forEach {
        when (currentlyChangingControl) {
            ControlButton.Left -> {
                LEFT_BUTTON_FRACT = it.x/WIDTH
                RIGHT_BUTTON_FRACT = max(LEFT_BUTTON_FRACT, RIGHT_BUTTON_FRACT)
            }
            ControlButton.Right -> {
                RIGHT_BUTTON_FRACT = it.x/WIDTH
                LEFT_BUTTON_FRACT = min(LEFT_BUTTON_FRACT, RIGHT_BUTTON_FRACT)
            }
            ControlButton.Jump -> JUMP_FRACT = it.y/HEIGHT
        }
    }

    r.color = Color.WHITE.withAlpha(0.5)
    if (JUMP_CONTROL == ControlScheme.JumpButton) {
        r.rect(0, JUMP_FRACT * HEIGHT, WIDTH, 5)
    }
    if (LEFT_RIGHT_CONTROL == ControlScheme.LeftRightTap) {
        var y = 0.0
        if (JUMP_CONTROL == ControlScheme.JumpButton) {
            y = max(y, JUMP_FRACT * HEIGHT)
        }
        r.rect(WIDTH * LEFT_BUTTON_FRACT, y, 5, HEIGHT)
        r.rect(WIDTH * RIGHT_BUTTON_FRACT, y, 5, HEIGHT)
    }

    TOUCHES_WENT_UP.forEach { _ ->
        backAScreen()
    }
}

fun controlsUI(r: Renderer) {
    settingButton(r, "Movement", {
        r.text(fontMedium, LEFT_RIGHT_CONTROL.title, it, Align.right)
    }, {
        LEFT_RIGHT_CONTROL = if (LEFT_RIGHT_CONTROL == ControlScheme.LeftRightTap) ControlScheme.MoveToFinger else ControlScheme.LeftRightTap
    })

    button(r, "Change Left Button Edge", LEFT_RIGHT_CONTROL == ControlScheme.LeftRightTap) {
        currentlyChangingControl = ControlButton.Left
        switchScreen(Screen.ChangeButtonPositions)
    }
    button(r, "Change Right Button Edge", LEFT_RIGHT_CONTROL == ControlScheme.LeftRightTap) {
        currentlyChangingControl = ControlButton.Right
        switchScreen(Screen.ChangeButtonPositions)
    }

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