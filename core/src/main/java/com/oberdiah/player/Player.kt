package com.oberdiah.player

import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.PAUSED
import com.oberdiah.PLAYER_PHYSICS_MASK
import com.oberdiah.PhysicsObject
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.Screen
import com.oberdiah.WORLD_PHYSICS_MASK
import com.oberdiah.abs
import com.oberdiah.circleShape
import com.oberdiah.div
import com.oberdiah.rectShape
import com.oberdiah.registerGameEndWithScoreSystem
import com.oberdiah.ui.switchScreen
import com.oberdiah.unaryMinus

class Player(startingPoint: Point) : PhysicsObject(startingPoint) {
    /** Narrower than the player */
    lateinit var narrowFeetBox: Fixture
    lateinit var wideFeetBox: Fixture
    lateinit var bottomCircleFixture: Fixture
    lateinit var midRectFixture: Fixture
    lateinit var topCircleFixture: Fixture

    init {
        circleShape(PLAYER_SIZE.w / 2) {
            bottomCircleFixture = addFixture(it)
        }
        // Put the second circle up a circle diameter above the first
        circleShape(PLAYER_SIZE.w / 2, Point(0, PLAYER_SIZE.w)) {
            topCircleFixture = addFixture(it)
        }

        rectShape(PLAYER_SIZE / Point(1.6, 2), Point(0, PLAYER_SIZE.x / 2) * GLOBAL_SCALE) {
            midRectFixture = addFixture(it)
        }

        rectShape(PLAYER_SIZE / Point(1.25, 4), Point(0f, -PLAYER_SIZE.w / 2)) {
            narrowFeetBox = addFixture(it, true)
        }

        rectShape(PLAYER_SIZE / Point(0.5, 1.2), Point(0f, -PLAYER_SIZE.w / 2)) {
            wideFeetBox = addFixture(it, true)
        }

        reset()
    }

    override fun hitByExplosion() {
        if (!playerState.isDead) {
            playerState.justDied()
        }
    }

    override fun reset() {
        body.setTransform(startingPoint, 0f)
        body.isFixedRotation = true
        body.gravityScale = PLAYER_GRAVITY_MODIFIER
        body.velocity = Point(0.0, 0.0)
        body.linearDamping = 0.0

        playerInfoBoard.reset()
        playerState.reset()
        playerInputs.reset()
    }

    private fun addFixture(shape: Shape, isSensor: Boolean = false): Fixture {
        val fixtureDef = FixtureDef()
        fixtureDef.shape = shape
        fixtureDef.density = 5.0f
        fixtureDef.friction = 0.0f
        fixtureDef.restitution = 0.0f
        fixtureDef.filter.categoryBits = PLAYER_PHYSICS_MASK
        fixtureDef.isSensor = isSensor
        return body.addFixture(fixtureDef)
    }

    override fun render(r: Renderer) {
        if (playerState.isAlive) {
            playerRenderer.render(r)
        }
    }

    override fun tick() {
        if (playerState.timeSinceDied > 2.5) {
            PAUSED = true
            registerGameEndWithScoreSystem()
            switchScreen(Screen.EndGame)
            return
        }

        playerInfoBoard.tick()
        playerState.tick()


        if (playerState.isSlamming) {
            if (playerInfoBoard.bombsStandingOnGenerous.isNotEmpty()) {
                playerState.justSlammedIntoABomb(playerInfoBoard.bombsStandingOnGenerous.first())
            } else if (playerInfoBoard.isStandingOnNotBombExact) {
                playerState.justSlammedIntoTheGround()
            }
        } else if (playerState.isIntentionallyMovingUp) {
            if (playerInfoBoard.isStandingOnStandableExact) {
                if (playerState.timeSinceStartedIntentionallyMovingUp > JUMP_PREVENTION_WINDOW) {
                    playerState.justCasuallyLanded()
                }
            }
        }

        if (playerState.isIntentionallyMovingUp && body.velocity.y < 1.0 && body.velocity.y > 0.0) {
            body.gravityScale = PLAYER_GRAVITY_MODIFIER * 0.5
        } else if (playerState.isSlamming) {
            body.gravityScale = PLAYER_GRAVITY_MODIFIER * 8.0
        } else {
            body.gravityScale = PLAYER_GRAVITY_MODIFIER
        }

        if (playerState.isAlive) {
            playerInputs.tick()
        }
    }

    fun setGhosting(ghosting: Boolean) {
        val filter = if (ghosting) {
            Filter().apply {
                categoryBits = PLAYER_PHYSICS_MASK
                // Only collide with tiles
                maskBits = WORLD_PHYSICS_MASK
            }
        } else {
            Filter().apply {
                categoryBits = PLAYER_PHYSICS_MASK
                maskBits = 0xFFFF.toShort()
            }
        }

        bottomCircleFixture.filterData = filter
        midRectFixture.filterData = filter
        topCircleFixture.filterData = filter
    }
}