package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
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
    get() {
        return this * 180 / 3.1415
    }

abstract class Drawable {
    abstract fun draw(renderer: SpriteBatch, camera: Camera)
}

class SpriteDraw(val sprite: Sprite, val p: Point, val s: Size, val color: Color = Color.WHITE) :
    Drawable() {
    override fun draw(renderer: SpriteBatch, camera: Camera) {
        // The sprite renderer isn't projected to make the text look correct
        // So we need to do it manually
        val p = project(p, camera)

        if (p.y > HEIGHT * 1.2 || p.y < -HEIGHT * 0.2) {
            return
        }

        val s = s * SCREEN_SIZE / Size(camera.viewportWidth, camera.viewportHeight)
        sprite.x = p.x.f
        sprite.y = p.y.f
        sprite.setSize(s.x.f, s.y.f)
        sprite.color = color
        sprite.draw(renderer)
    }
}

class TextDraw(
    val color: Color,
    val font: BitmapFont,
    val text: String,
    val p: Point,
    val align: Int,
    val shouldCache: Boolean = true
) : Drawable() {
    override fun draw(renderer: SpriteBatch, camera: Camera) {
        // The sprite renderer isn't projected to make the text look correct
        // So we need to compensate for that
        val pos = project(p, camera)

        if (pos.y > HEIGHT * 1.2 || pos.y < -HEIGHT * 0.2) {
            return
        }

        var y = pos.y.f
        if (align == Align.center || align == Align.left || align == Align.right) {
            y += font.capHeight / 2
        }
        if (align == Align.bottomLeft) {
            y += font.capHeight
        }

        if (shouldCache) {
            val fontCache = getFontCache(font, align, text)
            if (fontCache.color != color) {
                fontCache.color = color

                // This is quite an expensive operation, but I don't think we can really avoid it
                // as we need to be able to do alpha fades. We at least skip it
                // as much as we possibly can.
                fontCache.setColors(color)
            }
            fontCache.setPosition(pos.x.f, y)
            fontCache.draw(renderer)
        } else {
            font.color = color
            font.draw(renderer, text, pos.x.f, y, 0f, align, false)
        }

    }
}

val earClipper = EarClippingTriangulator()

class Renderer(val name: String, val camera: Camera) {
    private val shapeRenderer = ShapeRenderer()
    private val spriteRenderer = SpriteBatch()
    private val spritesToDraw = mutableListOf<Drawable>()

