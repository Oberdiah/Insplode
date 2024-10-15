package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.player
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.GameTime.GAMEPLAY_DELTA
import com.oberdiah.utils.colorScheme
import kotlin.math.pow
import kotlin.random.Random

object PointOrbs {
    private data class OrbToBe(
        val p: Point,
        val points: Int,
        val startVel: Velocity = Velocity(),
        val addRandomVelocity: Boolean = true,
        // Spawn a single large orb rather than many small ones.
        val spawnSingleOrb: Boolean = false,
        val ensureEmptySpaceOnSpawn: Boolean = true,
        val canBePickedUpInstantly: Boolean = false,
        // Starts out at 0.0 and increases a bit every frame we can't find a place to spawn.
        var searchRadius: Double = 0.0
    )

    private var orbsToSpawn = mutableListOf<OrbToBe>()

    // We like to spawn point orbs with these values if we can, it's just nice :)
    private val pointOrbValues = listOf(1, 5, 20, 50, 100)

    fun reset() {
        orbsToSpawn.clear()
    }

    fun tick() {
        orbsToSpawn.removeIf { orbToSpawn ->
            val (_, points, startVel) = orbToSpawn
            var p = orbToSpawn.p + createRandomFacingPoint() * orbToSpawn.searchRadius

            // Check if it's safe to spawn
            if (orbToSpawn.ensureEmptySpaceOnSpawn) {
                val radius = PointOrb.calculateRadius(points)
                // This should technically be radius * 2, but we want to allow a little overlap.
                if (!isRectEmptySpace(Rect.centered(p, Size(radius, radius)))) {
                    orbToSpawn.searchRadius += 0.1
                    return@removeIf false
                }
            }

            if (orbToSpawn.spawnSingleOrb) {
                PointOrb(p, points, startVel)
                return@removeIf true
            }

            // greedy algorithm to spawn point orbs
            var pointsLeft = points
            while (pointsLeft > 0) {
                val orbScore = pointOrbValues.last { it <= pointsLeft }
                var velocity = startVel

                if (orbToSpawn.addRandomVelocity) {
                    velocity += createRandomFacingPoint() * 5.0
                }

                val orb = PointOrb(p, orbScore, velocity)
                if (orbToSpawn.canBePickedUpInstantly) {
                    orb.timeAlive = 0.5
                }
                pointsLeft -= orbScore
            }

            return@removeIf true
        }
    }

    /**
     * Spawns a point orb at the given point worth the given number of points.
     */
    fun spawnOrbs(
        p: Point,
        scoreGiven: Int,
        startVel: Velocity = Velocity(),
        addRandomVelocity: Boolean = true,
        spawnSingleOrb: Boolean = false,
        ensureEmptySpaceOnSpawn: Boolean = true,
        canBePickedUpInstantly: Boolean = false
    ) {
        if (scoreGiven == 0) {
            return
        }

        // We need to delay spawning because otherwise the physics system doesn't like it much.
        orbsToSpawn.add(
            OrbToBe(
                p,
                scoreGiven,
                startVel,
                addRandomVelocity,
                spawnSingleOrb,
                ensureEmptySpaceOnSpawn,
                canBePickedUpInstantly
            )
        )
    }

