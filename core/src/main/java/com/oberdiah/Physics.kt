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

fun resetPhysics() {
    directlyChangedTileIds.clear()
}

/**
 * This is the list of tiles that have changed in the last frame, directly or indirectly
 * (a neighbour changed and we may have to recompute our shape)
 */
var tileIdsChangedInTheLastFrame = listOf<TileId>()

/**
 * This is the list of tiles that have directly been changed in the last frame.
 * i.e they themselves have been destroyed or created.
 */
val directlyChangedTileIds = mutableSetOf<TileId>()
fun updateTilePhysics() {
    // Rebuild ourselves and all of our neighbors
    val tilesToRebuild = mutableSetOf<Tile>()
    tilesToRebuild.addAll(directlyChangedTileIds.mapNotNull { getTile(it) as? Tile })
    for (tileId in directlyChangedTileIds) {
        val tile = getTile(tileId) as? Tile ?: continue
        tile.marchingCubeNeighbors.forEach {
            if (it is Tile) {
                tilesToRebuild.add(it)
            }
        }
    }

    tileIdsChangedInTheLastFrame = tilesToRebuild.map(Tile::getId)
    directlyChangedTileIds.clear()

    for (tile in tilesToRebuild) {
        tile.recalculatePhysicsBody()
    }
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