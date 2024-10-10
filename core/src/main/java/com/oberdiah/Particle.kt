package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.oberdiah.level.getTile
import com.oberdiah.utils.GameTime.GAMEPLAY_DELTA
import com.oberdiah.utils.TileType
import kotlin.random.Random

private val allParticles = mutableListOf<Particle>()
private val particlesToDestroy = mutableListOf<Int>()
private val glowLocations = mutableListOf<Glow>()
private val glowsToDestroy = mutableListOf<Glow>()

private var particleDestroyThreshold = 0.0

fun tickParticles() {
    particleDestroyThreshold = max(0, (allParticles.size - 1000) / 20000.0)
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
    canCollide: Boolean = true
) {
    if (!statefulRenderParticles.value) return
    // Don't spawn smoke on top of tiles
    if (getTile(p).canCollide() && canCollide) return
    val radius = TILE_SIZE_IN_UNITS * (Random.nextDouble() * 0.3 + 0.2)
    Smoke(p, velocity, radius, color, gravityScaling, canCollide).registerWithSimulation()
}

fun spawnGlow(p: Point, radius: Number) {
    if (!statefulRenderParticles.value) return
    glowLocations.add(Glow(p, radius))
}

class Glow(val p: Point, var radius: Number) {
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
    edgeLength: Number,
    val color: Color = Color.DARK_GRAY.withAlpha(0.5),
    val gravityScaling: Double = 1.0,
    canCollide: Boolean = true,
) : Particle(startP, startV, canCollide, edgeLength = edgeLength) {
    var angle = Random.nextDouble() * 3.1415 * 2
    val angleRate = Random.nextDouble() - 0.5

    init {
        bounciness = 0.05
    }

    override fun applyForces() {
        v.y += GRAVITY * GAMEPLAY_DELTA * 0.15 * gravityScaling
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
    edgeLength: Number,
    val tileType: TileType,
    var affectedByGravity: Boolean = true,
) : Particle(startP, startV, edgeLength = edgeLength) {
    var angle = Random.nextDouble() * 3.1415 * 2
    val angleRate = Random.nextDouble() - 0.5

    override fun applyForces() {
        if (affectedByGravity) {
            v.y -= GRAVITY * GAMEPLAY_DELTA * 0.5
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
        playParticleHitSound(v.len.d, edgeLength.d)
    }
}

abstract class Particle(
    val p: Point,
    val v: Velocity = Velocity(),
    val canCollide: Boolean = true,
    var edgeLength: Number,
) {
    var insideLevel = false
    var bounciness = 0.6
    var stoppedMoving = false
    var index: Int? = null
    abstract fun render(r: Renderer)

    fun registerWithSimulation() {
        index = allParticles.size
        allParticles.add(this)
    }

    fun destroy() {
        particlesToDestroy.add(index!!)
    }

    abstract fun applyForces();

    private var wasInsideLevel = false
    private val previousP = Point()
    open fun tick() {
        if (p.x == 0.0 || p.x == UNITS_WIDE.d) {
            destroy()
        }

        previousP.x = p.x
        previousP.y = p.y
        if (!stoppedMoving) {
            applyForces()
        }
        p.x += v.x * GAMEPLAY_DELTA
        p.y += v.y * GAMEPLAY_DELTA

        if (p.x < 0 || p.x > UNITS_WIDE) {
            v.x *= -1
        }

        bounce()
    }

    private fun bounce() {
        if (!canCollide) return

        val tile = getTile(p)
        // if it's not a tile we can forget it.
        if (tile !is Tile) return

        val tx = tile.x
        val ty = tile.y
        val bottomLeft = tile.canCollide()
        val topLeft = getTile(tx, ty + 1).canCollide()
        val bottomRight = getTile(tx + 1, ty).canCollide()
        val topRight = getTile(tx + 1, ty + 1).canCollide()

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