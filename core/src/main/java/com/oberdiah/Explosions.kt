package com.oberdiah

import com.oberdiah.utils.addScreenShake
import kotlin.math.pow
import kotlin.random.Random


fun boom(
    point: Point,
    radiusIn: Number,
    affectsThePlayer: Boolean = true,
    playSound: Boolean = true
) {
    val radius = radiusIn * GLOBAL_SCALE

    // To prevent explosions on exact integer coords from behaving weirdly.
    point.x += Random.nextDouble(-0.001, 0.001)
    point.y += Random.nextDouble(-0.001, 0.001)

    val tileRadius = (radius * TILES_PER_UNIT).i
    val tempPoint = Point()

    spawnGlow(point, radius)

    addScreenShake(radius.d.pow(0.5) * 0.5)

    if (playSound) {
        playExplosionSound(radius.d)
    }

    for (dx in -tileRadius..tileRadius) {
        for (dy in -tileRadius..tileRadius) {
            tempPoint.x = point.x + dx * TILE_SIZE_IN_UNITS
            tempPoint.y = point.y + dy * TILE_SIZE_IN_UNITS
            val tile = getTile(tempPoint)
            val dist = point.distTo(tempPoint)

            if (dist <= radius) {
                val vx = (tileRadius - abs(dx)) * sign(dx) + (Random.nextDouble() - 0.5)
                val vy = (tileRadius - abs(dy)) * sign(dy) + Random.nextDouble()
                val velocity =
                    Velocity(vx * 0.5 * Random.nextDouble(), (vy * 2.5 + 8) * Random.nextDouble())
                if (!DEBUG_MOVEMENT_MODE) spawnSmoke(tempPoint.cpy, velocity)
                if (tile is Tile && tile.doesExist()) {
                    tile.dematerialize()
                    spawnFragment(tempPoint.cpy, velocity, tile.getTileType())
                }
            }
        }
    }

    for (a in getAllPhysicsObjects) {
        if (a.body.p.distTo(point) < radius) {
            if (a !is Player || affectsThePlayer) {
                val pushForce = (a.body.p - point)
                pushForce.len = radius * 10
                a.body.applyImpulse(pushForce)
                a.hitByExplosion()
            }
        }
    }
}