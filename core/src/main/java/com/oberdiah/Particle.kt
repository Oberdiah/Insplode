package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.oberdiah.Utils.TileType
import kotlin.random.Random

private val allParticles = mutableListOf<Particle>()
val particlesToDestroy = mutableListOf<Particle>()
val glowLocations = mutableListOf<Glow>()
val glowsToDestroy = mutableListOf<Glow>()

fun tickParticles() {
    if (APP_FRAME % 20 == 0) {
//        allParticles.add(Fragment(Point(5, 15), Velocity(Random.nextDouble() - 0.5, -Random.nextDouble()) * 5, 0.5, TileType.Stone))
//        allParticles.add(Fragment(Point(5, -15), Velocity(0.2, -1) * 5, 0.125, TileType.Stone))
    }

    allParticles.forEach { it.tick() }
    allParticles.removeAll(particlesToDestroy)
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
    if (!ENABLED_PARTICLES) return

    val radius = SIMPLE_SIZE_IN_WORLD * (Random.nextDouble() * 0.3 + 0.2)

    allParticles.add(Fragment(p, v, radius, tileType, affectedByGravity))
}

fun spawnSmoke(p: Point, velocity: Velocity) {
    if (!ENABLED_PARTICLES) return
    val radius = SIMPLE_SIZE_IN_WORLD * (Random.nextDouble() * 0.3 + 0.2)
    allParticles.add(Smoke(p, velocity, radius))
}

fun spawnGlow(p: Point, radius: Number) {
    glowLocations.add(Glow(p, radius))
}

class Glow(val p: Point, var radius: Number) {
    var life = 0.05
    fun tick() {
        life -= DELTA
        if (life <= 0) {
            glowsToDestroy.add(this)
        }
    }

    fun render(r: Renderer) {
        r.color = Color.WHITE.withAlpha(0.3)
        r.circle(p, radius)
    }
}

class Smoke(startP: Point, startV: Velocity, var edgeLength: Number) : Particle(startP, startV) {
    var angle = Random.nextDouble() * 3.1415 * 2
    val angleRate = Random.nextDouble() - 0.5

    init {
        bounciness = 0.05
    }

    override fun applyForces() {
        v.y += GRAVITY * DELTA * 0.15
    }

    override fun tick() {
        super.tick()
        edgeLength -= DELTA / 16
        if (stoppedMoving) {
            edgeLength -= DELTA
        }
        angle += angleRate / 5

        if (edgeLength <= 0) {
            destroy()
        }
    }

    override fun render(r: Renderer) {
        r.color = Color.DARK_GRAY.withAlpha(0.5)
        val scaleUp = 2.5
        r.centeredRect(p, edgeLength * scaleUp, edgeLength * scaleUp, angle)
    }
}

class Fragment(
    startP: Point,
    startV: Velocity,
    var edgeLength: Number,
    val tileType: TileType,
    var affectedByGravity: Boolean = true
) :
    Particle(startP, startV) {
    var angle = Random.nextDouble() * 3.1415 * 2
    val angleRate = Random.nextDouble() - 0.5

    override fun applyForces() {
        if (affectedByGravity) {
            v.y -= GRAVITY * DELTA * 0.5
        }
    }

    override fun tick() {
        super.tick()
        edgeLength -= DELTA / 32
        if (stoppedMoving) {
            edgeLength -= DELTA / 2
        }
        angle += angleRate / 5

        if (edgeLength <= 0) {
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
        playParticleSound(v.len.d, edgeLength.d)
    }
}

abstract class Particle(val p: Point, val v: Velocity = Velocity()) {
    var insideLevel = false
    var bounciness = 0.6
    var stoppedMoving = false

    abstract fun render(r: Renderer)

    fun destroy() {
        particlesToDestroy.add(this)
    }

    abstract fun applyForces();

    private var wasInsideLevel = false
    private val previousP = Point()
    open fun tick() {
        if (p.x == 0.0 || p.x == WORLD_WIDTH.d) {
            destroy()
        }

        previousP.x = p.x
        previousP.y = p.y
        if (!stoppedMoving) {
            applyForces()
        }
        p.x += v.x * DELTA
        p.y += v.y * DELTA

        if (p.x < 0) {
            v.x *= -1
        } else if (p.x > WORLD_WIDTH) {
            v.x *= -1
        }

        bounce()
    }

    private fun bounce() {
        val tile = getTile(p)
        // if it's not a tile we can forget it.
        if (tile.isNAT) return

        val tx = tile.x
        val ty = tile.y
        val bottomLeft = tile.exists
        val topLeft = getTile(tx, ty + 1).exists
        val bottomRight = getTile(tx + 1, ty).exists
        val topRight = getTile(tx + 1, ty + 1).exists

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

                val intersectionPoint = Point()
                val winner = Point(-100, 0)
                var normal: Point? = null
                var previousL = polygon[0] + tile.coord
                for (k in 1..polygon.size) {
                    val l = polygon[k % polygon.size] + tile.coord

                    if (lineIntersection(previousL, l, previousP, p, intersectionPoint)) {
                        if (intersectionPoint.distTo(previousP) < winner.distTo(previousP)) {
                            winner.setTo(intersectionPoint)
                            normal = lineNormal(previousL, l, previousP)
                        }
                    }

                    previousL = l
                }

                if (normal != null) {
                    val newV = v - normal * 2 * v.dot(normal)
                    v.x = newV.x * bounciness
                    v.y = newV.y * bounciness

                    collided()
                }
            }
            if (insideLevel && wasInsideLevel) {
                v.x = 0.0
                v.y = 0.0
                stoppedMoving = true
            }
        }
    }

    open fun collided() {}
}