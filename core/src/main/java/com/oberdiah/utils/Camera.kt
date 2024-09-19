package com.oberdiah.utils

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.oberdiah.*
import kotlin.math.pow

private var cameraY = 0.0
var camera = OrthographicCamera()

fun resetCamera() {
    CAMERA_LOCKED = true
    camera.setToOrtho(false, UNITS_WIDE.f, SCREEN_HEIGHT_IN_UNITS.f)
    camera.position.y = getDesiredCameraY(CAMERA_SPAWN_Y)
    cameraY = camera.position.y.d
    camera.update()
}

private fun getDesiredCameraY(focusPoint: Double): Float {
    return (focusPoint + SCREEN_HEIGHT_IN_UNITS / 2 - SCREEN_HEIGHT_IN_UNITS * CURRENT_PLAYER_Y_FRACT).f
}

val TRANSITION_TO_LOWER_CAMERA_HEIGHT
    get() = CURRENT_HIGHEST_TILE_Y + 3

const val LOWER_CAMERA_HEIGHT_TRANSITION_RANGE = 8

fun updateCamera() {
    SCREEN_SHAKE -= 12 * DELTA
    SCREEN_SHAKE = max(SCREEN_SHAKE, 0)

    val diff = player.body.p.y - TRANSITION_TO_LOWER_CAMERA_HEIGHT
    val transitionAmount = diff / LOWER_CAMERA_HEIGHT_TRANSITION_RANGE
    CURRENT_PLAYER_Y_FRACT =
        lerp(BASE_PLAYER_Y_FRACT, ELEVATED_PLAYER_Y_FRACT, saturate(transitionAmount))

    if (!CAMERA_LOCKED) {
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

/** World space to screen space */
fun wToSSpace(p: Point): Point {
    // Use the camera projection matrix. p is a 2d point in world space.
    val vec = camera.project(Vector3(p.x.f, p.y.f, 0f))
    return Point(vec.x.d, vec.y.d)
}

enum class ScreenShakeSettings(val text: String, val shakeAmount: Number) {
    Off("Off", 0.0),
    Low("Low", 1.0),
    Normal("Normal", 2.0),
    Extreme("Extreme", 5.0),
}

private var SCREEN_SHAKE = 0.0
fun addScreenShake(amount: Number) {
    SCREEN_SHAKE = max(SCREEN_SHAKE, amount.d * SCREEN_SHAKE_SETTING.shakeAmount)
}
