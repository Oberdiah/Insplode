package com.oberdiah

import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.math.Vector2
import kotlin.math.atan2
import kotlin.random.Random

fun List<Point>.toV2Array(): Array<Vector2> {
    return Array(this.size) { this[it].v2 }
}

operator fun Iterable<Point>.times(tileSize: Double): List<Point> {
    return this.map { it * tileSize }
}

val Vector2.p: Point
    get() = Point(this.x.d, this.y.d)

class Point private constructor(val x: Double, val y: Double) {
    companion object {
        private fun create(x: Double, y: Double): Point {
            return Point(x, y)
        }

        operator fun invoke(angle: Double): Point {
            val p = Point(0.0, 1.0)
            return p.rotate(angle)
        }

        operator fun invoke(p: Point): Point {
            return Point(p.x, p.y)
        }

        operator fun invoke(x: Double, y: Double): Point {
            return create(x, y)
        }

        operator fun invoke(x: Float, y: Double): Point {
            return create(x.d, y)
        }

        operator fun invoke(x: Double, y: Float): Point {
            return create(x, y.d)
        }

        operator fun invoke(x: Float, y: Float): Point {
            return create(x.d, y.d)
        }

        operator fun invoke(x: Int, y: Int): Point {
            return create(x.d, y.d)
        }

        operator fun invoke(x: Int, y: Double): Point {
            return create(x.d, y)
        }

        operator fun invoke(x: Double, y: Int): Point {
            return create(x, y.d)
        }

        operator fun invoke(x: Float, y: Int): Point {
            return create(x.d, y.d)
        }

        operator fun invoke(x: Int, y: Float): Point {
            return create(x.d, y.d)
        }

        operator fun invoke(): Point {
            return Point(0.0, 0.0)
        }
    }

    override fun toString(): String {
        return "(${x.format(2)}, ${y.format(2)})"
    }

    val ui: Point
        get() = toUISpace(this)

    val wo: Point
        get() = toWorldSpace(this)

    val v2: Vector2
        get() {
            return Vector2(x.f, y.f)
        }

    val angle: Double
        get() {
            return atan2(x, y)
        }

    val len: Double
        get() {
            return sqrt(x * x + y * y)
        }

    fun angleTo(p: Point): Double {
        return atan2(p.x - x, p.y - y)
    }

    fun distTo(x: Double, y: Double): Double {
        val dx = x - this.x
        val dy = y - this.y
        return sqrt(dx * dx + dy * dy)
    }

    fun distTo(p: Point): Double {
        val dx = p.x - x
        val dy = p.y - y
        return sqrt(dx * dx + dy * dy)
    }

    fun withLen(newLen: Double): Point {
        val le = len
        if (le < 0.01) {
            return Point(newLen, 0.0)
        }
        val scale = newLen / le
        return Point(x * scale, y * scale)
    }

    fun normalize(): Point {
        return withLen(1.0)
    }

    fun withAngle(angle: Double): Point {
        return Point(0.0, len).rotate(angle)
    }

    fun rotate(angle: Double): Point {
        val cos = cos(angle)
        val sin = sin(angle)

        val x1 = -(x * cos - y * sin)
        val y1 = -(x * sin + y * cos)
        return Point(x1, y1)
    }

    operator fun times(p: Point): Point {
        return Point(x * p.x, y * p.y)
    }

    operator fun times(i: Double): Point {
        return Point(x * i, y * i)
    }

    operator fun times(i: Float): Point {
        return Point(x * i, y * i)
    }

    operator fun times(i: Int): Point {
        return Point(x * i, y * i)
    }

    operator fun plus(p: Point): Point {
        return Point(x + p.x, y + p.y)
    }

    operator fun plus(i: Double): Point {
        return Point(x + i, y + i)
    }

    operator fun plus(i: Float): Point {
        return Point(x + i, y + i)
    }

    operator fun plus(i: Int): Point {
        return Point(x + i, y + i)
    }

    operator fun minus(p: Point): Point {
        return Point(x - p.x, y - p.y)
    }

    operator fun minus(i: Double): Point {
        return Point(x - i, y - i)
    }

