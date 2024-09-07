package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import kotlin.experimental.inv
import kotlin.random.Random

fun tickPhysicsObjects() {
    getAllPhysicsObjects.forEach { it.tick() }
    allDynamicPhysicsObjects.addAll(toAddPhysicsObjects)
    allDynamicPhysicsObjects.removeAll(deadPhysicsObjects)
    deadPhysicsObjects.forEach { it.body.destroy() }
    deadPhysicsObjects.clear()
    toAddPhysicsObjects.clear()
}

fun resetPhysicsObjects() {
    allDynamicPhysicsObjects.forEach { it.reset() }
    tickPhysicsObjects()
}

fun renderPhysicsObjects(r: Renderer) {
    getAllPhysicsObjects.filter { it !is Pickup }.forEach { it.render(r) }
    getAllPhysicsObjects.filterIsInstance<Pickup>().forEach { it.render(r) }
}

val allDynamicPhysicsObjects = mutableListOf<PhysicsObject>()

val numPhysicsObjects: Int
    get() = allDynamicPhysicsObjects.size - deadPhysicsObjects.size + toAddPhysicsObjects.size
val getAllPhysicsObjects = object : Iterable<PhysicsObject> {
    override fun iterator(): Iterator<PhysicsObject> {
        return object : Iterator<PhysicsObject> {
            var i = 0
            override fun hasNext(): Boolean {
                if (i >= allDynamicPhysicsObjects.size) return false
                if (!allDynamicPhysicsObjects[i].destroyed) return true
                var j = i
                while (j < allDynamicPhysicsObjects.size && allDynamicPhysicsObjects[j].destroyed) j++
                return j != allDynamicPhysicsObjects.size
            }
            override fun next(): PhysicsObject {
                while (i < allDynamicPhysicsObjects.size && allDynamicPhysicsObjects[i].destroyed) i++
                return allDynamicPhysicsObjects[i++]
            }
        }
    }
}

val deadPhysicsObjects = mutableSetOf<PhysicsObject>()
val toAddPhysicsObjects = mutableSetOf<PhysicsObject>()

abstract class PhysicsObject(val startingPoint: Point, val startingVelocity: Velocity = Velocity()) {
    var body: PhysBody
//    private val _p = Point()
//    val p: Point
//        get() {
//            _p.x = body.position.x.d
//            _p.x = body.position.y.d
//            return _p
//        }

    var destroyed = false

    init {
        val bodyDef = BodyDef()
        bodyDef.type = BodyDef.BodyType.DynamicBody
        bodyDef.position.set(startingPoint.x.f, startingPoint.y.f)
        body = createBody(bodyDef)
        body.applyImpulse(startingVelocity)
        body.userData = this
        toAddPhysicsObjects.add(this)
    }

    fun destroy() {
        deadPhysicsObjects.add(this)
        destroyed = true
    }

    open fun reset() {
        destroy()
    }

    open fun collided(yourFixture: Fixture, otherFixture: Fixture) {
        val data = otherFixture.body.userData
        if (data is PhysicsObject) {
            collided(data)
        }
    }

    open fun endCollide(yourFixture: Fixture, otherFixture: Fixture) {
        val data = otherFixture.body.userData
        if (data is PhysicsObject) {
            endCollide(data)
        }
    }

    open fun collided(obj: PhysicsObject) {

    }

    open fun endCollide(obj: PhysicsObject) {

    }

    open fun hitByExplosion() {

    }

    open fun tick() {
        if (body.p.x < 0 || body.p.x > WORLD_WIDTH) {
            destroy()
        }
    }

    abstract fun render(r: Renderer)
}

class Pickup(startingPoint: Point) : PhysicsObject(startingPoint) {
    var size = Size(0.3f, 0.3f)

    init {
        circleShape(0.15) {
            val fixtureDef = FixtureDef()
            fixtureDef.shape = it
            fixtureDef.density = 5.5f
            fixtureDef.friction = 0.5f
            fixtureDef.restitution = 0.4f
            fixtureDef.filter.categoryBits = PICKUP_PHYSICS_MASK
            fixtureDef.filter.maskBits = BOMB_PHYSICS_MASK.inv()
            body.addFixture(fixtureDef)
        }
    }

    override fun collided(obj: PhysicsObject) {
        super.collided(obj)
        if (obj == player) {
            destroy()
            for (i in 0..5) {
                spawnSmoke(body.p, createRandomFacingPoint())
            }
        }
    }

    override fun render(r: Renderer) {
        r.color = Color.ROYAL.cpy().mul(0.9f)
        r.circle(body.p, 0.15)
    }
}