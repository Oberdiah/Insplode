package com.oberdiah

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.Utils.TOUCHES_DOWN
import com.oberdiah.Utils.TOUCHES_WENT_DOWN
import com.oberdiah.Utils.TOUCHES_WENT_UP
import com.oberdiah.Utils.TileType
import com.oberdiah.Utils.colorScheme
import com.oberdiah.Utils.isKeyJustPressed
import com.oberdiah.Utils.isKeyPressed
import com.oberdiah.ui.switchScreen
import kotlin.math.pow
import kotlin.random.Random

private val PLAYER_SIZE = Size(0.375, 0.7) * GLOBAL_SCALE
private const val COYOTE_TIME = 0.2
private const val PLAYER_GRAVITY_MODIFIER = 0.5

/**
 * A duration in which the player cannot regain jump, to prevent them from regaining jump just after
 * a successful slam hit
 */
private const val JUMP_PREVENTION_WINDOW = 0.3
val player = Player(Point(5, PLAYER_SPAWN_Y))

class Player(startingPoint: Point) : PhysicsObject(startingPoint) {

    /** What we use to determine if we're on the ground or not. Narrower than the player */
    lateinit var jumpBox: Fixture

    /** What we use to determine if we've hit something on the way down or not. Wider than the player */
    lateinit var slamBox: Fixture

    /** If this is non-null we're in the air. If it's null we're on the ground. */
    private var airTime: Double? = 0.0

    // If this is non-null, then it is a tile that exists.
    private var tileBelowMe: Tile? = null
    private var isSlamming = true
    private var timeSinceLastSlamHit = 0.0

    private val inAir: Boolean
        get() = airTime != null

    private val isDead: Boolean
        get() = deadEndingCountdown != null

    private val isAlive: Boolean
        get() = !isDead

    /** If this countdown exists, we're dead. When this countdown goes below 0, we end the game */
    private var deadEndingCountdown: Double? = null

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