    operator fun minus(i: Float): Point {
        return Point(x - i, y - i)
    }

    operator fun minus(i: Int): Point {
        return Point(x - i, y - i)
    }

    operator fun div(p: Point): Point {
        return Point(x / p.x, y / p.y)
    }

    operator fun div(i: Double): Point {
        return Point(x / i, y / i)
    }

    operator fun div(i: Float): Point {
        return Point(x / i, y / i)
    }

    operator fun div(i: Int): Point {
        return Point(x / i, y / i)
    }

    operator fun unaryMinus(): Point {
        return Point(-x, -y)
    }

    fun minX(m: Double): Point {
        return Point(min(x, m), y)
    }

    fun maxX(m: Double): Point {
        return Point(max(x, m), y)
    }

    fun minY(m: Double): Point {
        return Point(x, min(y, m))
    }

    fun maxY(m: Double): Point {
        return Point(x, max(y, m))
    }

    fun max(d: Double): Point {
        return Point(max(x, d), max(y, d))
    }

    fun min(d: Double): Point {
        return Point(min(x, d), min(y, d))
    }

    fun clampLen(min: Double, max: Double): Point {
        val copy = Point(this)
        val l = len
        return if (l > max) {
            copy.withLen(max)
        } else if (l < min) {
            copy.withLen(min)
        } else {
            copy
        }
    }

    fun amin(d: Double): Point {
        return Point(min(abs(x), d) * sign(x), min(abs(y), d) * sign(y))
    }

    fun clamp(a: Point, b: Point): Point {
        return Point(clamp(x, a.x, b.x), clamp(y, a.y, b.y))
    }

    fun dot(p: Point): Double {
        return x * p.x + y * p.y
    }

    fun copy(x: Double = this.x, y: Double = this.y): Point {
        return Point(x, y)
    }

    fun minus(x: Double = 0.0, y: Double = 0.0): Point {
        return Point(this.x - x, this.y - y)
    }

    fun plus(x: Double = 0.0, y: Double = 0.0): Point {
        return Point(this.x + x, this.y + y)
    }

    fun times(x: Double = 1.0, y: Double = 1.0): Point {
        return Point(this.x * x, this.y * y)
    }

    fun div(x: Double = 1.0, y: Double = 1.0): Point {
        return Point(this.x / x, this.y / y)
    }
}

typealias Velocity = Point
typealias Size = Point

val Size.w: Double
    get() = x
val Size.h: Double
    get() = y

class Rect(var p: Point, var s: Size) {
    companion object {
        fun centered(center: Point, size: Size): Rect {
            return Rect(center - size / 2.0, size)
        }
    }

    val x
        get() = p.x

    val y
        get() = p.y

    val w
        get() = s.w

    val h
        get() = s.h

    val bl
        get() = p

    val br
        get() = p + Point(s.w, 0.0)

    val tl
        get() = p + Point(0.0, s.h)

    val tr
        get() = p + s

    fun center(): Point {
        return p + s / 2.0
    }

    fun offsetBy(p: Point): Rect {
        return Rect(this.p + p, s)
    }

    fun offsetBy(x: Double, y: Double): Rect {
        return Rect(this.p + Point(x, y), s)
    }

    fun enlargen(buffer: Double): Rect {
        return Rect(p - Point(buffer, buffer), s + Point(buffer, buffer) * 2.0)
    }

    fun contains(p1: Point): Boolean {
        val a = (p1.x >= p.x)
        val b = (p1.y >= p.y)
        val c = (p1.x <= p.x + s.w)
        val d = (p1.y <= p.y + s.h)
        return a && b && c && d
    }

    fun touches(rect: Rect): Boolean {
        return p.x < rect.p.x + rect.s.w &&
                p.y < rect.p.y + rect.s.h &&
                p.x + s.w > rect.p.x &&
                p.y + s.h > rect.p.y

    }

    fun randomPointInside(): Point {
        if (s.w == 0.0 && s.h == 0.0) {
            return p
        }

        return Point(Random.nextDouble(p.x, p.x + s.w), Random.nextDouble(p.y, p.y + s.h))
    }

    constructor() : this(Point(), Size())
}