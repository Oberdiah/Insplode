package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.*
import com.oberdiah.Utils.TileType
import com.oberdiah.Utils.colorScheme
import kotlin.random.Random

val player = Player(Point(5, PLAYER_SPAWN_Y))

class Player(startingPoint: Point) : PhysicsObject(startingPoint) {
    val size = Size(0.4, 0.7)
    lateinit var jumpBox: Fixture
    var canJump = false
    private var airTime = 0.0
    private var inAir = true

    val numHealthDots = 3
    var health = 3

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

            if (airTime > 0.3) {
                spawnParticlesAtMyFeet(
                    ferocity = body.velocity.len.d * 0.5,
                    number = max(body.velocity.len.i * 2, 2)
                )
                if (body.velocity.len > 20) {
                    addScreenShake(body.velocity.len.d * 0.03)
                    boom(body.p, body.velocity.len.d * 0.05, hurtsThePlayer = false)
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

    init {
        body.isFixedRotation = true

        circleShape(size.w / 2) {
            addFixture(it)
        }
        circleShape(size.w / 2, Point(0, 0.35)) {
            addFixture(it)
        }

        rectShape(size / Point(1, 2), Point(0, 0.2)) {
            addFixture(it)
        }

        rectShape(size / Point(1.25, 4), Point(0f, -0.2f)) {
            jumpBox = addFixture(it, true)
        }
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
        r.circle(body.p.x, body.p.y + 0.35, size.w / 2)


        for (i in 0 until numHealthDots) {
            if (i < health) {
                r.color = Color.WHITE
            } else {
                r.color = Color.DARK_GRAY
            }
            r.circle(body.p.x, body.p.y + (0.35 / (numHealthDots - 1)) * i, size.w / 5)
        }
    }

    override fun tick() {
        val vel = body.velocity

        if (inAir) {
            airTime += DELTA
        }

        // detect jumps
        TOUCHES_WENT_DOWN.forEach {
            if (!TOUCH_CONSUMED && canJump && it.y <= HEIGHT * JUMP_FRACT) {
                canJump = false
                val desiredUpVel = 9.0f
                val velChange = desiredUpVel - vel.y
                val impulse = body.mass * velChange
                body.applyImpulse(Point(0f, impulse))

                spawnParticlesAtMyFeet(number = 2)

                jumpUIFadeOff = UI_MAX_FADE_IN
            }
        }


        val acceleration = 2.5
        var desiredVel = 0.0
        TOUCHES_DOWN.forEach {
            if (!TOUCH_CONSUMED && it.y > HEIGHT * JUMP_FRACT) {
                val goLeft = if (FOLLOW_FINGER) {
                    it.x < body.p.x * (SQUARE_SIZE - 1)
                } else {
                    it.x < LEFT_BUTTON_FRACT * WIDTH
                }

                val goRight = if (FOLLOW_FINGER) {
                    it.x > body.p.x * (SQUARE_SIZE + 1)
                } else {
                    it.x > RIGHT_BUTTON_FRACT * WIDTH
                }

                if (goLeft) {
                    desiredVel = max(-5, vel.x - acceleration)
                } else if (goRight) {
                    desiredVel = min(5, vel.x + acceleration)
                }
            }
        }

        val velChange = desiredVel - vel.x

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

}