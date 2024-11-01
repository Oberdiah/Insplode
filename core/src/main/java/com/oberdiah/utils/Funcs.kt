package com.oberdiah

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.Shape
import com.oberdiah.utils.GameTime
import com.oberdiah.utils.Perlin
import com.oberdiah.utils.camera
import kotlin.math.PI
import kotlin.math.pow
import kotlin.random.Random

fun project(p: Point, camera: Camera): Point {
    val coords = camera.project(Vector3(p.x.f, p.y.f, 0F))
    return Point(coords.x, coords.y)
}

fun unproject(p: Point, camera: Camera): Point {
    val coords = camera.unproject(Vector3(p.x.f, HEIGHT.f - p.y.f, 0F))
    return Point(coords.x, coords.y)
}

fun toUISpace(p: Point): Point {
    return project(p, camera)
}

fun toWorldSpace(p: Point): Point {
    return unproject(p, camera)
}

fun formatScore(amount: Int): String {
    if (amount >= 1000) {
        if (amount % 1000 == 0) {
            return "${amount / 1000}k"
        }

        return "${(amount / 1000.0).format(2)}k"
    }

    return "${amount}c"
}

fun createRandomPoint(): Point {
    return Point(Random.nextDouble(), Random.nextDouble())
}

fun createRandomFacingPoint(): Point {
    return Point(Random.nextDouble() * PI * 2)
}

fun colorFrom(i: Long): Color {
    val cl = i.shl(8).or(0xff)
    return Color(cl.toInt())
}

inline fun <reified T : Enum<T>> Enum<T>.next(): T {
    return enumValues<T>()[((this.ordinal + 1) % enumValues<T>().size)]
}

fun pointListToFloatArr(ps: List<Point>): FloatArray {
    val floatArr = FloatArray(ps.size * 2)
    ps.forEachIndexed { index, point ->
        floatArr[index * 2] = point.x.f
        floatArr[index * 2 + 1] = point.y.f
    }
    return floatArr
}

fun Color.withAlpha(d: Number): Color {
    val newCol = Color(this)
    newCol.a = d.toFloat()
    return newCol
}

fun max(a: Number, b: Number): Double {
    return a.toDouble().coerceAtLeast(b.toDouble())
}

fun max(a: Int, b: Int): Int {
    return a.coerceAtLeast(b)
}

fun min(a: Number, b: Number): Double {
    return a.toDouble().coerceAtMost(b.toDouble())
}

fun min(a: Int, b: Int): Int {
    return a.coerceAtMost(b)
}

fun Number.format(digits: Int) = "%.${digits}f".format(this)

val Number.d: Double
    get() {
        return this.toDouble()
    }

val Number.i: Int
    get() {
        return this.toInt()
    }

val Number.f: Float
    get() {
        return this.toFloat()
    }

val Number.abs: Double
    get() {
        return kotlin.math.abs(this.d)
    }

fun sqrt(n: Number): Double {
    return kotlin.math.sqrt(n.d)
}

fun cbrt(n: Number): Double {
    return Math.cbrt(n.d)
}

fun abs(n: Int): Int {
    return kotlin.math.abs(n)
}

fun abs(n: Number): Double {
    return kotlin.math.abs(n.d)
}

fun sign(n: Number): Double {
    return kotlin.math.sign(n.d)
}

fun sin(a: Number): Double {
    return kotlin.math.sin(a.d)
}

fun cos(a: Number): Double {
    return kotlin.math.cos(a.d)
}

fun clamp(t: Number, a: Number, b: Number): Double {
    return if (t < a) {
        a.d
    } else if (t > b) {
        b.d
    } else {
        t.d
    }
}

fun saturate(t: Number): Double {
    return clamp(t, 0, 1)
}

fun floor(t: Number): Int {
    return kotlin.math.floor(t.d).i
}

fun ceil(t: Number): Int {
    return kotlin.math.ceil(t.d).i
}

fun fract(x: Number): Number {
    return x - floor(x)
}

operator fun Number.compareTo(i: Number): Int {
    return this.d.compareTo(i.d)
}

operator fun Number.plus(i: Number): Number {
    return this.d + i.d
}

operator fun Number.minus(i: Number): Number {
    return this.d - i.d
}

operator fun Number.div(i: Number): Number {
    return this.d / i.d
}

operator fun Number.times(i: Number): Number {
    return this.d * i.d
}

operator fun Number.unaryMinus(): Number {
    return -this.d
}

