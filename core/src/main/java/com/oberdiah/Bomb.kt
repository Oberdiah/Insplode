package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.Utils.colorScheme
import kotlin.experimental.inv
import kotlin.math.PI
import kotlin.random.Random

private fun bombFixtureDef(shape: Shape): FixtureDef {
    val fixtureDef = FixtureDef()
    fixtureDef.shape = shape
    fixtureDef.density = 10.5f
    fixtureDef.friction = 1.0f
    fixtureDef.restitution = 0.1f
    fixtureDef.filter.categoryBits = BOMB_PHYSICS_MASK
    fixtureDef.filter.maskBits = PICKUP_PHYSICS_MASK.inv()
    return fixtureDef
}

abstract class Bomb(startingPoint: Point, val bombType: BombType) : PhysicsObject(startingPoint) {
    val radius
        get() = bombType.renderRadius

    val color
        get() = bombType.color

    val power
        get() = bombType.power

    val size = Size(radius * 2, radius * 2)

    override fun hitByExplosion() {
        explode()
    }

    open fun explode() {
        destroy()
    }

    override fun collided(yourFixture: Fixture, otherFixture: Fixture) {
        super.collided(yourFixture, otherFixture)
        playBombBumpSound(
            yourFixture.body.linearVelocity.len().d,
            yourFixture.body.mass.d,
            otherFixture.body.userData
        )
    }
}

enum class BombType(val power: Number, val renderRadius: Number, val color: Color) {
    SmallTimed(0.8, 0.2, colorScheme.bombPrimary),
    MediumTimed(1.3, 0.3, colorScheme.bombPrimary),
    LargeTimed(2.0, 0.4, colorScheme.bombPrimary),
    MegaTimed(3.0, 0.6, colorScheme.bombPrimary),
    LineBomb(0.3, 0.15, colorScheme.bombPrimary),
    SpringBomb(1.0, 0.25, colorScheme.bombPrimary),
    StickyBomb(1.3, 0.3, colorScheme.bombPrimary),
    ClusterBomb(1.0, 0.3, colorScheme.bombPrimary),
    ClusterParticle(0.8, 0.1, Color.BLACK),
    ImpactBomb(1.0, 0.3, colorScheme.bombPrimary),
}

class LineBomb(startingPoint: Point, val totalTime: Number = 5.0) :
    Bomb(startingPoint, BombType.LineBomb) {
    private val cornerSize = size / sqrt(2)
    var timeLeft = totalTime

    init {
        rectShape(size) {
            body.addFixture(bombFixtureDef(it))
        }
        rectShape(cornerSize, Point(PI / 2) * radius, PI / 4) {
            body.addFixture(bombFixtureDef(it))
        }
        rectShape(cornerSize, Point(-PI / 2) * radius, -PI / 4) {
            body.addFixture(bombFixtureDef(it))
        }
    }

    var lineLength = 10
    val canBlow: Boolean
        get() {
            val angle = (body.angle % (PI * 2) + PI * 2) % (PI * 2)
            val down = PI / 2
            val up = 3 * PI / 2
            val range = PI / 4
            if (angle > down - range && angle < down + range) {
                return false
            } else if (angle > up - range && angle < up + range) {
                return false
            }
            return true
        }

    override fun tick() {
        super.tick()
        if (canBlow) {
            timeLeft -= DELTA
            if (timeLeft <= 0.0) {
                explode()
            }
        }
    }

    override fun render(r: Renderer) {
        r.color = Color.RED.withAlpha(0.5)
        if (canBlow && timeLeft < totalTime / 4) {
            val length = lineLength * (1 - timeLeft / (totalTime / 4))
            r.line(
                body.p + Point(body.angle - PI / 2) * length,
                body.p - Point(body.angle - PI / 2) * length,
                0.05
            )
        }

        if (canBlow) {
            r.color = color
        } else {
            r.color = Color.GRAY
        }
        r.centeredRect(body.p, size, body.angle)
        r.centeredRect(
            body.p + Point(body.angle - PI / 2) * radius,
            cornerSize,
            body.angle + PI / 4
        )
        r.centeredRect(
            body.p - Point(body.angle - PI / 2) * radius,
            cornerSize,
            body.angle + PI / 4
        )
        r.color = Color.WHITE.withAlpha(0.6)
        r.arcFrom0(body.p, radius, timeLeft / totalTime)
    }

    override fun hitByExplosion() {
        timeLeft = min(0.5, totalTime)
    }

    override fun explode() {
        super.explode()
        val spacing = 0.5
        val distance = floor(lineLength / spacing)
        val motion = Point(body.angle - PI / 2) * 0.5
        val boomLoc = body.p - motion * distance
        for (i in -distance..distance) {
            boomLoc.x += motion.x
            boomLoc.y += motion.y
            boom(boomLoc, power)
        }
    }
}

