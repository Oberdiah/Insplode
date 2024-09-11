package com.oberdiah

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.Utils.TOUCHES_DOWN
import com.oberdiah.Utils.TOUCHES_WENT_UP
import com.oberdiah.Utils.colorScheme
import com.oberdiah.Utils.isKeyJustPressed
import com.oberdiah.Utils.isKeyPressed
import com.oberdiah.ui.switchScreen
import kotlin.random.Random

private val PLAYER_SIZE = Size(0.4, 0.7) * GLOBAL_SCALE
private const val COYOTE_TIME = 0.1
private const val PLAYER_GRAVITY_MODIFIER = 0.5

/** A duration in which the player cannot slam, to prevent them from immediately heading down again after hitting something */
private const val SLAM_PREVENTION_WINDOW = 0.3
val player = Player(Point(5, PLAYER_SPAWN_Y))

class Player(startingPoint: Point) : PhysicsObject(startingPoint) {

    /** What we use to determine if we're on the ground or not. Narrower than the player */
    lateinit var jumpBox: Fixture

    /** What we use to determine if we've hit something on the way down or not. Wider than the player */
    lateinit var slamBox: Fixture

    /**
     * We need this because slamming is an action that only requires a button to be held, not pressed
     * So if a player holds the button to jump, we can't immediately transition to slamming, we need
     * to wait for the player to release the button first
     */
    private var willSlamInsteadOfJump = false

    private var airTime = 0.0
    private var inAir = true
    private var timeSinceLastSlam = 10.0
    private var tileBelowMe: Tile = nonTile
    private var health = 3
    private var justSpawned = true

    private val numHealthDots = 3
    private val feetPosition
        get() = body.p - Point(0.0, PLAYER_SIZE.w / 2 * GLOBAL_SCALE)

    /// We need these because sometimes on collide the velocity is already really small for some reason
    private var tickVelocity = Point(0.0, 0.0)
    private var lastTickVelocity = Point(0.0, 0.0)

    init {
        body.isFixedRotation = true
        body.gravityScale = PLAYER_GRAVITY_MODIFIER

        circleShape(PLAYER_SIZE.w / 2) {
            addFixture(it)
        }
        circleShape(PLAYER_SIZE.w / 2, Point(0, PLAYER_SIZE.y / 2) * GLOBAL_SCALE) {
            addFixture(it)
        }

        rectShape(PLAYER_SIZE / Point(1, 2), Point(0, PLAYER_SIZE.x / 2) * GLOBAL_SCALE) {
            addFixture(it)
        }

        rectShape(PLAYER_SIZE / Point(1.25, 4), Point(0f, -PLAYER_SIZE.x / 2) * GLOBAL_SCALE) {
            jumpBox = addFixture(it, true)
        }

        rectShape(PLAYER_SIZE / Point(0.8, 2), Point(0f, -PLAYER_SIZE.x / 2) * GLOBAL_SCALE) {
            slamBox = addFixture(it, true)
        }
    }

    override fun hitByExplosion() {
        health--

        if (health <= 0) {
            PAUSED = true
            switchScreen(Screen.EndGame)
        }
    }

    override fun collided(yourFixture: Fixture, otherFixture: Fixture) {
        super.collided(yourFixture, otherFixture)

        if (yourFixture == slamBox) {
            if (isSlamming()) {
                justSpawned = false
                CAMERA_LOCKED = false

                val obj = otherFixture.body.userData
                if (obj is Bomb) {
                    boom(obj.body.p, obj.power, hurtsThePlayer = false)
                    obj.destroy()
                    val currentVel = body.velocity.y
                    val desiredVel = 10.0
                    val impulse = body.mass * (desiredVel - currentVel)
                    body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)
                    timeSinceLastSlam = 0.0
                } else {
                    val vel = lastTickVelocity
                    spawnParticlesAtMyFeet(
                        ferocity = vel.len.d * 0.2,
                        number = max((vel.len.d * 0.5).i, 2)
                    )

                    if (vel.len > 10) {
                        addScreenShake(vel.len.d * 0.02)
                        boom(body.p, vel.len.d * 0.05, hurtsThePlayer = false)
                    }
                }
            }
        }

