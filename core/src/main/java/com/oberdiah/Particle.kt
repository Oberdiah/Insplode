package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.oberdiah.level.Level
import com.oberdiah.player.player
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController
import com.oberdiah.utils.GameTime.GAMEPLAY_DELTA
import com.oberdiah.utils.TileType
import kotlin.random.Random

private val allParticles = mutableListOf<Particle>()
private val particlesToDestroy = mutableListOf<Int>()
private val glowLocations = mutableListOf<Glow>()
private val glowsToDestroy = mutableListOf<Glow>()

private var particleDestroyThreshold = 0.0

fun tickParticles() {
    particleDestroyThreshold = max(0.0, (allParticles.size - 1000) / 20000.0)
    allParticles.forEach { it.tick() }
    particlesToDestroy.sortDescending()
    for (i in particlesToDestroy) {
        // Remove the particle at the end and move the last particle to the removed particle's position
        allParticles[i] = allParticles.last()
        allParticles[i].index = i
        allParticles.removeLastOrNull()
    }
    particlesToDestroy.clear()

    glowLocations.forEach { it.tick() }
    glowLocations.removeAll(glowsToDestroy)
    glowsToDestroy.clear()
}

fun resetParticles() {
    allParticles.clear()
    particlesToDestroy.clear()
    glowLocations.clear()
    glowsToDestroy.clear()
}

fun renderParticles(r: Renderer) {
    glowLocations.forEach { it.render(r) }
    allParticles.forEach { it.render(r) }
}

fun spawnFragment(p: Point, v: Velocity, tileType: TileType, affectedByGravity: Boolean = true) {
    if (!statefulRenderParticles.value) return
    if (tileType == TileType.Air) return

    val radius = TILE_SIZE_IN_UNITS * (Random.nextDouble() * 0.3 + 0.2)
    Fragment(p, v, radius, tileType, affectedByGravity).registerWithSimulation()
}

fun spawnSmoke(
    p: Point,
    velocity: Velocity,
    color: Color = Color.DARK_GRAY.withAlpha(0.5),
    gravityScaling: Double = 1.0,
    canCollide: Boolean = true,
    radiusScaling: Double = 1.0,
    pulledTowards: Point? = null,
) {
    if (!statefulRenderParticles.value) return
    // Don't spawn smoke on top of tiles
    if (Level.getTile(p).canCollide() && canCollide) return
    val radius = TILE_SIZE_IN_UNITS * (Random.nextDouble() * 0.3 + 0.2) * radiusScaling
    Smoke(
        p,
        velocity,
        radius,
        color,
        gravityScaling,
        canCollide,
        pulledTowards
    ).registerWithSimulation()
}

fun spawnGlow(p: Point, radius: Double) {
    if (!statefulRenderParticles.value) return
    glowLocations.add(Glow(p, radius))
}

class Glow(val p: Point, var radius: Double) {
    var life = 0.05
    fun tick() {
        life -= GAMEPLAY_DELTA
        if (life <= 0) {
            glowsToDestroy.add(this)
        }
    }

    fun render(r: Renderer) {
        r.color = Color.WHITE.withAlpha(0.3)
        r.circle(p, radius)
    }
}

class Smoke(
    startP: Point,
    startV: Velocity,
    edgeLength: Double,
    val color: Color = Color.DARK_GRAY.withAlpha(0.5),
    val gravityScaling: Double = 1.0,
    canCollide: Boolean = true,
    pulledTowards: Point? = null,
) : Particle(startP, startV, canCollide, edgeLength = edgeLength, pulledTowards = pulledTowards) {
    var angle = Random.nextDouble() * 3.1415 * 2
    val angleRate = Random.nextDouble() - 0.5

    init {
        bounciness = 0.05
    }

    override fun applyForces() {
        v = v.plus(y = GRAVITY * GAMEPLAY_DELTA * 0.15 * gravityScaling)
    }

    override fun tick() {
        super.tick()
        edgeLength -= GAMEPLAY_DELTA / 16
        if (stoppedMoving) {
            edgeLength -= GAMEPLAY_DELTA
        }
        angle += angleRate / 5

        if (edgeLength <= particleDestroyThreshold) {
            destroy()
        }
    }

    override fun render(r: Renderer) {
        r.color = color
        val scaleUp = 2.5
        r.centeredRect(p, edgeLength * scaleUp, edgeLength * scaleUp, angle)
    }
}

