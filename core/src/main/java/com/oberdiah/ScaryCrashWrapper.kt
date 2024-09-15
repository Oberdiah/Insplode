package com.oberdiah

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.Utils.MyBox2DDebugRenderer
import com.oberdiah.Utils.camera
import com.oberdiah.Utils.time

private lateinit var world: World
private lateinit var debugRenderer: Box2DDebugRenderer

private var accumulator = 0f

fun doPhysicsStep() {
    val frameTime = min(DELTA, 0.25f).f
    accumulator += frameTime

    world.step(AVERAGE_DELTA.f, 6, 2)
}

fun initWorld() {
    Box2D.init()
    world = World(Vector2(0f, -GRAVITY.f), true)
    world.setContactListener(collisionListener)
    debugRenderer = MyBox2DDebugRenderer()
}

fun physicsDebugString(): String {
    return "Physics Objects: $numPhysicsObjects Body Count: ${world.bodyCount}"
}

fun debugRenderWorld() {
    time("Physics debug") { debugRenderer.render(world, camera.combined) }
}

private val allPhysBodies = mutableSetOf<PhysBody>()
fun createBody(def: BodyDef, shouldUpdate: Boolean = true): PhysBody {
    val physBody = PhysBody(world.createBody(def), shouldUpdate)
    allPhysBodies.add(physBody)
    return physBody
}

fun tickPhysicsWrapper() {
    allPhysBodies.forEach { it.updateInternals() }
}

class PhysBody(private val body: Body, val shouldUpdate: Boolean = true) {
    val p = Point()
    val velocity = Point()
    private var exists = true
    private var fixtures = mutableListOf<Fixture>()

    init {
        updateInternals()
    }

    var userData: Any
        get() = body.userData
        set(value) {
            body.userData = value
        }

    private var _angle = 0.0
    val angle
        get() = _angle

    var isFixedRotation: Boolean
        get() = body.isFixedRotation
        set(value) {
            require(exists)
            body.isFixedRotation = value
        }

    var type: BodyDef.BodyType
        get() = body.type
        set(value) {
            require(exists)
            body.type = value
        }

    var gravityScale: Number
        get() = body.gravityScale
        set(value) {
            require(exists)
            body.gravityScale = value.f
        }

    var linearDamping: Number
        get() = body.linearDamping.d
        set(value) {
            require(exists)
            body.linearDamping = value.f
        }

    val mass: Double
        get() = body.mass.d

    fun applyImpulse(velocity: Point, point: Point = p) {
        require(exists)
        body.applyLinearImpulse(velocity.v2, point.v2, true)
    }

    fun setTransform(p: Point, angle: Number) {
        require(exists)
        body.setTransform(p.v2, angle.f)
    }

    fun removeAllFixtures() {
        require(exists)
        for (fixture in fixtures) {
            body.destroyFixture(fixture)
        }
        fixtures.clear()
    }

    fun addFixture(fixtureDef: FixtureDef): Fixture {
        require(exists)
        val fixture = body.createFixture(fixtureDef)
        fixtures.add(fixture)
        return fixture
    }

    fun addFixture(shape: Shape, density: Number) {
        require(exists)
        fixtures.add(body.createFixture(shape, density.f))
    }

    fun updateInternals() {
        if (!shouldUpdate) {
            return
        }
        require(exists)
        val pos = body.position
        p.x = pos.x.d
        p.y = pos.y.d
        val vel = body.linearVelocity
        velocity.x = vel.x.d
        velocity.y = vel.y.d
        _angle = body.angle.d
    }

    fun destroy() {
        require(exists)
        exists = false
        world.destroyBody(body)
        allPhysBodies.remove(this)
    }
}