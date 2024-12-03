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

class TouchPoint(val index: Int) : Point() {
    var timeUp: Double = -1.0
    var timeDown: Double = -1.0
}

open class Point {
    open var x: Double = 0.0
    open var y: Double = 0.0

    override fun toString(): String {
        return "(${x.format(2)}, ${y.format(2)})"
    }

    val ui: Point
        get() = toUISpace(this)

    val wo: Point
        get() = toWorldSpace(this)

    val cpy: Point
        get() {
            return Point(this)
        }

    val v2: Vector2
        get() {
            return Vector2(x.f, y.f)
        }

    var angle: Double
        get() {
            return atan2(x, y)
        }
        set(value) {
            x = 0.0
            y = len.d
            rotate(value)
        }

    var len: Double
        get() {
            return sqrt(x * x + y * y)
        }
        set(value) {
            val le = len.d
            if (le < 0.01) {
                x = value.d
                y = 0.0
                return
            }
            val scale = value.d / le
            x *= scale
            y *= scale
        }

    constructor(angle: Double) {
        x = 0.0
        y = 1.0
        rotate(angle)
    }

    constructor(p: Point) {
        x = p.x
        y = p.y
    }

    constructor(x: Double, y: Double) {
        this.x = x.d
        this.y = y.d
    }

    constructor(x: Float, y: Double) {
        this.x = x.d
        this.y = y.d
    }

    constructor(x: Double, y: Float) {
        this.x = x.d
        this.y = y.d
    }

    constructor(x: Float, y: Float) {
        this.x = x.d
        this.y = y.d
    }

    constructor(x: Int, y: Int) {
        this.x = x.d
        this.y = y.d
    }

    constructor(x: Int, y: Double) {
        this.x = x.d
        this.y = y.d
    }

    constructor(x: Double, y: Int) {
        this.x = x.d
        this.y = y.d
    }

    constructor(x: Int, y: Float) {
        this.x = x.d
        this.y = y.d
    }

    constructor(x: Float, y: Int) {
        this.x = x.d
        this.y = y.d
    }

    constructor() : this(0.0, 0.0)

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

    fun rotate(angle: Double) {
        val cos = cos(angle)
        val sin = sin(angle)

        val x1 = -(x * cos - y * sin)
        val y1 = -(x * sin + y * cos)
        x = x1
        y = y1
    }

    fun normCpy(): Point {
        val cpy = this.cpy
        cpy.len = 1.0
        return cpy
    }

    fun lenCpy(l: Double): Point {
        val cpy = this.cpy
        cpy.len = l
        return cpy
    }

    operator fun times(p: Point): Point {
        return Point(x * p.x, y * p.y)
    }

    open operator fun times(i: Double): Point {
        return Point(x * i, y * i)
    }

    open operator fun times(i: Float): Point {
        return Point(x * i, y * i)
    }

    open operator fun times(i: Int): Point {
        return Point(x * i, y * i)
    }

    open operator fun plus(p: Point): Point {
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

    open operator fun div(p: Point): Point {
        return Point(x / p.x, y / p.y)
    }

    open operator fun div(i: Double): Point {
        return Point(x / i, y / i)
    }

    open operator fun div(i: Float): Point {
        return Point(x / i, y / i)
    }

    open operator fun div(i: Int): Point {
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

    fun zero() {
        x = 0.0
        y = 0.0
    }

    fun clampX(a: Double, b: Double) {
        x = clamp(x, a, b)
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
        if (l > max) {
            copy.len = max
        } else if (l < min) {
            copy.len = min
        }
        return copy
    }

    fun amin(d: Double): Point {
        return Point(min(abs(x), d) * sign(x), min(abs(y), d) * sign(y))
    }

    fun clamp(a: Point, b: Point) {
        x = clamp(x, a.x, b.x)
        y = clamp(y, a.y, b.y)
    }

    fun dot(p: Point): Double {
        return x * p.x + y * p.y
    }

    fun setTo(p: Point) {
        x = p.x
        y = p.y
    }
}

typealias Velocity = Point

class Size(w: Double, h: Double) : Point(w, h) {
    constructor() : this(0.0, 0.0)
    constructor(s: Double) : this(s, s)
    constructor(p: Point) : this(p.x, p.y)
    constructor(w: Float, h: Float) : this(w.d, h.d)
    constructor(w: Int, h: Int) : this(w.d, h.d)
    constructor(w: Int, h: Double) : this(w.d, h)
    constructor(w: Double, h: Int) : this(w, h.d)
    constructor(w: Float, h: Double) : this(w.d, h)
    constructor(w: Double, h: Float) : this(w, h.d)
    constructor(w: Float, h: Int) : this(w.d, h.d)
    constructor(w: Int, h: Float) : this(w.d, h)

    override operator fun plus(p: Point): Size {
        return Size(x + p.x, y + p.y)
    }

    override operator fun div(p: Point): Size {
        return Size(x / p.x, y / p.y)
    }

    override operator fun div(i: Double): Size {
        return Size(x / i, y / i)
    }

    override operator fun times(i: Double): Size {
        return Size(x * i, y * i)
    }

    var w: Double
        get() = x
        set(value) {
            x = value.d
        }

    var h: Double
        get() = y
        set(value) {
            y = value.d
        }
}

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