        if (yourFixture == jumpBox) {
            // The only way for inAir to become false in-game.
            inAir = false
        }
    }

    override fun reset() {
        body.setTransform(startingPoint, 0f)
        inAir = true
        justSpawned = true
        health = numHealthDots
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
        if (canJump()) {
            r.color = colorScheme.player
        } else if (isSlamming()) {
            r.color = colorScheme.playerSlamming
        } else {
            r.color = colorScheme.playerNoJump
        }
        r.rect(body.p.x - PLAYER_SIZE.w / 2, body.p.y, PLAYER_SIZE.w, PLAYER_SIZE.h / 2)
        r.circle(body.p, PLAYER_SIZE.w / 2)
        r.circle(body.p.x, body.p.y + 0.35 * GLOBAL_SCALE, PLAYER_SIZE.w / 2)

        for (i in 0 until numHealthDots) {
            if (i < health) {
                r.color = Color.WHITE
            } else {
                r.color = Color.DARK_GRAY
            }
            r.circle(
                body.p.x,
                body.p.y + (0.35 * GLOBAL_SCALE / (numHealthDots - 1)) * i,
                PLAYER_SIZE.w / 5
            )
        }
    }

    override fun tick() {
        lastTickVelocity = tickVelocity.cpy
        tickVelocity = body.velocity.cpy

        timeSinceLastSlam += DELTA

        val vel = body.velocity

        tileBelowMe = getTileBelowMe()

        if (!tileBelowMe.exists) {
            inAir = true
        }

        if (inAir) {
            airTime += DELTA
        } else {
            airTime = 0.0
            willSlamInsteadOfJump = false
        }

        if (isSlamming()) {
            // Slam into the ground
            body.applyImpulse(Point(0f, -body.mass) * GLOBAL_SCALE)
        }

        if (isActionButtonJustPressed() && canJump()) {
            doJump()
        }
        if (!isActionButtonPressed() && !canJump()) {
            willSlamInsteadOfJump = true
        }

        val acceleration = 2.5 * GLOBAL_SCALE
        var desiredXVel = 0.0
        var goLeft = false
        var goRight = false
        TOUCHES_DOWN.forEach {
            if (it.y > HEIGHT * JUMP_UI_FRACT) {
                goLeft = goLeft || if (FOLLOW_FINGER) {
                    it.x < body.p.x * (SQUARE_SIZE_IN_PIXELS - 1)
                } else {
                    it.x < LEFT_BUTTON_UI_FRACT * WIDTH
                }

                goRight = goRight || if (FOLLOW_FINGER) {
                    it.x > body.p.x * (SQUARE_SIZE_IN_PIXELS + 1)
                } else {
                    it.x > RIGHT_BUTTON_UI_FRACT * WIDTH
                }
            }
        }

        // If on Desktop, read A/D and Left/Right arrow keys
        if (isKeyPressed(Keys.A) || isKeyPressed(Keys.LEFT)) {
            goLeft = true
        }
        if (isKeyPressed(Keys.D) || isKeyPressed(Keys.RIGHT)) {
            goRight = true
        }

        if (goLeft && goRight) {
            // Do nothing
        } else if (goLeft) {
            desiredXVel = max(-5 * GLOBAL_SCALE, vel.x - acceleration)
        } else if (goRight) {
            desiredXVel = min(5 * GLOBAL_SCALE, vel.x + acceleration)
        }

        val velChange = desiredXVel - vel.x

        // If velChange is large enough, spawn particles to simulate kicked up dirt in the direction of movement
        if (!inAir) {
            if (velChange > 3 || velChange < -3) {
                spawnParticlesAtMyFeet(number = 3, addedVelocity = Point(velChange * 0.5, 0.0))
            }
        }

        val impulse = body.mass * velChange
        body.applyImpulse(Point(impulse.f, 0))
    }

    private fun spawnParticlesAtMyFeet(
        addedVelocity: Point = Point(0.0, 0.0),
        number: Int = 5,
        ferocity: Double = 2.0
    ) {
        val posToSpawn = body.p - Point(0.0, 0.15 * GLOBAL_SCALE)

        for (i in 0 until number) {
            spawnFragment(
                posToSpawn.cpy,
                addedVelocity + Velocity(
                    ferocity * (Random.nextDouble() - 0.5),
                    ferocity * Random.nextDouble()
                ),
                tileBelowMe.tileType
            )
        }
    }

    private fun doJump() {
        // Prevent double jumping
        airTime += COYOTE_TIME
        inAir = true
        val desiredUpVel = 9.0f
        val velChange = min(desiredUpVel - body.velocity.y, desiredUpVel)
        val impulse = body.mass * velChange
        body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)

        spawnParticlesAtMyFeet(number = 2)
    }

    fun canJump(): Boolean {
        // Coyote time
        return airTime < COYOTE_TIME
    }

    private fun isSlamming(): Boolean {
        if (justSpawned) {
            return true
        }

        return isActionButtonPressed() && willSlamInsteadOfJump && timeSinceLastSlam > SLAM_PREVENTION_WINDOW
    }

    private fun isActionButtonPressed(): Boolean {
        // If on desktop, we're slamming if space is held and we cannot jump
        return if (Gdx.app.type == Application.ApplicationType.Desktop) {
            isKeyPressed(Keys.SPACE) || isKeyPressed(Keys.W) || isKeyPressed(Keys.UP)
        } else {
            // If on mobile, we're doing an action if there are no touches on the screen and we cannot jump
            TOUCHES_DOWN.isEmpty()
        }
    }

    private fun isActionButtonJustPressed(): Boolean {
        return isActionButtonPressed() && if (Gdx.app.type == Application.ApplicationType.Desktop) {
            isKeyJustPressed(Keys.SPACE) || isKeyJustPressed(Keys.W) || isKeyJustPressed(Keys.UP)
        } else {
            TOUCHES_WENT_UP.isNotEmpty()
        }
    }

    private fun getTileBelowMe(): Tile {
        // We effectively need to check in a 3x3 grid below the player

        // Top middle of the search grid
        val searchStartPos = feetPosition

        for (x in intArrayOf(0, 1, -1)) {
            for (y in 0 downTo -2) {
                val pos = searchStartPos + Point(
                    x * SIMPLE_SIZE_IN_WORLD * 0.5,
                    y * SIMPLE_SIZE_IN_WORLD * 0.5
                )
                val newTile = getTile(pos)
                if (newTile.exists) {
                    return newTile
                }
            }
        }

        return nonTile
    }
}