class Fragment(
    startP: Point,
    startV: Velocity,
    edgeLength: Double,
    val tileType: TileType,
    var affectedByGravity: Boolean = true,
) : Particle(startP, startV, edgeLength = edgeLength) {
    var angle = Random.nextDouble() * 3.1415 * 2
    val angleRate = Random.nextDouble() - 0.5

    override fun applyForces() {
        if (affectedByGravity) {
            v = v.minus(y = GRAVITY * GAMEPLAY_DELTA * 0.5)
        }
    }

    override fun tick() {
        super.tick()
        edgeLength -= GAMEPLAY_DELTA / 32
        if (stoppedMoving) {
            edgeLength -= GAMEPLAY_DELTA / 2
        }
        angle += angleRate / 5

        if (edgeLength <= particleDestroyThreshold) {
            destroy()
        }
    }

    override fun render(r: Renderer) {
        r.color = tileType.color()
        val scaleUp = 2.5
        r.centeredRect(p, edgeLength * scaleUp, edgeLength * scaleUp, angle)
    }

    override fun collided() {
        super.collided()
        playParticleHitSound(v.len, edgeLength)
    }
}

abstract class Particle(
    var p: Point,
    var v: Velocity = Velocity(),
    val canCollide: Boolean = true,
    var edgeLength: Double,
    val pulledTowards: Point? = null
) {
    var insideLevel = false
    var bounciness = 0.6
    var stoppedMoving = false
    var index: Int? = null
    private var hasBeenDestroyed = false
    abstract fun render(r: Renderer)

    fun registerWithSimulation() {
        index = allParticles.size
        allParticles.add(this)
    }

    fun destroy() {
        if (!hasBeenDestroyed) {
            particlesToDestroy.add(index!!)
            hasBeenDestroyed = true
        }
    }

    abstract fun applyForces();

    private var wasInsideLevel = false
    private var previousP = Point()
    open fun tick() {
        if (p.x == 0.0 || p.x == UNITS_WIDE.d) {
            destroy()
        }

        previousP = p

        if (!stoppedMoving) {
            applyForces()
        }

        val pulledTo =
            if (UpgradeController.playerHas(Upgrade.BlackHole) && GAME_STATE == GameState.InGame) {
                player.body.p
            } else {
                pulledTowards
            }

        if (pulledTo != null) {
            val unnormalizedForce = pulledTo - p
            if (unnormalizedForce.len < 0.1) {
                destroy()
                return
            }

            val force = unnormalizedForce.withLen(min(1.0, unnormalizedForce.len))
            if (v.dot(force) < 0 || v.len < 5.0) {
                v += force
            }
        }

        p += v * GAMEPLAY_DELTA

        if (p.x < 0 || p.x > UNITS_WIDE) {
            v.times(x = -1.0)
        }

        bounce()
    }

    private fun bounce() {
        if (!canCollide) return

        val tile = Level.getTile(p)
        // if it's not a tile we can forget it.
        if (tile !is Tile) return

        val tx = tile.x
        val ty = tile.y
        val bottomLeft = tile.canCollide()
        val topLeft = Level.getTile(tx, ty + 1).canCollide()
        val bottomRight = Level.getTile(tx + 1, ty).canCollide()
        val topRight = Level.getTile(tx + 1, ty + 1).canCollide()

        if (bottomLeft || topLeft || bottomRight || topRight) {
            val polygon = marchingSquaresScaled(bottomLeft, bottomRight, topLeft, topRight)

            val ex = p.x - tile.coord.x
            val ey = p.y - tile.coord.y

            var i = 0
            var j = polygon.lastIndex
            wasInsideLevel = insideLevel
            insideLevel = false
            while (i < polygon.size) {
                if (((polygon[i].y > ey) != (polygon[j].y > ey)) &&
                    ex < (polygon[j].x - polygon[i].x) * (ey - (polygon[i].y)) / (polygon[j].y - polygon[i].y) + (polygon[i].x)
                ) {
                    insideLevel = !insideLevel
                }
                j = i++
            }

            if (insideLevel && !wasInsideLevel) {
                // Iterate over lines in polygon
                // intersect each with our wee line segment

                var winner = Point(-100, 0)
                var normal: Point? = null
                var previousL = polygon[0] + tile.coord
                for (k in 1..polygon.size) {
                    val l = polygon[k % polygon.size] + tile.coord
                    val intersectionPoint = lineIntersection(previousL, l, previousP, p)
                    if (intersectionPoint != null) {
                        if (intersectionPoint.distTo(previousP) < winner.distTo(previousP)) {
                            winner = intersectionPoint
                            normal = lineNormal(previousL, l, previousP)
                        }
                    }

                    previousL = l
                }

                if (normal != null) {
                    val newV = v - normal * 2 * v.dot(normal)
                    v = newV * bounciness

                    collided()
                }
            }
            if (insideLevel && wasInsideLevel) {
                v = Point()
                stoppedMoving = true
            }
        }
    }

    open fun collided() {}
}