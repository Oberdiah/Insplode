package com.oberdiah.utils

import com.badlogic.gdx.graphics.OrthographicCamera
import com.oberdiah.*
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.player
import com.oberdiah.ui.MENU_ZONE_BOTTOM_Y
import kotlin.math.pow

private var cameraY = 0.0
var camera = OrthographicCamera()
var screenCamera = OrthographicCamera()

enum class CameraFollowing {
    Player,
    MenuStartPos,
    Nothing,
}

var CAMERA_FOLLOWING = CameraFollowing.Nothing
    private set

fun startCameraToDiegeticMenuTransition() {
    CAMERA_FOLLOWING = CameraFollowing.MenuStartPos
}

private fun cameraHasReachedFinalDiegeticMenuPosition() {
    CAMERA_FOLLOWING = CameraFollowing.Nothing
    GAME_STATE = GameState.DiegeticMenu
    restartGame()
}

fun resetCamera() {
    CAMERA_FOLLOWING = CameraFollowing.Nothing
    screenCamera.setToOrtho(false, WIDTH.f, HEIGHT.f)
    camera.setToOrtho(false, UNITS_WIDE.f, SCREEN_HEIGHT_IN_UNITS.f)
    camera.position.y = getCameraYForMenu()
    cameraY = camera.position.y.d
    camera.update()
    LOWEST_CAMERA_Y_THIS_RUN = 0.0
}

private fun getCameraYForPlayerFollow(): Float {
    return (player.body.p.y + SCREEN_HEIGHT_IN_UNITS / 2 - SCREEN_HEIGHT_IN_UNITS * CURRENT_PLAYER_Y_FRACT).f
}

fun getCameraYForMenu(): Float {
    return (MENU_ZONE_BOTTOM_Y + SCREEN_HEIGHT_IN_UNITS / 2).f
}

var LOWEST_CAMERA_Y_THIS_RUN = 0.0
    private set

val TRANSITION_TO_LOWER_CAMERA_HEIGHT
    get() = LOWEST_CAMERA_Y_THIS_RUN + SCREEN_HEIGHT_IN_UNITS * 0.75

const val LOWER_CAMERA_HEIGHT_TRANSITION_RANGE = 12

/** Set the camera Y (Bottom of the camera) in world coordinates */
fun setCameraY(y: Double) {
    cameraY = y + SCREEN_HEIGHT_IN_UNITS / 2
}

fun updateCamera() {
    if (getCameraYForPlayerFollow() < camera.position.y && GAME_STATE == GameState.InGame) {
        CAMERA_FOLLOWING = CameraFollowing.Player
    }

    LOWEST_CAMERA_Y_THIS_RUN = min(LOWEST_CAMERA_Y_THIS_RUN, CAMERA_POS_Y)

    SCREEN_SHAKE -= 12 * GameTime.GAMEPLAY_DELTA
    SCREEN_SHAKE = max(SCREEN_SHAKE, 0)

    val diff = player.body.p.y - TRANSITION_TO_LOWER_CAMERA_HEIGHT
    val transitionAmount = diff / LOWER_CAMERA_HEIGHT_TRANSITION_RANGE

    CURRENT_PLAYER_Y_FRACT =
        lerp(
            BASE_PLAYER_Y_FRACT,
            ELEVATED_PLAYER_Y_FRACT,
            easeInOutSine(saturate(transitionAmount))
        )

    when (CAMERA_FOLLOWING) {
        CameraFollowing.Player -> {
            val desiredCameraPos = getCameraYForPlayerFollow()
            cameraY += (desiredCameraPos - camera.position.y) * 0.1
        }

        CameraFollowing.MenuStartPos -> {
            val desiredCameraPos = getCameraYForMenu()
            val cameraDistToMove = (desiredCameraPos - camera.position.y) * 0.1
            cameraY += if (cameraDistToMove > 0) {
                clamp(cameraDistToMove, 0.05, 1.5)
            } else {
                clamp(cameraDistToMove, -1.5, -0.05)
            }
            if (abs(camera.position.y - desiredCameraPos) < 0.05) {
                cameraHasReachedFinalDiegeticMenuPosition()
            }
        }

        CameraFollowing.Nothing -> {
            // Do nothing
        }
    }

    var shake = 0.0
    if (SCREEN_SHAKE > 0) {
        shake = getShake(SCREEN_SHAKE.pow(2))
    }
    camera.position.y = cameraY.f + shake.f

    camera.position.x = UNITS_WIDE.f / 2
    camera.update()
}

enum class ScreenShakeSettings(val text: String, val shakeAmount: Number) {
    Off("Off", 0.0),
    Low("Low", 1.0),
    Normal("Normal", 2.0),
    Extreme("Extreme", 5.0),
}

private var SCREEN_SHAKE = 0.0
fun addScreenShake(amount: Number) {
    SCREEN_SHAKE = max(SCREEN_SHAKE, amount.d * statefulScreenShakeSetting.value.shakeAmount)
}
