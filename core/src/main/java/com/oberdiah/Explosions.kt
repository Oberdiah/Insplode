package com.oberdiah

import kotlin.math.pow
import kotlin.random.Random


fun boom(point: Point, radius: Number, hurtsThePlayer: Boolean = true) {
    val simpleRadius = (radius * SIMPLES_RESOLUTION).i
    val tempPoint = Point()

    spawnGlow(point, radius)

    addScreenShake(radius.d.pow(0.5) * 0.5)

    playExplosionSound(radius.d)

    for (dx in -simpleRadius..simpleRadius) {
        for (dy in -simpleRadius..simpleRadius) {
            tempPoint.x = point.x + dx * SIMPLE_SIZE
            tempPoint.y = point.y + dy * SIMPLE_SIZE
            val tile = getTile(tempPoint)
            val dist = point.distTo(tempPoint)

            if (dist <= radius) {
                val vx = (simpleRadius - abs(dx)) * sign(dx) + (Random.nextDouble() - 0.5)
                val vy = (simpleRadius - abs(dy)) * sign(dy) + Random.nextDouble()
                val velocity =
                    Velocity(vx * 0.5 * Random.nextDouble(), (vy * 2.5 + 8) * Random.nextDouble())
                if (!DEBUG_MOVEMENT_MODE) spawnSmoke(tempPoint.cpy, velocity)
                if (tile.exists) {
                    tile.exists = false

                    spawnFragment(tempPoint.cpy, velocity, tile.tileType)
                }
            }
        }
    }

    for (a in getAllPhysicsObjects) {
        if (a.body.p.distTo(point) < radius) {
            val pushForce = (a.body.p - point)
            pushForce.len = radius * 10
            a.body.applyImpulse(pushForce)

            if (a !is Player || hurtsThePlayer) {
                a.hitByExplosion()
            }
        }
    }
}