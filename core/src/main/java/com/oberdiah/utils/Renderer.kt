package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ShortArray
import kotlin.math.PI


private val Number.toDegrees: Number
    get() { return this * 180/3.1415 }

class TextDraw(val color: Color, val font: BitmapFont, val text: String, val x: Number, val y: Number, val align: Int)

val earClipper = EarClippingTriangulator()

class Renderer(val name: String) {
    val renderer = ShapeRenderer()
    val textRenderer = SpriteBatch()
    val textToDraw = mutableListOf<TextDraw>()

    fun begin() {
        Gdx.gl.glEnable(GL30.GL_BLEND)
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA)
        renderer.begin(ShapeType.Filled)
        Gdx.gl.glLineWidth(10f)
    }

    fun end() {
        renderer.end()
        if (textToDraw.size > 0) {
            textRenderer.begin()
            textToDraw.forEach {
                it.font.color = it.color
                var y = it.y.f
                if (it.align == Align.center || it.align == Align.left || it.align == Align.right) {
                    y += it.font.capHeight/2
                }
                if (it.align == Align.bottomLeft) {
                    y += it.font.capHeight
                }
                it.font.draw(textRenderer, it.text, it.x.f, y, 0f, it.align, false)
            }
            textToDraw.clear()
            textRenderer.end()
        }
    }

    var color: Color
        get() = renderer.color
        set(c) {
            renderer.color = c
        }

    fun text(font: BitmapFont, text: String, p: Point, align: Int = Align.bottomLeft) {
        textToDraw.add(TextDraw(renderer.color.cpy(), font, text, p.x, p.y, align))
    }

    fun text(font: BitmapFont, text: String, x: Number, y: Number, align: Int = Align.bottomLeft) {
        textToDraw.add(TextDraw(renderer.color.cpy(), font, text, x, y, align))
    }

    fun rect(rect: Rect) {
        renderer.rect(rect.p.x.f, rect.p.y.f, rect.s.w.f, rect.s.h.f)
    }

    fun rect(x: Number, y: Number, w: Number, h: Number) {
        renderer.rect(x.f, y.f, w.f, h.f)
    }

    fun rect(p: Point, s: Size) {
        renderer.rect(p.x.f, p.y.f, s.w.f, s.h.f)
    }

    fun rect(p: Point, w: Number, h: Number) {
        renderer.rect(p.x.f, p.y.f, w.f, h.f)
    }

    fun rect(p: Point, w: Number, h: Number, a: Number) {
        renderer.rect(p.x.f, p.y.f, w.f / 2, h.f / 2, w.f, h.f, 1f, 1f, a.toDegrees.f)
    }

    fun rect(p: Point, s: Size, a: Number) {
        renderer.rect(p.x.f, p.y.f, s.w.f / 2, s.h.f / 2, s.w.f, s.h.f, 1f, 1f, a.toDegrees.f)
    }

    fun centeredRect(mid: Point, w: Number, h: Number, a: Number) {
        renderer.rect(mid.x.f - w.f / 2, mid.y.f - h.f / 2, w.f / 2, h.f / 2, w.f, h.f, 1f, 1f, a.toDegrees.f)
    }

    fun centeredRect(mid: Point, s: Size, a: Number) {
        renderer.rect(mid.x.f - s.w.f / 2, mid.y.f - s.h.f / 2, s.w.f / 2, s.h.f / 2, s.w.f, s.h.f, 1f, 1f, a.toDegrees.f)
    }

    fun centeredRect(mid: Point, s: Size) {
        renderer.rect(mid.x.f - s.w.f / 2, mid.y.f - s.h.f / 2, s.w.f, s.h.f)
    }

    fun polyLine(ps: List<Point>) {
        val arr = FloatArray(ps.size * 2)
        ps.forEachIndexed { index, point ->
            arr[index * 2] = point.x.f
            arr[index * 2 + 1] = point.y.f
        }
        renderer.polygon(arr)
    }

    fun arcFrom0(p: Point, radius: Number, fraction: Number, segments: Int = 20) {
        arc(p, radius, 0, fraction * Math.PI * 2, segments)
    }

    fun arc(p: Point, radius: Number, start: Number, end: Number, segments: Int = 20) {
        renderer.arc(p.x.f, p.y.f, radius.f, start.toDegrees.f, end.toDegrees.f, segments)
    }

    fun poly(points: Collection<Point>, offset: Point, angle: Number) {
        val arr = FloatArray(points.size * 2)
        val cos = cos(angle)
        val sin = sin(angle)
        points.forEachIndexed { index, point ->
            val x = (point.x * cos - point.y * sin)
            val y = (point.x * sin + point.y * cos)
            arr[index * 2] = x.f
            arr[index * 2 + 1] = y.f
        }
        poly(arr, 1.0, offset.x, offset.y)
    }

    fun poly(vertices: FloatArray, scale1: Number, offsetX1: Number, offsetY1: Number) {
        val scale = scale1.f
        val offsetX = offsetX1.f
        val offsetY = offsetY1.f
        val arrRes: ShortArray = earClipper.computeTriangles(vertices)
        var i = 0
        while (i < arrRes.size - 2) {
            val x1 = vertices[arrRes[i] * 2] * scale + offsetX
            val y1 = vertices[arrRes[i] * 2 + 1] * scale + offsetY
            val x2 = vertices[arrRes[i + 1] * 2] * scale + offsetX
            val y2 = vertices[arrRes[i + 1] * 2 + 1] * scale + offsetY
            val x3 = vertices[arrRes[i + 2] * 2] * scale + offsetX
            val y3 = vertices[arrRes[i + 2] * 2 + 1] * scale + offsetY
            renderer.triangle(x1, y1, x2, y2, x3, y3)
            i += 3
        }
    }

    fun circle(p: Point, rad: Number, segments: Int = 20) {
        renderer.circle(p.x.f, p.y.f, rad.f, segments)
    }

    fun circle(x: Number, y: Number, rad: Number, segments: Int = 20) {
        renderer.circle(x.f, y.f, rad.f, segments)
    }

    fun lineCircle(p: Point, radius: Number, width: Number, segments: Int = 20) {
        lineCircle(p.x, p.y, radius, width, segments)
    }

    fun lineCircle(x: Number, y: Number, radius: Number, width: Number, segments: Int = 20) {
        lineCircle(x.d, y.d, radius.d, width.d, segments)
    }

    fun line(a: Point, b: Point, width: Number) {
        line(a.x, a.y, b.x, b.y, width)
    }

    fun line(x1: Number, y1: Number, x2: Number, y2: Number, width: Number) {
        renderer.rectLine(x1.f, y1.f, x2.f, y2.f, width.f)
    }

    fun ngon(p: Point, radius: Number, angle: Number, sides: Int) {
        var previousX = (cos(angle + 2 * 0 * PI/sides ) * radius) + p.x
        var previousY = (sin(angle + 2 * 0 * PI/sides ) * radius) + p.y
        for (i in 1..sides) {
            val x = (cos(angle + 2 * i * PI/sides ) * radius) + p.x
            val y = (sin(angle + 2 * i * PI/sides ) * radius) + p.y
            renderer.triangle(p.x.f, p.y.f, previousX.f, previousY.f, x.f, y.f)
            previousX = x
            previousY = y
        }
    }

    fun ngonLine(p: Point, radius: Number, angle: Number, width: Number, sides: Int) {
        var previousX = (cos(angle + 2 * 0 * PI/sides ) * radius) + p.x
        var previousY = (sin(angle + 2 * 0 * PI/sides ) * radius) + p.y
        for (i in 1..sides) {
            val x = (cos(angle + 2 * i * PI/sides ) * radius) + p.x
            val y = (sin(angle + 2 * i * PI/sides ) * radius) + p.y
            line(previousX, previousY, x, y, width)
            previousX = x
            previousY = y
        }
    }

    private fun lineCircle(x: Double, y: Double, radius: Double, width: Double, segments: Int) {
        require(segments > 0) { "segments must be > 0." }
        val angle = 2 * MathUtils.PI / segments
        val cos = MathUtils.cos(angle)
        val sin = MathUtils.sin(angle)
        var cx = radius
        var cy = 0.0

        for (i in 0 until segments) {
            val x1 = x + cx
            val y1 = y + cy
            val temp = cx
            cx = cos * cx - sin * cy
            cy = sin * temp + cos * cy
            line(x1, y1, x + cx, y + cy, width)
        }
        // Ensure the last segment is identical to the first.

        val x1 = x + cx
        val y1 = y + cy

        cx = radius
        cy = 0.0
        line(x1, y1, x + cx, y + cy, width)
    }
}