    class PointOrb(
        startingPoint: Point,
        val value: Int,
        startingVelocity: Velocity = Velocity()
    ) : PhysicsObject(startingPoint, startingVelocity) {
        var timeAlive = 0.0
        var isAttractedToPlayer = false
        var timeAttractedToPlayer = 0.0

        companion object {
            const val GOLDEN_THRESHOLD = 50

            fun calculateRadius(value: Int): Double {
                var relativeValue = value.d
                if (relativeValue >= GOLDEN_THRESHOLD) {
                    relativeValue /= 50.0
                }

                // https://www.desmos.com/calculator/dj5zns3vu4
                return relativeValue.pow(0.2) / 8.0 + 0.03
            }
        }

        val radius: Double
            get() = calculateRadius(value)

        val color: Color
            get() {
                return if (value >= GOLDEN_THRESHOLD) {
                    colorScheme.goldenPointOrbColor
                } else {
                    colorScheme.pointOrbColor
                }
            }

        init {
            circleShape(radius) {
                val fixtureDef = FixtureDef()
                fixtureDef.shape = it
                // Very light so it doesn't affect the player much.
                fixtureDef.density = 5.5f
                fixtureDef.friction = 0.5f
                fixtureDef.restitution = 0.2f
                fixtureDef.filter.categoryBits = PICKUP_PHYSICS_MASK
                // This has a corresponding line in PhysicsObjects.kt
//                fixtureDef.filter.maskBits = BOMB_PHYSICS_MASK.inv()
                body.addFixture(fixtureDef)
            }
        }

        override fun tick() {
            super.tick()
            timeAlive += GAMEPLAY_DELTA

            val magnetRadius = UpgradeController.getPlayerMagnetRadius()

            val playerPos = player.body.p
            val distance = playerPos.distTo(body.p)
            isAttractedToPlayer = distance < magnetRadius && player.state.isAlive

            if (isAttractedToPlayer) {
                val direction = (playerPos - body.p)
                direction.len = clamp(direction.len, 0.5, 1.5)
                body.applyImpulse(direction * 0.5, body.p)
                timeAttractedToPlayer += GAMEPLAY_DELTA
            } else {
                timeAttractedToPlayer -= GAMEPLAY_DELTA
                timeAttractedToPlayer = timeAttractedToPlayer.coerceAtLeast(0.0)
            }
        }

        override fun collided(yourFixture: Fixture, otherFixture: Fixture) {
            super.collided(yourFixture, otherFixture)
            val data = otherFixture.body.userData
            if (timeAlive > 0.5 || isAttractedToPlayer) {
                if (data == player && player.state.isAlive && otherFixture.userData == PLAYER_DETECTOR_IDENTIFIER) {
                    destroy()

                    val strength = value.d.pow(0.5)
                    val radius = strength * TILE_SIZE_IN_UNITS

                    if (value >= 5.0) {
                        boom(
                            body.p,
                            radius * 0.5,
                            affectsThePlayer = false,
                            affectsTheLandscape = false,
                            playSound = false
                        )
                    }

                    ScoreSystem.givePlayerScore(value)
                    spawnSmokeHere(15, body.p)
                }
            }
        }

        override fun collided(obj: PhysicsObject) {
            super.collided(obj)
            if (timeAlive > 0.5 && UpgradeController.playerHas(Upgrade.CombineOrbs)) {
                if (obj is PointOrb) {
                    if (obj.value == value) {
                        // Spawn a new orb in, with the combined value.
                        spawnOrbs(
                            body.p,
                            value * 2,
                            body.velocity,
                            addRandomVelocity = false,
                            spawnSingleOrb = true,
                            ensureEmptySpaceOnSpawn = false,
                            canBePickedUpInstantly = true
                        )

                        val smokeSpawnPoint = body.p + (obj.body.p - body.p) * 0.5
                        spawnSmokeHere(5, smokeSpawnPoint)

                        obj.destroy()
                        destroy()
                    }
                }
            }
        }

        private fun spawnSmokeHere(num: Int, centeredOn: Point) {
            val radius = radius.pow(0.5)
            for (i in 0..num * radius.i) {
                spawnSmoke(
                    centeredOn + createRandomFacingPoint() * Random.nextDouble() * this@PointOrb.radius * 0.25,
                    createRandomFacingPoint() * Random.nextDouble(),
                    color.cpy().lerp(Color.WHITE, Random.nextDouble(0.3, 0.8).f)
                )
            }
        }

        override fun render(r: Renderer) {
            val radius =
                saturate(saturate(timeAlive * 2.5 + 0.5) - saturate(timeAttractedToPlayer) * 0.5) * radius

            val thisColor = color

            r.color = thisColor.cpy().lerp(Color.BLACK, 0.75f).withAlpha(0.5)
            r.circle(body.p, radius * 1.15)

            r.color = thisColor
            r.circle(body.p, radius)

            // render a second white circle on top, pulsing in size
            r.color = Color.WHITE.withAlpha(0.4)
            r.circle(body.p, radius * (0.6 + sin(RUN_TIME_ELAPSED * 5) * 0.2))
        }
    }
}