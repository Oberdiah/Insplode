package com.oberdiah

import com.badlogic.gdx.utils.ShortArray

class Triangle(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float)

val floatArrLookup = mutableListOf<FloatArray>()
val floatArrLookupScaled = mutableListOf<FloatArray>()
val pointLookupScaled = mutableListOf<List<Point>>()
val pointMiddleLookup = mutableListOf<Point>()
val triangleMarchingLookup = mutableListOf<List<Triangle>>()

fun idx(bottomLeft: Boolean, bottomRight: Boolean, topLeft: Boolean, topRight: Boolean): Int {
    var i = 0
    if (bottomLeft) i += 1
    if (bottomRight) i += 2
    if (topLeft) i += 4
    if (topRight) i += 8
    return i
}

fun marchingSquaresFA(bottomLeft: Boolean, bottomRight: Boolean, topLeft: Boolean, topRight: Boolean): FloatArray {
    return floatArrLookup[idx(bottomLeft, bottomRight, topLeft, topRight)]
}
fun marchingSquaresFAScaled(bottomLeft: Boolean, bottomRight: Boolean, topLeft: Boolean, topRight: Boolean): FloatArray {
    return floatArrLookupScaled[idx(bottomLeft, bottomRight, topLeft, topRight)]
}

fun marchingSquaresTriangles(bottomLeft: Boolean, bottomRight: Boolean, topLeft: Boolean, topRight: Boolean): List<Triangle> {
    return triangleMarchingLookup[idx(bottomLeft, bottomRight, topLeft, topRight)]
}

fun marchingSquaresScaled(bottomLeft: Boolean, bottomRight: Boolean, topLeft: Boolean, topRight: Boolean): List<Point> {
    return pointLookupScaled[idx(bottomLeft, bottomRight, topLeft, topRight)]
}

fun marchingSquaresMiddle(bottomLeft: Boolean, bottomRight: Boolean, topLeft: Boolean, topRight: Boolean): Point {
    return pointMiddleLookup[idx(bottomLeft, bottomRight, topLeft, topRight)]
}

fun initMarchingSquares() {
    for (i in 0..15) {
        val bottomLeft = i.and(1) != 0
        val bottomRight = i.and(2) != 0
        val topLeft = i.and(4) != 0
        val topRight = i.and(8) != 0

        val squarePoints = marchingSquares(bottomLeft, bottomRight, topLeft, topRight)
        val pointsScaled = squarePoints * SIMPLE_SIZE
        pointLookupScaled.add(pointsScaled)
        floatArrLookup.add(pointListToFloatArr(squarePoints))
        val scaledFA = pointListToFloatArr(pointsScaled)
        floatArrLookupScaled.add(scaledFA)
        pointMiddleLookup.add(calculateMarchingSquaresMiddle(bottomLeft, bottomRight, topLeft, topRight))
        triangleMarchingLookup.add(calculateMarchingTriangles(scaledFA))
    }
}

fun calculateMarchingTriangles(vertices: FloatArray): List<Triangle> {
    val arrRes: ShortArray = earClipper.computeTriangles(vertices)
    val list = mutableListOf<Triangle>()
    var i = 0
    while (i < arrRes.size - 2) {
        val x1 = vertices[arrRes[i] * 2]
        val y1 = vertices[arrRes[i] * 2 + 1]
        val x2 = vertices[arrRes[i + 1] * 2]
        val y2 = vertices[arrRes[i + 1] * 2 + 1]
        val x3 = vertices[arrRes[i + 2] * 2]
        val y3 = vertices[arrRes[i + 2] * 2 + 1]
        list.add(Triangle(x1, y1, x2, y2, x3, y3))
        i += 3
    }
    return list
}

private val bl_a = Point(0, 0)
private val br = Point(1, 0)
private val tl = Point(0, 1)
private val tr = Point(1, 1)
private val bm = Point(0.5, 0)
private val lm = Point(0, 0.5)
private val rm = Point(1, 0.5)
private val tm = Point(0.5, 1)
private val a_a = listOf(bl_a, br, rm, tm, tl)
private val b_b = listOf(bl_a, br, tr, tm, lm)
private val c_c = listOf(bl_a, bm, rm, tr, tl)
private val d_d = listOf(bm, br, tr, tl, lm)
private val e_e = listOf(tl, tr, rm, lm)
private val f_f = listOf(br, tr, tm, bm)
private val g_g = listOf(br, bl_a, lm, rm)
private val h_h = listOf(bl_a, tl, tm, bm)
private val i_i = listOf(bl_a, bm, rm, tr, tm, lm)
private val j_j = listOf(br, bm, lm, tl, tm, rm)
private val l_l = listOf(bl_a, bm, lm)
private val m_m = listOf(br, bm, rm)
private val n_n = listOf(tl, tm, lm)
private val o_o = listOf(tr, tm, rm)
private val p_p = listOf(bl_a, br, tr, tl)
private val empty = listOf<Point>()

fun marchingSquares(bottomLeft: Boolean, bottomRight: Boolean, topLeft: Boolean, topRight: Boolean): List<Point> {
    return (if (bottomLeft && bottomRight && topLeft && topRight) {
        p_p
    } else if (bottomLeft && bottomRight && topLeft) {
        a_a
    } else if (bottomLeft && bottomRight && topRight) {
        b_b
    } else if (topLeft && topRight && bottomLeft) {
        c_c
    } else if (topLeft && topRight && bottomRight) {
        d_d
    } else if (topLeft && topRight) {
        e_e
    } else if (bottomRight && topRight) {
        f_f
    } else if (bottomRight && bottomLeft) {
        g_g
    } else if (bottomLeft && topLeft) {
        h_h
    } else if (bottomLeft && topRight) {
        i_i
    } else if (bottomRight && topLeft) {
        j_j
    } else if (bottomLeft) {
        l_l
    } else if (bottomRight) {
        m_m
    } else if (topLeft) {
        n_n
    } else if (topRight) {
        o_o
    } else {
        empty
    })
}

fun calculateMarchingSquaresMiddle(bottomLeft: Boolean, bottomRight: Boolean, topLeft: Boolean, topRight: Boolean): Point {
    return (if (bottomLeft && bottomRight && topLeft && topRight) {
        Point(0.5, 0.5)
    } else if (bottomLeft && bottomRight && topLeft) {
        Point(0.5, 0.5)
    } else if (bottomLeft && bottomRight && topRight) {
        Point(0.5, 0.5)
    } else if (topLeft && topRight && bottomLeft) {
        Point(0.5, 0.5)
    } else if (topLeft && topRight && bottomRight) {
        Point(0.5, 0.5)
    } else if (topLeft && topRight) {
        Point(0.5, 0.75)
    } else if (bottomRight && topRight) {
        Point(0.75, 0.5)
    } else if (bottomRight && bottomLeft) {
        Point(0.5, 0.25)
    } else if (bottomLeft && topLeft) {
        Point(0.25, 0.5)
    } else if (bottomLeft && topRight) {
        Point(0.5, 0.5)
    } else if (bottomRight && topLeft) {
        Point(0.5, 0.5)
    } else if (bottomLeft) {
        Point(0.2, 0.2)
    } else if (bottomRight) {
        Point(0.8, 0.2)
    } else if (topLeft) {
        Point(0.2, 0.8)
    } else if (topRight) {
        Point(0.8, 0.8)
    } else {
        Point(0.5, 0.5)
    })
}