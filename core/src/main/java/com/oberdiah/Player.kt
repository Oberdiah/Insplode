package com.oberdiah

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.utils.TOUCHES_DOWN
import com.oberdiah.utils.TOUCHES_WENT_DOWN
import com.oberdiah.utils.TOUCHES_WENT_UP
import com.oberdiah.utils.TileType
import com.oberdiah.utils.addScreenShake
import com.oberdiah.utils.colorScheme
import com.oberdiah.utils.isKeyJustPressed
import com.oberdiah.utils.isKeyPressed
import com.oberdiah.ui.switchScreen
import kotlin.math.pow
import kotlin.random.Random

val PLAYER_SIZE = Size(0.375, 0.7) * GLOBAL_SCALE
private const val COYOTE_TIME = 0.15
private const val PLAYER_GRAVITY_MODIFIER = 0.5
private const val JUMP_BUILD_UP_TIME = 1.0

/** The x-zone in which the player will no longer be moved closer to where they want to be */
private const val PLAYER_UNCERTAINTY_WINDOW = TILE_SIZE_IN_UNITS * 0.5

/** Below this, slams don't happen */
private const val MINIMUM_SLAM_VELOCITY = 5.0

/** This is in units */
private const val PLAYER_FINGER_DRAG_DISTANCE = 0.2

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
    lateinit var bottomCircleFixture: Fixture
    lateinit var topCircleFixture: Fixture

    /** If this is non-null we're in the air. If it's null we're on the ground. */
    private var airTime: Double? = 0.0

    // If this is non-null, then it is a tile that exists.
    private var tileBelowMe: Tile? = null
    private var isSlamming = true
    private var timeSinceLastJumpOrSlam = 0.0

    private val inAir: Boolean
        get() = airTime != null

    private val onGround: Boolean
        get() = airTime == null

    val isDead: Boolean
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
        circleShape(PLAYER_SIZE.w / 2) {
            bottomCircleFixture = addFixture(it)
        }
        // Put the second circle up a circle diameter above the first
        circleShape(PLAYER_SIZE.w / 2, Point(0, PLAYER_SIZE.w)) {
            topCircleFixture = addFixture(it)
        }

