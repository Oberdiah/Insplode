package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.oberdiah.utils.addScreenShake
import kotlin.experimental.inv
import kotlin.math.pow

var orbsToSpawn = mutableListOf<Pair<Point, Int>>()

fun tickPointOrbs() {
    orbsToSpawn.forEach { orbToSpawn ->
        val (p, points) = orbToSpawn
        // greedy algorithm to spawn point orbs
        var pointsLeft = points
        while (pointsLeft > 0) {
            val orbScore = pointOrbValues.first { it.scoreGiven <= pointsLeft }
            PointOrb(p, orbScore, createRandomFacingPoint() * 5.0)
            pointsLeft -= orbScore.scoreGiven
        }
    }
    orbsToSpawn.clear()
}

/**
 * Spawns a point orb at the given point worth the given number of points.
 */
fun spawnPointOrbs(p: Point, scoreGiven: Int) {
    orbsToSpawn.add(p to scoreGiven)
}

private val pointOrbValues = PointOrbValue.values().sortedByDescending { it.scoreGiven }

enum class PointOrbValue(val scoreGiven: Int, val radius: Double, val color: Color) {
    One(1, 0.15, Color.ROYAL.withAlpha(0.5)),
    Five(5, 0.20, Color.ROYAL.withAlpha(0.5)),
    Twenty(20, 0.25, Color.ROYAL.withAlpha(0.5)),
    Hundred(100, 0.30, Color.ROYAL.withAlpha(0.5)),
}

class PointOrb(
    startingPoint: Point,
    val value: PointOrbValue,
    startingVelocity: Velocity = Velocity()
) : PhysicsObject(startingPoint, startingVelocity) {
    var timeAlive = 0.0

    init {
        circleShape(value.radius) {
            val fixtureDef = FixtureDef()
            fixtureDef.shape = it
            fixtureDef.density = 5.5f
            fixtureDef.friction = 0.5f
            fixtureDef.restitution = 0.4f
            fixtureDef.filter.categoryBits = PICKUP_PHYSICS_MASK
            // This has a corresponding line in PhysicsObjects.kt
//            fixtureDef.filter.maskBits = BOMB_PHYSICS_MASK.inv()
            body.addFixture(fixtureDef)
        }
    }

    override fun tick() {
        super.tick()
        timeAlive += DELTA
    }

    override fun collided(obj: PhysicsObject) {
        super.collided(obj)
        if (obj == player && timeAlive > 0.5) {
            destroy()

            if (value != PointOrbValue.One) {
                boom(
                    body.p,
                    value.scoreGiven.d.pow(0.5) * TILE_SIZE_IN_UNITS,
                    affectsThePlayer = false,
                )
            }
            playPickupSound(value)
            for (i in 0..15) {
                spawnSmoke(
                    body.p + createRandomFacingPoint() * 0.1,
                    createRandomFacingPoint(),
                    value.color
                )
            }
        }
    }

    override fun render(r: Renderer) {
        r.color = value.color
        r.circle(body.p, value.radius)
    }
}