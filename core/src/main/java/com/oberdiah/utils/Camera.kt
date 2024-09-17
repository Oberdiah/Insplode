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
    return (focusPoint + SCREEN_HEIGHT_IN_UNITS / 2 - SCREEN_HEIGHT_IN_UNITS * PLAYER_Y_FRACT).f
}

private var bombing = false
fun updateCamera() {
    if (DEBUG_MOVEMENT_MODE) {
        TOUCHES_WENT_DOWN.forEach {
            if (it.y > HEIGHT - 200) {
                bombing = true
            }
        }
        TOUCHES_WENT_UP.forEach {
            bombing = false
        }
        TOUCHES_DOWN.forEach {
            if (!bombing) {
                if (it.y < HEIGHT / 2) {
                    player.body.setTransform(player.body.p + Point(0, -0.5), 0f)
                } else {
                    player.body.setTransform(player.body.p + Point(0, 0.5), 0f)
                }
            } else {
                if (APP_FRAME % 20 == 0) {
                    boom(it.wo, 1.3)
                }
            }
        }
    }

    SCREEN_SHAKE -= 12 * DELTA
    SCREEN_SHAKE = max(SCREEN_SHAKE, 0)

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
