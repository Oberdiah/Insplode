package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.player
import com.oberdiah.player.playerState
import com.oberdiah.utils.GameTime.GAMEPLAY_DELTA
import com.oberdiah.utils.colorScheme
import kotlin.math.pow
import kotlin.random.Random

object PointOrbs {
    private data class OrbToBe(val p: Point, val points: Int, val startVel: Velocity = Velocity())

    private var orbsToSpawn = mutableListOf<OrbToBe>()

    fun tick() {
        orbsToSpawn.forEach { orbToSpawn ->
            val (p, points, startVel) = orbToSpawn
            // greedy algorithm to spawn point orbs
            var pointsLeft = points
            while (pointsLeft > 0) {
                val orbScore = pointOrbValues.first { it.scoreGiven <= pointsLeft }
                PointOrb(p, orbScore, createRandomFacingPoint() * 5.0 + startVel)
                pointsLeft -= orbScore.scoreGiven
            }
        }
        orbsToSpawn.clear()
    }

    /**
     * Spawns a point orb at the given point worth the given number of points.
     */
    fun spawnOrbs(p: Point, scoreGiven: Int, startVel: Velocity = Velocity()) {
        // We need to delay spawning because otherwise the physics system doesn't like it much.
        orbsToSpawn.add(OrbToBe(p, scoreGiven, startVel))
    }

    private val pointOrbValues = PointOrbValue.entries.sortedByDescending { it.scoreGiven }

    enum class PointOrbValue(val scoreGiven: Int, val radius: Double) {
        One(1, 0.15),
        Five(5, 0.20),
        Twenty(20, 0.25),
        Hundred(100, 0.30),
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
            timeAlive += GAMEPLAY_DELTA
        }

        override fun collided(obj: PhysicsObject) {
            super.collided(obj)
            if (obj == player && timeAlive > 0.5 && playerState.isAlive) {
                destroy()

                val strength = value.scoreGiven.d.pow(0.5)
                val radius = strength * TILE_SIZE_IN_UNITS

                if (value != PointOrbValue.One) {
                    boom(
                        body.p,
                        radius * 0.5,
                        affectsThePlayer = false,
                        affectsTheLandscape = false,
                        playSound = false
                    )
                }

                givePlayerScore(value.scoreGiven)
                for (i in 0..15 * strength.i) {
                    spawnSmoke(
                        body.p + createRandomFacingPoint() * Random.nextDouble() * radius * 0.25,
                        createRandomFacingPoint() * Random.nextDouble(),
                        colorScheme.pickupColor.cpy()
                            .lerp(Color.WHITE, Random.nextDouble(0.3, 0.8).f)
                    )
                }
            }
        }

        override fun render(r: Renderer) {
            val radius = saturate(timeAlive * 2.5 + 0.5) * value.radius

            drawOrb(r, body.p, radius)
        }
    }

    fun drawOrb(r: Renderer, p: Point, radius: Double) {
        r.color = colorScheme.pickupColor.cpy().lerp(Color.BLACK, 0.75f).withAlpha(0.5)
        r.circle(p, radius * 1.15)

        r.color = colorScheme.pickupColor
        r.circle(p, radius)

        // render a second white circle on top, pulsing in size
        r.color = Color.WHITE.withAlpha(0.4)
        r.circle(p, radius * (0.6 + sin(RUN_TIME_ELAPSED * 5) * 0.2))
    }
}