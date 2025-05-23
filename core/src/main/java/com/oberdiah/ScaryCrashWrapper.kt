package com.oberdiah

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.MyBox2DDebugRenderer
import com.oberdiah.utils.camera
import com.oberdiah.utils.time

private lateinit var world: World
private lateinit var debugRenderer: Box2DDebugRenderer

fun doPhysicsStep() {
    // The average is calculated before the game speed is applied, so there's
    // no weird smoothing to worry about.
    // We do the whole min thing so we can deal with lag spikes sorta-nicely even if it's
    // not technically correct.
    val delta = min(GameTime.AVERAGE_GAMEPLAY_DELTA, GameTime.GAME_SPEED / 60.0)
    world.step(delta.f, 6, 2)
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

fun whatAmITouching(fixtures: List<Fixture>): Set<Any> {
    val bodies = mutableListOf<Any?>()
    for (contact in world.contactList) {
        if (!contact.isTouching) continue
        if (!contact.isEnabled) continue
        if (fixtures.contains(contact.fixtureA)) {
            bodies.add(contact.fixtureB.body.userData)
        }
        if (fixtures.contains(contact.fixtureB)) {
            bodies.add(contact.fixtureA.body.userData)
        }
    }

    return bodies.filterNotNull().toSet()
}

fun forAllContacts(action: (Fixture, Fixture) -> Unit) {
    for (contact in world.contactList) {
        if (!contact.isTouching) continue
        if (!contact.isEnabled) continue
        action(contact.fixtureA, contact.fixtureB)
    }
}

fun isRectEmptySpace(rect: Rect): Boolean {
    var isEmpty = true
    world.QueryAABB(
        { fixture ->
            isEmpty = false
            false
        },
        rect.p.x.f, rect.p.y.f, rect.x.f + rect.w.f, rect.y.f + rect.h.f
    )
    return isEmpty
}

fun tickPhysicsWrapper() {
    allPhysBodies.forEach { it.updateInternals() }
}

class PhysBody(private val body: Body, private val shouldUpdate: Boolean = true) {
    val p: Point
        get() {
            require(exists)
            return Point(body.position.x, body.position.y)
        }

    var velocity: Point
        get() {
            require(exists)
            return Point(body.linearVelocity.x, body.linearVelocity.y)
        }
        set(value) {
            require(exists)
            body.linearVelocity = value.v2
        }

    var angularVelocity: Double
        get() = body.angularVelocity.d
        set(value) {
            require(exists)
            body.angularVelocity = value.f
        }

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

    val angle
        get() = body.angle.d


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

    var gravityScale: Double
        get() = body.gravityScale.d
        set(value) {
            require(exists)
            body.gravityScale = value.f
        }

    var linearDamping: Double
        get() = body.linearDamping.d
        set(value) {
            require(exists)
            body.linearDamping = value.f
        }

    val mass: Double
        get() = body.mass.d

    fun applyImpulse(velocity: Point, point: Point = p) {
        require(exists)
        require(velocity.x.f.isFinite()) { "velocity.x is not finite: ${velocity.x}" }
        require(velocity.y.f.isFinite()) { "velocity.y is not finite: ${velocity.y}" }
        require(point.x.f.isFinite()) { "point.x is not finite: ${point.x}" }
        require(point.y.f.isFinite()) { "point.y is not finite: ${point.y}" }
        body.applyLinearImpulse(velocity.v2, point.v2, true)
    }

    fun setTransform(p: Point, angle: Double) {
        require(exists)
        require(p.x.f.isFinite())
        require(p.y.f.isFinite())
        require(angle.f.isFinite())
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

    fun addFixture(shape: Shape, density: Double) {
        require(exists)
        fixtures.add(body.createFixture(shape, density.f))
    }

    internal fun updateInternals() {
        if (!shouldUpdate) {
            return
        }
        require(exists)
    }

    fun destroy() {
        require(exists)
        exists = false
        world.destroyBody(body)
        allPhysBodies.remove(this)
    }
}