//        rectShape(PLAYER_SIZE / Point(1, 2), Point(0, PLAYER_SIZE.x / 2) * GLOBAL_SCALE) {
//            addFixture(it)
//        }

        rectShape(PLAYER_SIZE / Point(1.25, 4), Point(0f, -PLAYER_SIZE.w / 2)) {
            jumpBox = addFixture(it, true)
        }

        rectShape(PLAYER_SIZE / Point(0.6, 1.5), Point(0f, -PLAYER_SIZE.w / 2)) {
            slamBox = addFixture(it, true)
        }

        reset()
    }

    override fun hitByExplosion() {
        if (isDead) {
            return
        }

        deadEndingCountdown = 2.5
        body.linearDamping = Float.MAX_VALUE
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

        val obj = otherFixture.body.userData

        if (obj is Bomb) {
            if (isSlamming) {
                finishSlamHitBomb(obj)
            }
        }
        if (yourFixture == jumpBox && obj is Tile) {
            if (isSlamming) {
                finishSlamHitGround()
            }
        }

        if (yourFixture == jumpBox) {
            landOnGround()
        }
    }

    private fun landOnGround() {
        // We can't 'land on ground' in the normal sense if we're slamming,
        // and we don't want to land if we've just slammed or jumped as we're clearly heading up and this
        // will be a mistake.
        if (timeSinceLastJumpOrSlam > JUMP_PREVENTION_WINDOW && !isSlamming && inAir) {
            // You're back on the ground
            airTime = null
            registerLandedOnGroundWithScoreSystem()
        }
    }

    private fun finishSlamHitBomb(hitBomb: Bomb) {
        isSlamming = false
        CAMERA_LOCKED = false

        if (abs(lastTickVelocity.y) > MINIMUM_SLAM_VELOCITY) {
            boom(hitBomb.body.p, hitBomb.power, affectsThePlayer = false)
            hitBomb.destroy()
            val currentVel = body.velocity.y
            val desiredVel =
                clamp(abs(body.velocity.y).pow(0.75) + hitBomb.power * 2.0, 5.0, 15.0)
            val impulse = body.mass * (desiredVel - currentVel)
            body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)
            timeSinceLastJumpOrSlam = 0.0
            registerBombSlamWithScoreSystem(hitBomb)
        }
    }

    private fun finishSlamHitGround() {
        isSlamming = false
        CAMERA_LOCKED = false

//                spawnPointOrbs(body.p, 100)

        val vel = lastTickVelocity
        spawnParticlesAtMyFeet(
            ferocity = vel.len.d * 0.2,
            number = max((vel.len.d * 0.5).i, 2)
        )

        if (vel.len > 10) {
            // Player landing deserves more than the normal amount of shake
            addScreenShake(vel.len.d * 0.025)
            boom(body.p, vel.len.d * 0.03, affectsThePlayer = false)
        }
    }

    override fun reset() {
        body.setTransform(startingPoint, 0f)
        body.isFixedRotation = true
        body.gravityScale = PLAYER_GRAVITY_MODIFIER
        body.velocity = Point(0.0, 0.0)
        body.linearDamping = 0.0
        tickVelocity = Point(0.0, 0.0)
        lastTickVelocity = Point(0.0, 0.0)
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

        if (!PAUSED) {
            TOUCHES_DOWN.firstOrNull()?.let { touch ->
                // If the player is within the uncertainty window, make the line green
                val lineX = getDesiredXPos(touch.x / UNIT_SIZE_IN_PIXELS)

                if (lineX in (body.p.x - PLAYER_UNCERTAINTY_WINDOW * 1.1)..(body.p.x + PLAYER_UNCERTAINTY_WINDOW * 1.1)) {
                    r.color = Color.WHITE.withAlpha(0.5)
                } else {
                    r.color = Color.WHITE.withAlpha(0.25)
                }
                // For visual interest, draw two lines one thinner than the other
                r.line(
                    lineX,
                    CAMERA_POS_Y,
                    lineX,
                    CAMERA_POS_Y + SCREEN_HEIGHT_IN_UNITS,
                    0.3,
                )
            }
        }

        if (canJump()) {
            r.color = colorScheme.player
        } else if (isSlamming) {
            r.color = colorScheme.playerSlamming
        } else {
            r.color = colorScheme.playerNoJump
        }

        r.circle(body.p, PLAYER_SIZE.w / 2)

        val desiredHeadOffset = -getJumpFraction() * 0.2
        renderedHeadOffset = frameAccurateLerp(renderedHeadOffset, desiredHeadOffset, 30.0)

        r.rect(
            body.p.x - PLAYER_SIZE.w / 2,
            body.p.y,
            PLAYER_SIZE.w,
            PLAYER_SIZE.h / 2 + renderedHeadOffset
        )

        // Move the player's head up and down based on the actionPerformAmount
        r.circle(
            body.p.x,
            body.p.y + 0.35 * GLOBAL_SCALE + renderedHeadOffset,
            PLAYER_SIZE.w / 2
        )
    }

    private fun getJumpFraction(): Double {
        if (!isBuildingUpJump) {
            return 0.0
        }

        val elapsed = RUN_TIME_ELAPSED - lastFingerTime
        return saturate(elapsed / JUMP_BUILD_UP_TIME)
    }

    /** Between 0 and 1 */
    private fun getJumpPower(): Double {
        if (!isBuildingUpJump) {
            return 0.0
        }

        return saturate(lerp(0.6, 1.0, getJumpFraction())) / JUMP_BUILD_UP_TIME
    }

    private var isBuildingUpJump = false
    private var renderedHeadOffset = 0.0

    /** The point where the player's finger last went down. */
    private var lastFingerPoint = Point()
    private var lastFingerTime = 0.0

    private var lastBodyXValue = 0.0
    private fun getDesiredXPos(fingerX: Double): Double {
        return lastBodyXValue + (fingerX - lastFingerPoint.x) * 1.8
    }

    override fun tick() {
        lastTickVelocity = tickVelocity.cpy
        tickVelocity = body.velocity.cpy

        tileBelowMe = getTileBelowMe()
        timeSinceLastJumpOrSlam += DELTA

        var amOnTopOfBomb = false
        whatAmITouching(listOf(slamBox)).forEach {
            if (isSlamming) {
                if (it is Bomb) {
                    finishSlamHitBomb(it)
                }
            }
        }
        whatAmITouching(listOf(jumpBox)).forEach {
            // We can land 'on ground' on bombs too.
            // No amount of slamming on a bomb should ever cause us to land though.
            if (!isSlamming && it is Bomb) {
                landOnGround()
                amOnTopOfBomb = true
            }
        }

        // If we're on the ground but there's nothing below me, we shouldn't be on the ground
        // any longer. (Assuming we're not sitting on a bomb)
        if (tileBelowMe == null && onGround && !amOnTopOfBomb) {
            airTime = 0.0
        }

        if (isSlamming && onGround) {
            finishSlamHitGround()
        }

//        if (isKeyPressed(Keys.R)) {
//            boom(body.p, 1.5, false)
//        }

        deadEndingCountdown = deadEndingCountdown?.minus(DELTA)

        if ((deadEndingCountdown ?: 0.0) < 0.0) {
            PAUSED = true
            registerGameEndWithScoreSystem()
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

        if (isJumpJustPressed() && canJump()) {
            doJump()
        } else if (isSlamJustPressed() && !canJump()) {
            isSlamming = true
        }

        if (TOUCHES_DOWN.size == 1) {
            TOUCHES_WENT_DOWN.forEach {
                lastFingerPoint = it / UNIT_SIZE_IN_PIXELS
                lastFingerTime = RUN_TIME_ELAPSED
                lastBodyXValue = body.p.x
                if (canJump()) {
                    isBuildingUpJump = true
                }
            }
        }

        var desiredXPos = body.p.x

        TOUCHES_DOWN.firstOrNull()?.let { touch ->
            desiredXPos = getDesiredXPos(touch.x / UNIT_SIZE_IN_PIXELS)

            if ((touch / UNIT_SIZE_IN_PIXELS - lastFingerPoint).len > 0.5) {
                // We're outside the jump circle, we're no longer building up a jump.
                isBuildingUpJump = false
            }
            val v = lastFingerTime + JUMP_BUILD_UP_TIME
            if (isBuildingUpJump && v > RUN_TIME_ELAPSED && v < RUN_TIME_ELAPSED + 0.1) {
                if (statefulVibrationSetting.value) {
                    Gdx.input.vibrate(100)
                }
            }
        }

        // If on Desktop, read A/D and Left/Right arrow keys
        if (isKeyPressed(Keys.A) || isKeyPressed(Keys.LEFT)) {
            desiredXPos = 0.0
        }
        if (isKeyPressed(Keys.D) || isKeyPressed(Keys.RIGHT)) {
            desiredXPos = UNITS_WIDE.d
        }

        val magnitude = saturate(abs(desiredXPos - body.p.x) / PLAYER_UNCERTAINTY_WINDOW)

        val desiredXVel = when {
            desiredXPos < body.p.x -> -5 * magnitude
            desiredXPos > body.p.x -> 5 * magnitude
            else -> 0.0
        }

        val velChange = desiredXVel - vel.x

        // If velChange is large enough, spawn particles to simulate kicked up dirt in the direction of movement
        if (!inAir) {
            if (abs(velChange) > 5.1) {
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
        timeSinceLastJumpOrSlam = 0.0
        val desiredUpVel = 15.0 * getJumpPower()
        val velChange = min(desiredUpVel - body.velocity.y, desiredUpVel)
        val impulse = body.mass * velChange
        body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)
        isBuildingUpJump = false

        spawnParticlesAtMyFeet(number = 2)
    }

    private fun canJump(): Boolean {
        return (airTime ?: 0.0) < COYOTE_TIME
    }

    private fun isJumpJustPressed(): Boolean {
        return TOUCHES_WENT_UP.firstOrNull()?.let {
            return isBuildingUpJump
        } ?: if (Gdx.app.type == Application.ApplicationType.Desktop) {
            isKeyJustPressed(Keys.SPACE) || isKeyJustPressed(Keys.W) || isKeyJustPressed(Keys.UP)
        } else {
            false
        }
    }

    private fun isSlamJustPressed(): Boolean {
        return TOUCHES_WENT_UP.firstOrNull()?.let {
            return true
        } ?: if (Gdx.app.type == Application.ApplicationType.Desktop) {
            isKeyJustPressed(Keys.S) || isKeyJustPressed(Keys.DOWN)
        } else {
            false
        }
    }


    private fun getTileBelowMe(): Tile? {
        // We effectively need to check in a 3x3 grid below the player

        // Top middle of the search grid
        val searchStartPos = feetPosition

        for (x in intArrayOf(0, 1, -1)) {
            for (y in 0 downTo -2) {
                val pos = searchStartPos + Point(
                    x * TILE_SIZE_IN_UNITS * 0.75,
                    y * TILE_SIZE_IN_UNITS * 0.75
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