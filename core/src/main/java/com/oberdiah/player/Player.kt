package com.oberdiah.player

import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.GAME_STATE
import com.oberdiah.GLOBAL_SCALE
import com.oberdiah.GameState
import com.oberdiah.PICKUP_PHYSICS_MASK
import com.oberdiah.PLAYER_DETECTOR_IDENTIFIER
import com.oberdiah.PLAYER_PHYSICS_MASK
import com.oberdiah.PhysicsObject
import com.oberdiah.Point
import com.oberdiah.Renderer
import com.oberdiah.ScoreSystem
import com.oberdiah.Tile
import com.oberdiah.UNITS_WIDE
import com.oberdiah.WORLD_PHYSICS_MASK
import com.oberdiah.circleShape
import com.oberdiah.level.LASER_HEIGHT
import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.player_state.PlayerStateImpl
import com.oberdiah.rectShape
import com.oberdiah.ui.PauseButton
import com.oberdiah.ui.goToDiegeticMenu
import com.oberdiah.utils.endTheGame
import kotlin.experimental.inv

class Player(startingPoint: Point) : PhysicsObject(startingPoint) {
    enum class DeathReason {
        Void,
        Bomb,
        Lava,
        QuitByChoice
    }

    /** Narrower than the player */
    lateinit var narrowFeetBox: Fixture
    lateinit var wideFeetBox: Fixture
    lateinit var bottomCircleFixture: Fixture
    lateinit var midRectFixture: Fixture
    lateinit var topCircleFixture: Fixture

    lateinit var topCircleDetector: Fixture
    lateinit var bottomCircleDetector: Fixture

    val state = PlayerStateImpl()

    init {
        circleShape(PLAYER_SIZE.w / 2) {
            bottomCircleFixture = addFixture(it)
            bottomCircleFixture.userData = PLAYER_DETECTOR_IDENTIFIER
            bottomCircleDetector = addFixture(it, true)
            bottomCircleDetector.userData = PLAYER_DETECTOR_IDENTIFIER
        }
        // Put the second circle up a circle diameter above the first
        circleShape(PLAYER_SIZE.w / 2, Point(0, PLAYER_SIZE.w)) {
            topCircleFixture = addFixture(it)
            topCircleFixture.userData = PLAYER_DETECTOR_IDENTIFIER
            topCircleDetector = addFixture(it, true)
            topCircleDetector.userData = PLAYER_DETECTOR_IDENTIFIER
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

    override fun collided(yourFixture: Fixture, otherFixture: Fixture) {
        super.collided(yourFixture, otherFixture)

        val userData = otherFixture.body.userData
        if (userData is Tile && !yourFixture.isSensor) {
            if (userData.getTileType().killsOnTouch) {
                hasDied(DeathReason.Lava)
            }
        }
    }

    override fun hitByExplosion() {
        if (!state.isSlamming) {
            hasDied(DeathReason.Bomb)
        }
    }

    private fun hasDied(reason: DeathReason) {
        if (!state.isDead) {
            state.justDied(reason)
        }
    }

    override fun reset() {
        body.setTransform(PLAYER_SPAWN_POINT, 0f)
        body.isFixedRotation = true
        body.velocity = Point(0.0, 0.0)
        body.linearDamping = 0.0

        PlayerInfoBoard.reset()
        state.reset()
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
            endTheGame(deathReason = state.deathReason ?: DeathReason.Void)
            return
        }
        if (GAME_STATE == GameState.DiegeticMenu) {
            return
        }

        PlayerInfoBoard.tick()
        state.tick()

        val playerLaserCheckHeight =
            if (PlayerInputs.currentPreparingAction == PlayerInputs.PreparingAction.Slam
            ) {
                player.body.p.y
            } else {
                player.body.p.y + PLAYER_SIZE.h / 2
            }

        if (playerLaserCheckHeight > LASER_HEIGHT && RUN_TIME_ELAPSED > 0.0) {
            hasDied(DeathReason.Void)
            return
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

        if (state.isAlive && !PauseButton.isEatingInputs()) {
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
                maskBits = PICKUP_PHYSICS_MASK.inv()
            }
        }

        bottomCircleFixture.filterData = filter
        midRectFixture.filterData = filter
        topCircleFixture.filterData = filter
    }
}