        rectShape(PLAYER_SIZE / Point(0.6, 1.5), Point(0f, -PLAYER_SIZE.x / 2) * GLOBAL_SCALE) {
            slamBox = addFixture(it, true)
        }
    }

    override fun hitByExplosion() {
        deadEndingCountdown = 2.5
        body.gravityScale = 0.0
        // Spawn a bunch of smoke in the shape of the player
        for (i in 0 until 100) {
            val pos = body.p + Point(
                Random.nextDouble(-PLAYER_SIZE.x / 2, PLAYER_SIZE.x / 2),
                Random.nextDouble(-PLAYER_SIZE.y / 2, PLAYER_SIZE.y / 2)
            )
            val vel = Velocity(Random.nextDouble(-0.5, 0.5), Random.nextDouble(-0.5, 0.5))
            spawnSmoke(pos, vel)
        }
    }

    override fun collided(yourFixture: Fixture, otherFixture: Fixture) {
        super.collided(yourFixture, otherFixture)

        if (isSlamming) {
            val obj = otherFixture.body.userData
            if (yourFixture == slamBox && obj is Bomb) {
                isSlamming = false
                CAMERA_LOCKED = false

                boom(obj.body.p, obj.power, affectsThePlayer = false)
                obj.destroy()
                val currentVel = body.velocity.y
                val desiredVel =
                    clamp((-body.velocity.y).pow(0.75) + obj.power * 2.0, 5.0, 15.0)
                val impulse = body.mass * (desiredVel - currentVel)
                body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)
                timeSinceLastSlamHit = 0.0
            }
            if (yourFixture == jumpBox && obj !is Bomb) {
                isSlamming = false
                CAMERA_LOCKED = false

                val vel = lastTickVelocity
                spawnParticlesAtMyFeet(
                    ferocity = vel.len.d * 0.2,
                    number = max((vel.len.d * 0.5).i, 2)
                )

                if (vel.len > 10) {
                    addScreenShake(vel.len.d * 0.02)
                    boom(body.p, vel.len.d * 0.05, affectsThePlayer = false)
                }
            }
        }

        if (yourFixture == jumpBox && timeSinceLastSlamHit > JUMP_PREVENTION_WINDOW) {
            airTime = null
        }
    }

    override fun reset() {
        body.setTransform(startingPoint, 0f)
        body.gravityScale = PLAYER_GRAVITY_MODIFIER
        airTime = 0.0
        deadEndingCountdown = null
        isSlamming = true
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
        if (isDead) {
            return
        }

        if (canJump()) {
            r.color = colorScheme.player
        } else if (isSlamming) {
            r.color = colorScheme.playerSlamming
        } else {
            r.color = colorScheme.playerNoJump
        }
        r.rect(body.p.x - PLAYER_SIZE.w / 2, body.p.y, PLAYER_SIZE.w, PLAYER_SIZE.h / 2)
        r.circle(body.p, PLAYER_SIZE.w / 2)
        r.circle(body.p.x, body.p.y + 0.35 * GLOBAL_SCALE, PLAYER_SIZE.w / 2)
    }

    private var lastXValue = 0.0
    private var lastBodyXValue = 0.0

    override fun tick() {
        lastTickVelocity = tickVelocity.cpy
        tickVelocity = body.velocity.cpy

        tileBelowMe = getTileBelowMe()
        timeSinceLastSlamHit += DELTA

        if (tileBelowMe == null && !inAir) {
            airTime = 0.0
        }

        deadEndingCountdown = deadEndingCountdown?.minus(DELTA)

        if ((deadEndingCountdown ?: 0.0) < 0.0) {
            PAUSED = true
            switchScreen(Screen.EndGame)
        }

        // If airTime is not null, add delta to it.
        airTime = airTime?.plus(DELTA)

        if (inAir && body.velocity.y < 1.0 && body.velocity.y > 0.0) {
            body.gravityScale = PLAYER_GRAVITY_MODIFIER * 0.5
        } else {
            body.gravityScale = PLAYER_GRAVITY_MODIFIER
        }

        if (isSlamming) {
            // Slam into the ground
            body.applyImpulse(Point(0f, -body.mass) * GLOBAL_SCALE)
        }

        if (isAlive) {
            playerControl()
        }
    }

    private fun playerControl() {
        val vel = body.velocity

        val acceleration = 2.5 * GLOBAL_SCALE
        var desiredXVel = 0.0
        var goLeft = false
        var goRight = false

        if (isActionButtonJustPressed() && canJump()) {
            doJump()
        } else if (isActionButtonJustPressed() && !canJump()) {
            isSlamming = true
        }

        TOUCHES_WENT_DOWN.forEach {
            lastXValue = it.x
            lastBodyXValue = body.p.x * SQUARE_SIZE_IN_PIXELS
        }

        TOUCHES_DOWN.forEach {
            if (it.y > HEIGHT * JUMP_UI_FRACT) {
                goLeft = goLeft || if (FOLLOW_FINGER) {
                    (lastBodyXValue + it.x - lastXValue) < (body.p.x - SIMPLE_SIZE_IN_WORLD) * SQUARE_SIZE_IN_PIXELS
                } else {
                    it.x < LEFT_BUTTON_UI_FRACT * WIDTH
                }

                goRight = goRight || if (FOLLOW_FINGER) {
                    (lastBodyXValue + it.x - lastXValue) > (body.p.x + SIMPLE_SIZE_IN_WORLD) * SQUARE_SIZE_IN_PIXELS
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
                tileBelowMe?.getTileType() ?: TileType.Air
            )
        }
    }

    private fun doJump() {
        // Prevent double jumping
        airTime = COYOTE_TIME
        val desiredUpVel = 9.0f
        val velChange = min(desiredUpVel - body.velocity.y, desiredUpVel)
        val impulse = body.mass * velChange
        body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)

        spawnParticlesAtMyFeet(number = 2)
    }

    fun canJump(): Boolean {
        return (airTime ?: 0.0) < COYOTE_TIME
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


    private fun getTileBelowMe(): Tile? {
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
                if (newTile is Tile && newTile.doesExist()) {
                    return newTile
                }
            }
        }

        return null
    }
}