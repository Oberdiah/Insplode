package com.oberdiah.Utils

import com.badlogic.gdx.graphics.OrthographicCamera
import com.oberdiah.*
import kotlin.math.pow
import kotlin.random.Random

private var cameraY = 0.0
var camera = OrthographicCamera()

fun resetCamera() {
    camera.setToOrtho(false, SQUARES_WIDE.f, SQUARES_TALL.f)
    camera.position.y = (PLAYER_SPAWN_Y + SQUARES_TALL/2 - SQUARES_TALL * PLAYER_Y_FRACT).f
    cameraY = camera.position.y.d
    camera.update()
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
                if (it.y < HEIGHT/2) {
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

    SCREEN_SHAKE -= 0.1
    SCREEN_SHAKE = max(SCREEN_SHAKE, 0)

    val cameraFocus = player.body.p.y
    val desiredCameraPos = (cameraFocus + SQUARES_TALL/2 - SQUARES_TALL * PLAYER_Y_FRACT)
    cameraY += (desiredCameraPos - camera.position.y) * 0.1

    val shake = (Perlin.fbm(APP_TIME * 200, 0, 3, 2.0) * SCREEN_SHAKE.pow(2) * 0.2).f

    camera.position.y = cameraY.f + shake

    camera.position.x = SQUARES_WIDE.f/2
    camera.update()

    worldSpaceRenderer.renderer.projectionMatrix = camera.combined
}