// Tunnel out of dirt
// Small, medium and large circles
class TimedBomb(startingPoint: Point, bombType: BombType, val totalTime: Number = 6.0) :
    Bomb(startingPoint, bombType) {
    var timeLeft = totalTime

    init {
        circleShape(radius) {
            body.addFixture(bombFixtureDef(it))
        }
    }

    override fun hitByExplosion() {
        timeLeft = min(0.5, totalTime)
    }

    override fun tick() {
        super.tick()
        timeLeft -= DELTA
        if (timeLeft <= 0.0) {
            explode()
        }
    }

    override fun render(r: Renderer) {
        r.color = color
        r.circle(body.p, radius)
        r.color = Color.WHITE.withAlpha(0.6)
        r.arcFrom0(body.p, radius * 0.8, timeLeft / totalTime)
    }

    override fun explode() {
        super.explode()
        boom(body.p, power)
    }
}


// Spears/drills
// Question mark bomb?
// Big black hole?

class ClusterBomb(startingPoint: Point, val totalTime: Number = 6.0) :
    Bomb(startingPoint, BombType.ClusterBomb) {
    var timeLeft = totalTime

    init {
        ngonShape(radius, 6) {
            body.addFixture(bombFixtureDef(it))
        }
    }

    override fun hitByExplosion() {
        timeLeft = min(0.5, totalTime)
    }

    override fun tick() {
        super.tick()
        timeLeft -= DELTA
        if (timeLeft <= 0.0) {
            explode()
        }
    }

    override fun render(r: Renderer) {
        r.color = color
        r.ngon(body.p, radius, body.angle, 6)
        r.color = Color.WHITE.withAlpha(0.6)
        r.arcFrom0(body.p, radius * 0.8, timeLeft / totalTime)
        r.color = color.cpy().mul(0.5f)
        r.ngonLine(body.p, radius - 0.025, body.angle, 0.05, 6)
    }

    override fun explode() {
        super.explode()
        for (i in 0 until 6) {
            val bomb = ClusterParticle(body.p, 0.3 * Random.nextDouble() + 0.05)
            val velocity = Point(i * 2 * PI / 6)
            velocity.len = 6.0
            bomb.body.applyImpulse(velocity)
        }
    }
}

class ClusterParticle(startingPoint: Point, val totalTime: Number) :
    Bomb(startingPoint, BombType.ClusterParticle) {
    init {
        circleShape(radius) {
            val def = bombFixtureDef(it)
            def.filter.groupIndex = -5
            body.addFixture(def)
        }
    }

    override fun hitByExplosion() {}

    var timeLeft = totalTime
    override fun tick() {
        super.tick()
        timeLeft -= DELTA
        if (timeLeft <= 0.0) {
            explode()
        }
    }

    override fun explode() {
        super.explode()
        boom(body.p, power)
    }

    override fun render(r: Renderer) {
        r.color = color
        r.circle(body.p, radius)
    }
}

class StickyBomb(startingPoint: Point, val totalTime: Number = 6.0) :
    Bomb(startingPoint, BombType.StickyBomb) {
    val trapezoidHeight = 1.0
    private val trapezoid = listOf(
        Point(-radius, 0),
        Point(radius, 0),
        Point(radius * 0.4, -radius * trapezoidHeight),
        Point(-radius * 0.4, -radius * trapezoidHeight)
    )
    private val trapezoid2 = listOf(
        Point(-radius, 0),
        Point(radius, 0),
        Point(radius * 0.4, -radius * trapezoidHeight),
        Point(-radius * 0.4, -radius * trapezoidHeight)
    )
    private var typeToBe = BodyDef.BodyType.DynamicBody
    private val tilesTouching = mutableSetOf<Tile>()
    private var timeLeft = totalTime

    init {
        body.isFixedRotation = true
        val rect = PolygonShape()
        val points = trapezoid.toV2Array()
        rect.set(points)
        val fixtureDef = bombFixtureDef(rect)
        body.addFixture(fixtureDef)
        rect.dispose()

//        circleShape(radius) {
//            body.createFixture(bombFixtureDef(it))
//        }
    }

    override fun collided(yourFixture: Fixture, otherFixture: Fixture) {
        super.collided(yourFixture, otherFixture)
        val userData = otherFixture.body.userData
        if (userData is Tile) {
            typeToBe = BodyDef.BodyType.StaticBody
            tilesTouching.add(userData)
        }
    }

    override fun endCollide(yourFixture: Fixture, otherFixture: Fixture) {
        super.endCollide(yourFixture, otherFixture)
        val userData = otherFixture.body.userData
        if (userData is Tile) {
            typeToBe = BodyDef.BodyType.StaticBody
            tilesTouching.remove(userData)
        }
    }

    override fun tick() {
        super.tick()

        if (tilesTouching.none { it.exists }) {
            tilesTouching.clear()
            typeToBe = BodyDef.BodyType.DynamicBody
        }

        if (typeToBe != body.type) {
            body.type = typeToBe
        }

        if (body.type == BodyDef.BodyType.StaticBody) {
            timeLeft -= DELTA
        }

        if (timeLeft <= 0) {
            explode()
        }
    }

    override fun hitByExplosion() {
        if (body.type == BodyDef.BodyType.StaticBody) {
            timeLeft = min(0.5, timeLeft)
        }
    }

    override fun explode() {
        super.explode()
        boom(body.p, power)
    }

    override fun render(r: Renderer) {
        r.color = Color.GRAY
//        r.centeredRect(body.position.p + Point(radius/2, -radius/2-0.1), radius*0.6, radius*0.9, PI/6)
//        r.centeredRect(body.position.p - Point(radius/2, radius/2+0.1), radius*0.6, radius*0.9, -PI/6)

        if (body.type == BodyDef.BodyType.StaticBody) {
            r.color = color
        } else {
            r.color = Color.GRAY
        }
//        r.circle(body.position.p, radius)

        r.poly(trapezoid, body.p, body.angle)

        r.color = Color.WHITE.withAlpha(0.5)
        r.arcFrom0(body.p - Point(0, radius / 2), radius / 2, timeLeft / totalTime)

//        r.color = Color.WHITE.withAlpha(0.5)
//        val alpha = (timeLeft / totalTime)
//        trapezoid2[2].y = -(trapezoidHeight * radius * alpha).d
//        trapezoid2[2].x = (radius * (0.4 + (1-alpha)*(1-0.4))).d
//        trapezoid2[3].y = -(trapezoidHeight * radius * alpha).d
//        trapezoid2[3].x = (-radius * (0.4 + (1-alpha)*(1-0.4))).d
//
//        r.poly(trapezoid2, body.position.p, body.angle)
    }
}

