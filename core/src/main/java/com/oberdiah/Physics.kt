package com.oberdiah

import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.level.Level

val collisionListener = ListenerClass()

class ListenerClass : ContactListener {
    override fun beginContact(contact: Contact) {
        val data = contact.fixtureA.body.userData
        val data2 = contact.fixtureB.body.userData

        if (data is PhysicsObject && !data.destroyed) {
            data.collided(contact.fixtureA, contact.fixtureB)
        }
        if (data2 is PhysicsObject && !data2.destroyed) {
            data2.collided(contact.fixtureB, contact.fixtureA)
        }
    }

    override fun endContact(contact: Contact) {
        val data = contact.fixtureA.body.userData
        val data2 = contact.fixtureB.body.userData

        if (data is PhysicsObject && !data.destroyed) {
            data.endCollide(contact.fixtureA, contact.fixtureB)
        }
        if (data2 is PhysicsObject && !data2.destroyed) {
            data2.endCollide(contact.fixtureB, contact.fixtureA)
        }
    }

    override fun preSolve(contact: Contact?, oldManifold: Manifold?) {
    }

    override fun postSolve(contact: Contact?, impulse: ContactImpulse?) {
    }
}

fun updateTilePhysics() {
    tileIdsChangedLastFrameMarchingCubes.mapNotNull { Level.getTile(it) as? Tile }
        .forEach { it.recalculatePhysicsBody() }
}

fun createWall(rect: Rect): PhysBody {
    val groundBodyDef = BodyDef()
    groundBodyDef.position.set((rect.p + rect.s / 2).v2)
    val groundBody = createBody(groundBodyDef)

    rectShape(rect.s) {
        groundBody.addFixture(it, 0.0)
    }

    return groundBody
}