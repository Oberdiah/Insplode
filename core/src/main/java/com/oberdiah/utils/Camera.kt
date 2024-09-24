package com.oberdiah.utils

import com.badlogic.gdx.graphics.OrthographicCamera
import com.oberdiah.*
import com.oberdiah.player.player
import com.oberdiah.ui.MENU_ZONE_BOTTOM_Y
import kotlin.math.pow

private var cameraY = 0.0
var camera = OrthographicCamera()
var CAMERA_IS_FOLLOWING_THE_PLAYER = false
    private set

fun resetCamera() {
    CAMERA_IS_FOLLOWING_THE_PLAYER = false
    camera.setToOrtho(false, UNITS_WIDE.f, SCREEN_HEIGHT_IN_UNITS.f)
    camera.position.y = MENU_ZONE_BOTTOM_Y.f + SCREEN_HEIGHT_IN_UNITS.f / 2
    cameraY = camera.position.y.d
    camera.update()
}

private fun getDesiredCameraY(focusPoint: Double): Float {
    return (focusPoint + SCREEN_HEIGHT_IN_UNITS / 2 - SCREEN_HEIGHT_IN_UNITS * CURRENT_PLAYER_Y_FRACT).f
}

val TRANSITION_TO_LOWER_CAMERA_HEIGHT
    get() = CURRENT_HIGHEST_TILE_Y + 3

const val LOWER_CAMERA_HEIGHT_TRANSITION_RANGE = 8

/** Set the camera Y (Bottom of the camera) in world coordinates */
fun setCameraY(y: Double) {
    cameraY = y + SCREEN_HEIGHT_IN_UNITS / 2
}

fun updateCamera() {
    if (getDesiredCameraY(player.body.p.y) < camera.position.y) {
        CAMERA_IS_FOLLOWING_THE_PLAYER = true
    }

    SCREEN_SHAKE -= 12 * DELTA
    SCREEN_SHAKE = max(SCREEN_SHAKE, 0)

    val diff = player.body.p.y - TRANSITION_TO_LOWER_CAMERA_HEIGHT
    val transitionAmount = diff / LOWER_CAMERA_HEIGHT_TRANSITION_RANGE
    CURRENT_PLAYER_Y_FRACT =
        lerp(BASE_PLAYER_Y_FRACT, ELEVATED_PLAYER_Y_FRACT, saturate(transitionAmount))

    if (CAMERA_IS_FOLLOWING_THE_PLAYER) {
        val cameraFocus = player.body.p.y
        val desiredCameraPos = getDesiredCameraY(cameraFocus)
        cameraY += (desiredCameraPos - camera.position.y) * 0.1
    }

    val shake = (Perlin.fbm(APP_TIME * 200, 0, 3, 2.0) * SCREEN_SHAKE.pow(2) * 0.2).f

    camera.position.y = cameraY.f + shake

    camera.position.x = UNITS_WIDE.f / 2
    camera.update()

    worldSpaceRenderer.renderer.projectionMatrix = camera.combined
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