fun Color.asInt(): Int {
    return (255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt()
}

fun circleShape(radius: Number, callback: (Shape) -> Unit) {
    circleShape(radius, Point(), callback)
}

fun <T, R> T?.notNull(f: (T) -> R) {
    if (this != null) {
        f(this)
    }
}

// Finds the normal of the line, pointing towards the side specified by "side"
fun lineNormal(p0: Point, p1: Point, side: Point): Point {
    val z = (p1.x - p0.x) * (side.y - p1.y) - (p1.y - p0.y) * (side.x - p1.x)
    val vec = p1 - p0
    vec.len = 1
    if (z > 0) {
        return Point(-vec.y, vec.x)
    } else {
        return Point(vec.y, -vec.x)
    }
}

private val s1 = Point()
private val s2 = Point()

// Copied from https://stackoverflow.com/a/1968345
fun lineIntersection(p0: Point, p1: Point, p2: Point, p3: Point, intersection: Point): Boolean {
    s1.x = p1.x - p0.x
    s1.y = p1.y - p0.y
    s2.x = p3.x - p2.x
    s2.y = p3.y - p2.y

    val s = (-s1.y * (p0.x - p2.x) + s1.x * (p0.y - p2.y)) / (-s2.x * s1.y + s1.x * s2.y)
    val t = (s2.x * (p0.y - p2.y) - s2.y * (p0.x - p2.x)) / (-s2.x * s1.y + s1.x * s2.y)

    if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
        // Collision detected
        intersection.x = p0.x + (t * s1.x)
        intersection.y = p0.y + (t * s1.y)
        return true
    }

    return false
}

fun circleShape(radius: Number, middle: Point, callback: (Shape) -> Unit) {
    val circle = CircleShape()
    circle.radius = radius.f
    circle.position = middle.v2

    callback(circle)

    circle.dispose()
}

fun ngonShape(radius: Number, sides: Int, callback: (Shape) -> Unit) {
    val polygon = PolygonShape()
    val points = Array(8) { Vector2() }
    for (i in 0 until sides) {
        points[i].x = (cos(2 * i * PI / sides) * radius).f
        points[i].y = (sin(2 * i * PI / sides) * radius).f
    }
    polygon.set(points)
    callback(polygon)
    polygon.dispose()
}

fun rectShape(size: Size, callback: (Shape) -> Unit) {
    rectShape(size, Point(), callback)
}

fun rectShape(size: Size, middle: Point, callback: (Shape) -> Unit) {
    val rect = PolygonShape()
    rect.setAsBox(size.w.f / 2, size.h.f / 2, middle.v2, 0f)
    callback(rect)
    rect.dispose()
}

fun rectShape(size: Size, middle: Point, angle: Number, callback: (Shape) -> Unit) {
    val rect = PolygonShape()
    rect.setAsBox(size.w.f / 2, size.h.f / 2, middle.v2, angle.f)
    callback(rect)
    rect.dispose()
}

fun lerp(a: Number, b: Number, t: Number): Double {
    return a.d + (b.d - a.d) * t.d
}

/**
 * If you don't know what you want for speed, for reference a
 * speed of 10.0 is approximately a regular frame-lerp of 0.1
 */
fun frameAccurateLerp(a: Number, b: Number, speed: Number): Double {
    // https://youtu.be/yGhfUcPjXuE?t=1155
    val blend = 0.5.pow(GameTime.GRAPHICS_DELTA * speed.d)
    return lerp(b, a, blend)
}

fun easeInOutSine(x: Double): Double {
    return -(cos(Math.PI * saturate(x)) - 1) / 2;
}

fun transitionOver(
    fraction: Double,
    isActive: Boolean,
    wasActive: Boolean,
    distance: Double
): Double {
    return if (isActive) {
        lerp(0.0, distance, saturate(fraction))
    } else if (wasActive) {
        lerp(distance, 0.0, saturate(fraction))
    } else {
        0.0
    }
}

fun getShake(power: Double, ferocity: Double = 200.0, seed: Int = 1): Double {
    if (power < 0.001) {
        return 0.0
    }

    return (Perlin.fbm(GameTime.APP_TIME * ferocity + seed * 200.0, 0, 3, 2.0) * power * 0.2)
}

fun get2DShake(power: Double, seed: Int = 1, ferocity: Double = 200.0): Point {
    return Point(
        getShake(power, ferocity, seed = seed),
        getShake(power, ferocity, seed = seed + 2)
    )
}

fun <T> Map<T, Double>.getOrZero(key: T): Double {
    return this[key] ?: 0.0
}