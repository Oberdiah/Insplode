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
    changedTiles.clear()
}

val changedTiles = mutableSetOf<Tile>()
fun updateTilePhysics() {
    val tilesToRebuild = mutableSetOf<Tile>()
    tilesToRebuild.addAll(changedTiles)
    for (tile in changedTiles) {
        if (tile == nonTile) continue
        tilesToRebuild.addAll(tile.marchingCubeNeighbors)
    }
    tilesToRebuild.remove(nonTile)

    tilesChangedThisFrame = tilesToRebuild.toList()

    changedTiles.clear()

    for (tile in tilesToRebuild) {
        tile.body.removeAllFixtures()
    }

    for (tile in tilesToRebuild) {
        val bottomLeft = tile
        val topLeft = tile.data.tm
        val bottomRight = tile.data.rm
        val topRight = tile.data.tr

        val bl = bottomLeft.exists
        val tl = topLeft.exists
        val br = bottomRight.exists
        val tr = topRight.exists
        if (bl || br || tl || tr) {
            val fa = marchingSquaresFAScaled(bl, br, tl, tr)
            val groundBox = PolygonShape()
            groundBox.set(fa)
            tile.body.addFixture(groundBox, 0.0f)
            groundBox.dispose()
        }
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