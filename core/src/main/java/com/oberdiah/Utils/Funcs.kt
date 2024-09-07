package com.oberdiah

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.Shape
import com.oberdiah.Utils.camera
import kotlin.math.PI
import kotlin.random.Random

fun toUISpace(p: Point): Point {
    val coords = camera.project(Vector3(p.x.f, p.y.f, 0F))
    return Point(coords.x, coords.y)
}

fun toWorldSpace(p: Point): Point {
    val coords = camera.unproject(Vector3(p.x.f, HEIGHT.f - p.y.f, 0F))
    return Point(coords.x, coords.y)
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
fun min(a: Number, b: Number): Double {
    return a.toDouble().coerceAtMost(b.toDouble())
}
fun min(a: Int, b: Int): Int {
    return a.coerceAtMost(b)
}

fun Number.format(digits: Int) = "%.${digits}f".format(this)

val Number.d: Double
    get() { return this.toDouble() }

val Number.i: Int
    get() { return this.toInt() }

val Number.f: Float
    get() { return this.toFloat() }

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
    return min(max(t, a), b)
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

fun <T, R> T?.notNull(f: (T) -> R){
    if (this != null){
        f(this)
    }
}

// Finds the normal of the line, pointing towards the side specified by "side"
fun lineNormal(p0: Point, p1: Point, side: Point): Point {
    val z = (p1.x-p0.x)*(side.y-p1.y) - (p1.y-p0.y)*(side.x-p1.x)
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
    val t = ( s2.x * (p0.y - p2.y) - s2.y * (p0.x - p2.x)) / (-s2.x * s1.y + s1.x * s2.y)

    if (s >= 0 && s <= 1 && t >= 0 && t <= 1)
    {
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
        points[i].x = (cos(2 * i * PI/sides ) * radius).f
        points[i].y = (sin(2 * i * PI/sides ) * radius).f
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