// Timer for all the bombs (At top of screen)

// Bounce three times on head to shrink.
// Inverted triangles falling slowly
// Line/indicator
class ImpactBomb(startingPoint: Point) : Bomb(startingPoint, BombType.ImpactBomb) {
    private val trapezoidHeight = 1.0
    private val triangle = listOf(
        Point(-radius, 0),
        Point(radius, 0),
        Point(0, -radius * trapezoidHeight),
    )

    init {
        body.isFixedRotation = true
        val rect = PolygonShape()
        val points = triangle.toV2Array()
        rect.set(points)
        val fixtureDef = bombFixtureDef(rect)
        body.addFixture(fixtureDef)
        body.linearDamping = 1
        body.gravityScale = 0.2
        rect.dispose()
    }

    override fun explode() {
        super.explode()
        boom(body.p, power)
    }

    override fun collided(yourFixture: Fixture, otherFixture: Fixture) {
        super.collided(yourFixture, otherFixture)

        if (!destroyed) {
            explode()
        }
    }

    override fun render(r: Renderer) {
        r.color = color
        r.poly(triangle, body.p, 0)
    }
}

class SpringBomb(startingPoint: Point, val totalTime: Number = 10.0) :
    Bomb(startingPoint, BombType.SpringBomb) {
    var timeLeft = totalTime
    val springFrequency = 2.0
    var timeTillSpring = springFrequency
    var currentSpringDelay = springFrequency

    init {
        rectShape(size) {
            body.addFixture(bombFixtureDef(it))
        }
    }

    override fun explode() {
        super.explode()
        boom(body.p, power)
    }

    override fun tick() {
        super.tick()
        timeLeft -= DELTA
        timeTillSpring -= DELTA
        if (timeLeft <= 0.0) {
            explode()
        }
        if (timeTillSpring <= 0.0) {
            timeTillSpring += springFrequency * Random.nextDouble() * 0.5 + springFrequency * 0.5
            currentSpringDelay = timeTillSpring
            val velocity = Point(Random.nextFloat() - 0.5, 1.5f)
            velocity.len = 8.0 * body.mass
            body.applyImpulse(velocity)
        }
    }

    override fun hitByExplosion() {
        timeLeft = min(0.5, timeLeft)
    }

    override fun render(r: Renderer) {
        val bodyPos = body.p
        val overallFract = (timeLeft / totalTime)
        var springFract = (timeTillSpring / currentSpringDelay)
        if (timeTillSpring > timeLeft) {
            springFract = (timeLeft / currentSpringDelay).d
        }

        r.color = color
        r.circle(bodyPos, radius)
        r.color = Color.WHITE.withAlpha(0.5)
        r.arcFrom0(bodyPos, radius, springFract)
        r.color = color
        r.circle(bodyPos, radius * 0.7)
        r.color = Color.WHITE.withAlpha(0.5)
        r.arcFrom0(bodyPos, radius * 0.7, overallFract)
    }
}