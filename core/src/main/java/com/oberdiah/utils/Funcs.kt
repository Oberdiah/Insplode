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

fun Color.withAlpha(d: Float): Color {
    val newCol = Color(this)
    newCol.a = d
    return newCol
}

fun Color.withAlpha(d: Double): Color {
    return withAlpha(d.f)
}

fun max(a: Double, b: Double): Double {
    return a.coerceAtLeast(b)
}

fun max(a: Float, b: Float): Float {
    return a.coerceAtLeast(b)
}

fun max(a: Int, b: Int): Int {
    return a.coerceAtLeast(b)
}

fun min(a: Double, b: Double): Double {
    return a.coerceAtMost(b)
}

fun min(a: Float, b: Float): Float {
    return a.coerceAtMost(b)
}

fun min(a: Int, b: Int): Int {
    return a.coerceAtMost(b)
}

fun Int.format(digits: Int) = "%.${digits}f".format(this)
fun Double.format(digits: Int) = "%.${digits}f".format(this)
fun Float.format(digits: Int) = "%.${digits}f".format(this)

val Int.d: Double
    get() {
        return this.toDouble()
    }

val Float.d: Double
    get() {
        return this.toDouble()
    }

val Double.i: Int
    get() {
        return this.toInt()
    }

val Float.i: Int
    get() {
        return this.toInt()
    }

val Double.f: Float
    get() {
        return this.toFloat()
    }

val Int.f: Float
    get() {
        return this.toFloat()
    }

val Int.abs: Double
    get() {
        return kotlin.math.abs(this.d)
    }

val Float.abs: Double
    get() {
        return kotlin.math.abs(this.d)
    }

val Double.abs: Double
    get() {
        return kotlin.math.abs(this)
    }

/**
 * If you use the basic `in`/`contains` you end up having to box your doubles.
 */
fun ClosedFloatingPointRange<Double>.containsPrimitive(value: Double): Boolean {
    return value in start..endInclusive
}

fun ClosedFloatingPointRange<Float>.containsPrimitive(value: Float): Boolean {
    return value in start..endInclusive
}

fun sqrt(n: Double): Double {
    return kotlin.math.sqrt(n)
}

fun sqrt(n: Int): Double {
    return kotlin.math.sqrt(n.d)
}

fun sqrt(n: Float): Float {
    return kotlin.math.sqrt(n)
}

fun cbrt(n: Double): Double {
    return kotlin.math.cbrt(n)
}

fun cbrt(n: Int): Double {
    return kotlin.math.cbrt(n.d)
}

fun cbrt(n: Float): Float {
    return kotlin.math.cbrt(n)
}

fun abs(n: Int): Int {
    return kotlin.math.abs(n)
}

fun abs(n: Double): Double {
    return kotlin.math.abs(n)
}

fun abs(n: Float): Float {
    return kotlin.math.abs(n)
}

fun sign(n: Double): Double {
    return kotlin.math.sign(n)
}

fun sign(n: Float): Float {
    return kotlin.math.sign(n)
}

fun sign(n: Int): Double {
    return kotlin.math.sign(n.d)
}

fun sin(a: Double): Double {
    return kotlin.math.sin(a)
}

fun sin(a: Float): Float {
    return kotlin.math.sin(a)
}

fun cos(a: Double): Double {
    return kotlin.math.cos(a)
}

fun cos(a: Float): Float {
    return kotlin.math.cos(a)
}

fun clamp(t: Double, a: Double, b: Double): Double {
    return if (t < a) {
        a
    } else if (t > b) {
        b
    } else {
        t
    }
}

fun clamp(t: Float, a: Float, b: Float): Float {
    return if (t < a) {
        a
    } else if (t > b) {
        b
    } else {
        t
    }
}

fun clamp(t: Int, a: Int, b: Int): Int {
    return if (t < a) {
        a
    } else if (t > b) {
        b
    } else {
        t
    }
}

fun saturate(t: Double): Double {
    return clamp(t, 0.0, 1.0)
}

fun saturate(t: Float): Float {
    return clamp(t, 0f, 1f)
}

fun floor(t: Double): Int {
    return kotlin.math.floor(t).i
}

fun floor(t: Float): Int {
    return kotlin.math.floor(t).i
}

fun ceil(t: Double): Int {
    return kotlin.math.ceil(t).i
}

fun ceil(t: Float): Int {
    return kotlin.math.ceil(t).i
}

fun fract(x: Double): Double {
    return x - floor(x)
}

fun fract(x: Float): Float {
    return x - floor(x)
}

fun Color.asInt(): Int {
    return (255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt()
}

fun circleShape(radius: Double, callback: (Shape) -> Unit) {
    circleShape(radius.f, Point(), callback)
}

fun circleShape(radius: Float, callback: (Shape) -> Unit) {
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
    vec.len = 1.0
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

fun circleShape(radius: Double, middle: Point, callback: (Shape) -> Unit) {
    circleShape(radius.f, middle, callback)
}

fun circleShape(radius: Float, middle: Point, callback: (Shape) -> Unit) {
    val circle = CircleShape()
    circle.radius = radius
    circle.position = middle.v2

    callback(circle)

    circle.dispose()
}

fun ngonShape(radius: Double, sides: Int, callback: (Shape) -> Unit) {
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

fun rectShape(size: Size, middle: Point, angle: Double, callback: (Shape) -> Unit) {
    val rect = PolygonShape()
    rect.setAsBox(size.w.f / 2, size.h.f / 2, middle.v2, angle.f)
    callback(rect)
    rect.dispose()
}

fun lerp(a: Double, b: Double, t: Double): Double {
    return a + (b - a) * t
}

fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

/**
 * If you don't know what you want for speed, for reference a
 * speed of 10.0 is approximately a regular frame-lerp of 0.1
 */
fun frameAccurateLerp(a: Double, b: Double, speed: Double): Double {
    // https://youtu.be/yGhfUcPjXuE?t=1155
    val blend = 0.5.pow(GameTime.GRAPHICS_DELTA * speed)
    return lerp(b, a, blend)
}

fun frameAccurateLerp(a: Float, b: Float, speed: Double): Float {
    return frameAccurateLerp(a.d, b.d, speed).f
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

    return (Perlin.fbm(GameTime.APP_TIME * ferocity + seed * 200.0, 0.0, 3, 2.0) * power * 0.2)
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