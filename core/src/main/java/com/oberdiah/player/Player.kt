package com.oberdiah.player

import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.GAME_STATE
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.GameState
import com.oberdiah.PLAYER_PHYSICS_MASK
import com.oberdiah.PhysicsObject
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.ScoreSystem
import com.oberdiah.UNITS_WIDE
import com.oberdiah.WORLD_PHYSICS_MASK
import com.oberdiah.circleShape
import com.oberdiah.level.LASER_HEIGHT
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.player_state.PlayerStateImpl
import com.oberdiah.rectShape
import com.oberdiah.ui.goToDiegeticMenu
import com.oberdiah.ui.pauseHovered
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.upgrades.UpgradeController

class Player(startingPoint: Point) : PhysicsObject(startingPoint) {
    /** Narrower than the player */
    lateinit var narrowFeetBox: Fixture
    lateinit var wideFeetBox: Fixture
    lateinit var bottomCircleFixture: Fixture
    lateinit var midRectFixture: Fixture
    lateinit var topCircleFixture: Fixture
    val state = PlayerStateImpl()

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
        if (!state.isDead) {
            state.justDied()
        }
    }

    override fun reset() {
        body.setTransform(Point(UNITS_WIDE / 2, PLAYER_SPAWN_Y), 0f)
        body.isFixedRotation = true
        body.velocity = Point(0.0, 0.0)
        body.linearDamping = 0.0

        PlayerInfoBoard.reset()
        state.reset()
        PlayerInputs.reset()
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
        if (state.isAlive && GAME_STATE != GameState.DiegeticMenu) {
            PlayerRenderer.render(r)
        }
    }

    override fun tick() {
        if (state.timeSinceDied > DEAD_CONTEMPLATION_TIME && GAME_STATE == GameState.InGame) {
            goToDiegeticMenu()
            ScoreSystem.registerGameEnd()
            return
        }
        if (GAME_STATE == GameState.DiegeticMenu) {
            return
        }


        PlayerInfoBoard.tick()
        state.tick()

        val playerLaserCheckHeight = if (UpgradeController.playerHas(Upgrade.Slam)) {
            player.body.p.y - PLAYER_SIZE.h / 2
        } else {
            player.body.p.y + PLAYER_SIZE.h / 2
        }

        if (playerLaserCheckHeight > LASER_HEIGHT && RUN_TIME_ELAPSED > 5.0) {
            state.justDied()
        }

        if (state.isSlamming) {
            if (PlayerInfoBoard.bombsStandingOnGenerous.isNotEmpty()) {
                val bomb = PlayerInfoBoard.bombsStandingOnGenerous.minByOrNull {
                    it.body.p.distTo(PlayerInfoBoard.playerFeetPosition)
                }!!
                state.justSlammedIntoABomb(bomb)
            } else if (PlayerInfoBoard.isStandingOnNotBombExact) {
                state.justSlammedIntoTheGround()
            }
        } else if (state.isIntentionallyMovingUp) {
            if (PlayerInfoBoard.isStandingOnStandableExact) {
                if (state.timeSinceStartedIntentionallyMovingUp > JUMP_PREVENTION_WINDOW) {
                    // You cannot land if you're not moving down
                    if (PlayerInfoBoard.velocity.y < 0) {
                        state.justCasuallyLanded()
                    }
                }
            }
        }

        body.gravityScale = PlayerInfoBoard.currentGravity

        if (state.isAlive && !pauseHovered) {
            PlayerInputs.tick()
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