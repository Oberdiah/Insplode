package com.oberdiah

import kotlin.math.pow
import kotlin.random.Random


fun boom(
    point: Point,
    radiusIn: Number,
    affectsThePlayer: Boolean = true,
    playSound: Boolean = true
) {
    val radius = radiusIn * GLOBAL_SCALE

    val simpleRadius = (radius * SIMPLES_PER_UNIT).i
    val tempPoint = Point()

    spawnGlow(point, radius)

    addScreenShake(radius.d.pow(0.5) * 0.5)

    if (playSound) {
        playExplosionSound(radius.d)
    }

    for (dx in -simpleRadius..simpleRadius) {
        for (dy in -simpleRadius..simpleRadius) {
            tempPoint.x = point.x + dx * SIMPLE_SIZE_IN_WORLD
            tempPoint.y = point.y + dy * SIMPLE_SIZE_IN_WORLD
            val tile = getTile(tempPoint)
            val dist = point.distTo(tempPoint)

            if (dist <= radius) {
                val vx = (simpleRadius - abs(dx)) * sign(dx) + (Random.nextDouble() - 0.5)
                val vy = (simpleRadius - abs(dy)) * sign(dy) + Random.nextDouble()
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