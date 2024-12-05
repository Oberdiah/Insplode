package com.oberdiah.utils

import com.badlogic.gdx.graphics.OrthographicCamera
import com.oberdiah.*
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.player
import com.oberdiah.ui.MENU_ZONE_BOTTOM_Y
import com.oberdiah.ui.UPGRADES_SCREEN_BOTTOM_Y
import com.oberdiah.ui.diegeticCameraY
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
    if (CAMERA_POS_Y > MENU_ZONE_BOTTOM_Y) {
        diegeticCameraY = CAMERA_POS_Y
        endGame()
    } else {
        CAMERA_FOLLOWING = CameraFollowing.MenuStartPos
    }
}

fun initCamera() {
    screenCamera.setToOrtho(false, WIDTH.f, HEIGHT.f)
    camera.setToOrtho(false, UNITS_WIDE.f, SCREEN_HEIGHT_IN_UNITS.f)
    camera.position.y = getCameraYForMenu()
    cameraY = camera.position.y.d
    camera.update()
}

fun resetCamera() {
    CAMERA_FOLLOWING = CameraFollowing.Nothing
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
    get() = LOWEST_CAMERA_Y_THIS_RUN + SCREEN_HEIGHT_IN_UNITS * 0.5

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
    SCREEN_SHAKE = max(SCREEN_SHAKE, 0.0)

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
            cameraY = frameAccurateLerp(cameraY, desiredCameraPos.d, 10.0)
        }

        CameraFollowing.MenuStartPos -> {
            val desiredCameraPos = getCameraYForMenu()
            cameraY = frameAccurateLerp(cameraY, desiredCameraPos.d, 10.0)
            if (abs(camera.position.y - desiredCameraPos) < 0.05) {
                diegeticCameraY = CAMERA_POS_Y
                endGame()
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

enum class ScreenShakeSettings(val text: String, val shakeAmount: Double) {
    Off("Off", 0.0),
    Low("Low", 1.0),
    Normal("Normal", 2.0),
}

private var SCREEN_SHAKE = 0.0
fun addScreenShake(amount: Double) {
    SCREEN_SHAKE = max(SCREEN_SHAKE, amount * statefulScreenShakeSetting.value.shakeAmount)
}
