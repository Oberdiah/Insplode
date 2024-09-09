package com.oberdiah

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.Utils.TOUCHES_DOWN
import com.oberdiah.Utils.TOUCHES_WENT_DOWN
import com.oberdiah.Utils.colorScheme
import com.oberdiah.Utils.isKeyPressed
import com.oberdiah.ui.jumpUIFadeOff
import com.oberdiah.ui.switchScreen
import kotlin.random.Random

val player = Player(Point(5, PLAYER_SPAWN_Y))

class Player(startingPoint: Point) : PhysicsObject(startingPoint) {
    val size = Size(0.4, 0.7) * GLOBAL_SCALE
    lateinit var jumpBox: Fixture
    var canJump = false
    private var airTime = 0.0
    private var inAir = true

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
        if (yourFixture == jumpBox) {
            CAMERA_LOCKED = false

            if (airTime > 0.6) {
                val vel = lastTickVelocity

                spawnParticlesAtMyFeet(
                    ferocity = vel.len.d * 0.25,
                    number = max(vel.len.i * 2, 2)
                )
                if (vel.len > 20) {
                    addScreenShake(vel.len.d * 0.03)
                    boom(body.p, vel.len.d * 0.05, hurtsThePlayer = false)
                }
            }

            canJump = true
            airTime = 0.0
            inAir = false
        }
    }

    override fun endCollide(yourFixture: Fixture, otherFixture: Fixture) {
        super.endCollide(yourFixture, otherFixture)
        if (yourFixture == jumpBox) {
            inAir = true
        }
    }

    override fun reset() {
        body.setTransform(startingPoint, 0f)
        inAir = true
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

        if (inAir) {
            airTime += DELTA
        }

        // detect jumps
        TOUCHES_WENT_DOWN.forEach {
            if (it.y <= HEIGHT * JUMP_UI_FRACT) {
                attemptJump()
            }
        }

        // On Desktop, spacebar also jumps, as do W and Up arrow
        if (isKeyPressed(Keys.SPACE) || isKeyPressed(Keys.W) || isKeyPressed(Keys.UP)) {
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
        val tileTypeBelowMe = getTile(body.p - Point(0, 0.2)).tileType
        val posToSpawn = body.p - Point(0.0, 0.15)

        for (i in 0 until number) {
            spawnFragment(
                posToSpawn.cpy,
                addedVelocity + Velocity(
                    ferocity * (Random.nextDouble() - 0.5),
                    ferocity * Random.nextDouble()
                ),
                tileTypeBelowMe
            )
        }
    }

    private fun attemptJump() {
        if (!canJump) return
        canJump = false
        val desiredUpVel = 9.0f
        val velChange = desiredUpVel - body.velocity.y
        val impulse = body.mass * velChange
        body.applyImpulse(Point(0f, impulse) * GLOBAL_SCALE)

        spawnParticlesAtMyFeet(number = 2)

        jumpUIFadeOff = UI_MAX_FADE_IN
    }

}