    fun begin() {
        Gdx.gl.glEnable(GL30.GL_BLEND)
        Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeType.Filled)
        Gdx.gl.glLineWidth(10f)
    }

    fun end() {
        shapeRenderer.end()
        if (spritesToDraw.size > 0) {
            spriteRenderer.begin()
            spritesToDraw.forEach {
                it.draw(spriteRenderer, camera)
            }
            spritesToDraw.clear()
            spriteRenderer.end()
        }
    }

    var color: Color
        get() = shapeRenderer.color
        set(c) {
            shapeRenderer.color = c
        }

    fun sprite(sprite: Sprite, p: Point, s: Size, color: Color = Color.WHITE) {
        spritesToDraw.add(SpriteDraw(sprite, p, s, color))
    }

    fun centeredSprite(sprite: Sprite, p: Point, s: Size, color: Color = Color.WHITE) {
        sprite(sprite, p - s / 2, s, color)
    }

    fun text(
        font: BitmapFont,
        text: String,
        p: Point,
        align: Int = Align.bottomLeft,
        shouldCache: Boolean = true
    ) {
        spritesToDraw.add(TextDraw(shapeRenderer.color.cpy(), font, text, p, align, shouldCache))
    }

    fun text(
        font: BitmapFont,
        text: String,
        x: Number,
        y: Number,
        align: Int = Align.bottomLeft,
        shouldCache: Boolean = true
    ) {
        text(font, text, Point(x, y), align, shouldCache)
    }

    fun rect(rect: Rect) {
        shapeRenderer.rect(rect.p.x.f, rect.p.y.f, rect.s.w.f, rect.s.h.f)
    }

    fun rect(x: Number, y: Number, w: Number, h: Number) {
        shapeRenderer.rect(x.f, y.f, w.f, h.f)
    }

    fun rect(p: Point, s: Size) {
        shapeRenderer.rect(p.x.f, p.y.f, s.w.f, s.h.f)
    }

    fun rect(p: Point, w: Number, h: Number) {
        shapeRenderer.rect(p.x.f, p.y.f, w.f, h.f)
    }

    fun rect(p: Point, w: Number, h: Number, a: Number) {
        shapeRenderer.rect(p.x.f, p.y.f, w.f / 2, h.f / 2, w.f, h.f, 1f, 1f, a.toDegrees.f)
    }

    fun rect(p: Point, s: Size, a: Number) {
        shapeRenderer.rect(p.x.f, p.y.f, s.w.f / 2, s.h.f / 2, s.w.f, s.h.f, 1f, 1f, a.toDegrees.f)
    }

    fun centeredRect(mid: Point, w: Number, h: Number, a: Number) {
        shapeRenderer.rect(
            mid.x.f - w.f / 2,
            mid.y.f - h.f / 2,
            w.f / 2,
            h.f / 2,
            w.f,
            h.f,
            1f,
            1f,
            a.toDegrees.f
        )
    }

    fun centeredRect(mid: Point, s: Size, a: Number) {
        shapeRenderer.rect(
            mid.x.f - s.w.f / 2,
            mid.y.f - s.h.f / 2,
            s.w.f / 2,
            s.h.f / 2,
            s.w.f,
            s.h.f,
            1f,
            1f,
            a.toDegrees.f
        )
    }

    fun centeredRect(mid: Point, s: Size) {
        shapeRenderer.rect(mid.x.f - s.w.f / 2, mid.y.f - s.h.f / 2, s.w.f, s.h.f)
    }

    fun centeredHollowRect(mid: Point, s: Size, width: Number) {
        shapeRenderer.rect(mid.x.f - s.w.f / 2, mid.y.f - s.h.f / 2, s.w.f, width.f)
        shapeRenderer.rect(mid.x.f - s.w.f / 2, mid.y.f + s.h.f / 2 - width.f, s.w.f, width.f)
        shapeRenderer.rect(mid.x.f - s.w.f / 2, mid.y.f - s.h.f / 2, width.f, s.h.f)
        shapeRenderer.rect(mid.x.f + s.w.f / 2 - width.f, mid.y.f - s.h.f / 2, width.f, s.h.f)
    }

    fun polyLine(ps: List<Point>) {
        val arr = FloatArray(ps.size * 2)
        ps.forEachIndexed { index, point ->
            arr[index * 2] = point.x.f
            arr[index * 2 + 1] = point.y.f
        }
        shapeRenderer.polygon(arr)
    }

    fun arcFrom0(p: Point, radius: Number, fraction: Number, segments: Int = 20) {
        arc(p, radius, 0, clamp(fraction * Math.PI * 2, 0.0, Math.PI * 2), segments)
    }

    fun arc(p: Point, radius: Number, start: Number, end: Number, segments: Int = 20) {
        shapeRenderer.arc(p.x.f, p.y.f, radius.f, start.toDegrees.f, end.toDegrees.f, segments)
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
            shapeRenderer.triangle(x1, y1, x2, y2, x3, y3)
            i += 3
        }
    }

    fun circle(p: Point, rad: Number, segments: Int = 20) {
        shapeRenderer.circle(p.x.f, p.y.f, rad.f, segments)
    }

    fun circle(x: Number, y: Number, rad: Number, segments: Int = 20) {
        shapeRenderer.circle(x.f, y.f, rad.f, segments)
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
        shapeRenderer.rectLine(x1.f, y1.f, x2.f, y2.f, width.f)
    }

    fun ngon(p: Point, radius: Number, angle: Number, sides: Int) {
        var previousX = (cos(angle + 2 * 0 * PI / sides) * radius) + p.x
        var previousY = (sin(angle + 2 * 0 * PI / sides) * radius) + p.y
        for (i in 1..sides) {
            val x = (cos(angle + 2 * i * PI / sides) * radius) + p.x
            val y = (sin(angle + 2 * i * PI / sides) * radius) + p.y
            shapeRenderer.triangle(p.x.f, p.y.f, previousX.f, previousY.f, x.f, y.f)
            previousX = x
            previousY = y
        }
    }

    fun ngonLine(p: Point, radius: Number, angle: Number, width: Number, sides: Int) {
        var previousX = (cos(angle + 2 * 0 * PI / sides) * radius) + p.x
        var previousY = (sin(angle + 2 * 0 * PI / sides) * radius) + p.y
        for (i in 1..sides) {
            val x = (cos(angle + 2 * i * PI / sides) * radius) + p.x
            val y = (sin(angle + 2 * i * PI / sides) * radius) + p.y
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
