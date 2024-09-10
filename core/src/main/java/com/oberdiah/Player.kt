package com.oberdiah

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.Utils.TOUCHES_DOWN
import com.oberdiah.Utils.TOUCHES_WENT_DOWN
import com.oberdiah.Utils.colorScheme
import com.oberdiah.Utils.isKeyJustPressed
import com.oberdiah.Utils.isKeyPressed
import com.oberdiah.ui.jumpUIFadeOff
import com.oberdiah.ui.switchScreen
import kotlin.random.Random

val player = Player(Point(5, PLAYER_SPAWN_Y))

class Player(startingPoint: Point) : PhysicsObject(startingPoint) {
    val size = Size(0.4, 0.7) * GLOBAL_SCALE
    lateinit var jumpBox: Fixture
    var canJump = false
    var slamming = false
    private var airTime = 10.0
    private var inAir = true
    private var tileBelowMe: Tile = nonTile

    val feetPosition
        get() = body.p - Point(0.0, 0.35 * GLOBAL_SCALE)

    val willSmash
        get() = lastTickVelocity.len > 8 && slamming

    /// We need these because sometimes on collide the velocity is already really small for some reason
    private var tickVelocity = Point(0.0, 0.0)
    private var lastTickVelocity = Point(0.0, 0.0)

    val numHealthDots = 3
    var health = 3

    init {
        body.isFixedRotation = true

        circleShape(size.w / 2) {
            addFixture(it)
        }
        circleShape(size.w / 2, Point(0, 0.35) * GLOBAL_SCALE) {
            addFixture(it)
        }

        rectShape(size / Point(1, 2), Point(0, 0.2) * GLOBAL_SCALE) {
            addFixture(it)
        }

        rectShape(size / Point(1.25, 4), Point(0f, -0.2f) * GLOBAL_SCALE) {
            jumpBox = addFixture(it, true)
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

        var didBounce = false
        val obj = otherFixture.body.userData
        if (obj is Bomb) {
            if (slamming) {
                // If the player was moving fast enough
                if (willSmash) {
                    boom(obj.body.p, obj.power, hurtsThePlayer = false)
                    obj.destroy()
                    val currentVel = body.velocity.y
                    val desiredVel = 10.0
                    val impulse = body.mass * (desiredVel - currentVel)
                    body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)

                    didBounce = true
                }

                slamming = false
            }
        }

        if (yourFixture == jumpBox) {
            CAMERA_LOCKED = false

            if (airTime > 0.6) {
                val vel = lastTickVelocity / GLOBAL_SCALE

                spawnParticlesAtMyFeet(
                    ferocity = vel.len.d * 0.2,
                    number = max((vel.len.d * 1.5).i, 2)
                )

                if (vel.len > 20) {
                    addScreenShake(vel.len.d * 0.03)
                    boom(body.p, vel.len.d * 0.05, hurtsThePlayer = false)
                }
            }
            airTime = 0.0

            if (!didBounce) {
                canJump = true
                slamming = false
                inAir = false
            }
        }
    }

    override fun reset() {
        body.setTransform(startingPoint, 0f)
        inAir = true
        canJump = false
        slamming = false
        health = numHealthDots
    }

    private fun addFixture(shape: Shape, isSensor: Boolean = false): Fixture {
        val fixtureDef = FixtureDef()
        fixtureDef.shape = shape
        fixtureDef.density = 5.0f
        fixtureDef.friction = 0.0f
        fixtureDef.restitution = 0.1f
        fixtureDef.filter.categoryBits = PLAYER_PHYSICS_MASK
        fixtureDef.isSensor = isSensor
        return body.addFixture(fixtureDef)
    }

    override fun render(r: Renderer) {
        if (canJump) {
            r.color = colorScheme.player
        } else if (willSmash) {
            r.color = colorScheme.playerSmashable
        } else if (slamming) {
            r.color = colorScheme.playerSlamming
        } else {
            r.color = colorScheme.playerNoJump
        }
        r.rect(body.p.x - size.w / 2, body.p.y, size.w, size.h / 2)
        r.circle(body.p, size.w / 2)
        r.circle(body.p.x, body.p.y + 0.35 * GLOBAL_SCALE, size.w / 2)

        for (i in 0 until numHealthDots) {
            if (i < health) {
                r.color = Color.WHITE
            } else {
                r.color = Color.DARK_GRAY
            }
            r.circle(
                body.p.x,
                body.p.y + (0.35 * GLOBAL_SCALE / (numHealthDots - 1)) * i,
                size.w / 5
            )
        }
    }

    override fun tick() {
        lastTickVelocity = tickVelocity.cpy
        tickVelocity = body.velocity.cpy

        val vel = body.velocity

        tileBelowMe = getTile(feetPosition - Point(0, SIMPLE_SIZE * 0.5))

        if (inAir) {
            airTime += DELTA
        }

        if (slamming) {
            // Slam into the ground
            val downVelChange = -1.0f
            val impulse = body.mass * downVelChange
            body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)
        }

        // detect jumps
        TOUCHES_WENT_DOWN.forEach {
            if (it.y <= HEIGHT * JUMP_UI_FRACT) {
                attemptJump()
            }
        }

        // On Desktop, spacebar also jumps, as do W and Up arrow
        if (isKeyJustPressed(Keys.SPACE) || isKeyJustPressed(Keys.W) || isKeyJustPressed(Keys.UP)) {
            attemptJump()
        }

        val acceleration = 2.5 * GLOBAL_SCALE
        var desiredXVel = 0.0
        var goLeft = false
        var goRight = false
        TOUCHES_DOWN.forEach {
            if (it.y > HEIGHT * JUMP_UI_FRACT) {
                goLeft = goLeft || if (FOLLOW_FINGER) {
                    it.x < body.p.x * (SQUARE_SIZE - 1)
                } else {
                    it.x < LEFT_BUTTON_UI_FRACT * WIDTH
                }

                goRight = goRight || if (FOLLOW_FINGER) {
                    it.x > body.p.x * (SQUARE_SIZE + 1)
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

    private fun attemptJump() {
        if (canJump) {
            canJump = false
            val desiredUpVel = 9.0f
            val velChange = min(desiredUpVel - body.velocity.y, desiredUpVel)
            val impulse = body.mass * velChange
            body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)

            spawnParticlesAtMyFeet(number = 2)
        } else if (!slamming) {
            slamming = true
            jumpUIFadeOff = UI_MAX_FADE_IN
        }
    }

}