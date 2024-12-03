package com.oberdiah

import com.oberdiah.level.Level
import com.oberdiah.player.Player
import com.oberdiah.player.player
import com.oberdiah.utils.addScreenShake
import com.oberdiah.utils.vibrate
import kotlin.math.pow
import kotlin.random.Random


fun boom(
    point: Point,
    radiusIn: Double,
    affectsThePlayer: Boolean = true,
    playSound: Boolean = true,
    affectsTheLandscape: Boolean = true
) {
    val radius = radiusIn * GLOBAL_SCALE

    // To prevent explosions on exact integer coords from behaving weirdly.
    point.x += Random.nextDouble(-0.001, 0.001)
    point.y += Random.nextDouble(-0.001, 0.001)

    val tileRadius = (radius * TILES_PER_UNIT).i
    val tempPoint = Point()

    if (affectsTheLandscape) {
        // It looks weird rendered over the landscape
        spawnGlow(point, radius)
    }

    addScreenShake(radius.d.pow(0.5) * 0.5)

    if (playSound) {
        playExplosionSound(radius.d)
    }

    if (radius > 2.0 && player.state.isAlive) {
        vibrate((radius * 10).i)
    }

    val expectedNumParticlesToSpawn = tileRadius * tileRadius * 4
    val maxParticlesToSpawn = 500

    var fractionOfParticlesToSpawn = 1.0
    if (expectedNumParticlesToSpawn > maxParticlesToSpawn) {
        fractionOfParticlesToSpawn = maxParticlesToSpawn.d / expectedNumParticlesToSpawn
    }

    for (dx in -tileRadius..tileRadius) {
        for (dy in -tileRadius..tileRadius) {
            tempPoint.x = point.x + dx * TILE_SIZE_IN_UNITS
            tempPoint.y = point.y + dy * TILE_SIZE_IN_UNITS
            val tile = Level.getTile(tempPoint)
            val dist = point.distTo(tempPoint)

            if (dist <= radius) {
                val vx = (tileRadius - abs(dx)) * sign(dx) + (Random.nextDouble() - 0.5)
                val vy = (tileRadius - abs(dy)) * sign(dy) + Random.nextDouble()
                val velocity =
                    Velocity(vx * 0.5 * Random.nextDouble(), (vy * 2.5 + 8) * Random.nextDouble())
                if (tile is Tile && tile.doesExist() && affectsTheLandscape) {
                    tile.dematerialize()
                    if (Random.nextDouble() < fractionOfParticlesToSpawn) {
                        spawnFragment(tempPoint.cpy, velocity, tile.getTileType())
                    }
                }
                if (Random.nextDouble() < fractionOfParticlesToSpawn) {
                    spawnSmoke(tempPoint.cpy, velocity)
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
        // Also give all physics objects on stage a kick upward
        if (a !is Player && a.body.velocity.len < 2.0) {
            a.body.applyImpulse(Point(0.0, radius))
        }
    }
}