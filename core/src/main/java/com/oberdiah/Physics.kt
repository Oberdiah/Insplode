package com.oberdiah

import com.badlogic.gdx.physics.box2d.*

val collisionListener = ListenerClass()

class ListenerClass : ContactListener {
    override fun beginContact(contact: Contact) {
        val data = contact.fixtureA.body.userData
        val data2 = contact.fixtureB.body.userData

        if (data is PhysicsObject) {
            data.collided(contact.fixtureA, contact.fixtureB)
        }
        if (data2 is PhysicsObject) {
            data2.collided(contact.fixtureB, contact.fixtureA)
        }
    }

    override fun endContact(contact: Contact) {
        val data = contact.fixtureA.body.userData
        val data2 = contact.fixtureB.body.userData

        if (data is PhysicsObject) {
            data.endCollide(contact.fixtureA, contact.fixtureB)
        }
        if (data2 is PhysicsObject) {
            data2.endCollide(contact.fixtureB, contact.fixtureA)
        }
    }

    override fun preSolve(contact: Contact?, oldManifold: Manifold?) {
    }

    override fun postSolve(contact: Contact?, impulse: ContactImpulse?) {
    }
}

fun updateTilePhysics() {
    tileIdsChangedLastFrame.mapNotNull { getTile(it) as? Tile }
        .forEach { it.recalculatePhysicsBody() }
}

fun createWall(rect: Rect): PhysBody {
    val groundBodyDef = BodyDef()
    groundBodyDef.position.set((rect.p + rect.s / 2).v2)
    val groundBody = createBody(groundBodyDef)

    rectShape(rect.s) {
        groundBody.addFixture(it, 0.0f)
    }

    return groundBody
}