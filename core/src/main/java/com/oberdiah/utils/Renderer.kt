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
import kotlin.math.atan2


private val Double.toDegrees: Double
    get() {
        return this * 180 / 3.1415
    }

abstract class Drawable {
    abstract fun draw(renderer: SpriteBatch, camera: Camera)
}

class SpriteDraw(
    val sprite: Sprite,
    val p: Point,
    val s: Size,
    val color: Color = Color.WHITE,
    val angle: Double
) :
    Drawable() {
    override fun draw(renderer: SpriteBatch, camera: Camera) {
        // The sprite renderer isn't projected to make the text look correct
        // So we need to do it manually
        val p = project(p, camera)

        if (p.y > HEIGHT * 1.2 || p.y < -HEIGHT * 0.2 || color.a < 0.001f) {
            return
        }

        val s = s * SCREEN_SIZE / Size(camera.viewportWidth, camera.viewportHeight)
        sprite.setBounds(p.x.f, p.y.f, s.x.f, s.y.f)
        sprite.color = color
        sprite.rotation = angle.f
        sprite.setOrigin(s.x.f / 2, s.y.f / 2)
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

        if (pos.y > HEIGHT * 1.2 || pos.y < -HEIGHT * 0.2 || color.a < 0.001f) {
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

    fun centeredSprite(
        sprite: Sprite,
        p: Point,
        scale: Double,
        color: Color = Color.WHITE,
        angle: Double = 0.0
    ) {
        val textureSize = Size(sprite.width, sprite.height)
        val scaled = scale / max(textureSize.w, textureSize.h).f
        val size = textureSize * scaled
        centeredSprite(sprite, p, size, color, angle)
    }

    fun centeredSprite(
        sprite: Sprite,
        p: Point,
        s: Size,
        color: Color = Color.WHITE,
        angle: Double = 0.0
    ) {
        sprite(sprite, p - s / 2, s, color, angle)
    }

    fun sprite(sprite: Sprite, p: Point, s: Size, color: Color = Color.WHITE, angle: Double = 0.0) {
        spritesToDraw.add(SpriteDraw(sprite, p, s, color.cpy(), angle))
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
        x: Double,
        y: Double,
        align: Int = Align.bottomLeft,
        shouldCache: Boolean = true
    ) {
        text(font, text, Point(x, y), align, shouldCache)
    }

    fun hollowRect(rect: Rect, width: Double) {
        centeredHollowRect(rect.p + rect.s / 2, rect.s, width)
    }

    fun rect(rect: Rect) {
        shapeRenderer.rect(rect.p.x.f, rect.p.y.f, rect.s.w.f, rect.s.h.f)
    }

    fun rect(x: Double, y: Double, w: Double, h: Double) {
        shapeRenderer.rect(x.f, y.f, w.f, h.f)
    }

    fun rect(x: Double, y: Double, w: Double, h: Double, a: Double) {
        shapeRenderer.rect(x.f, y.f, w.f / 2, h.f / 2, w.f, h.f, 1f, 1f, a.toDegrees.f)
    }

    fun rect(p: Point, s: Size) {
        shapeRenderer.rect(p.x.f, p.y.f, s.w.f, s.h.f)
    }

    fun rect(p: Point, w: Double, h: Double) {
        shapeRenderer.rect(p.x.f, p.y.f, w.f, h.f)
    }

    fun rect(p: Point, w: Double, h: Double, a: Double) {
        shapeRenderer.rect(p.x.f, p.y.f, w.f / 2, h.f / 2, w.f, h.f, 1f, 1f, a.toDegrees.f)
    }

    fun rect(p: Point, s: Size, a: Double) {
        shapeRenderer.rect(p.x.f, p.y.f, s.w.f / 2, s.h.f / 2, s.w.f, s.h.f, 1f, 1f, a.toDegrees.f)
    }

    fun centeredRect(mid: Point, w: Double, h: Double, a: Double) {
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

    fun centeredRect(mid: Point, s: Size, a: Double) {
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

    fun centeredHollowRect(mid: Point, s: Size, width: Double) {
        shapeRenderer.rect(mid.x.f - s.w.f / 2, mid.y.f - s.h.f / 2, s.w.f, width.f)
        shapeRenderer.rect(mid.x.f - s.w.f / 2, mid.y.f + s.h.f / 2 - width.f, s.w.f, width.f)
        shapeRenderer.rect(mid.x.f - s.w.f / 2, mid.y.f - s.h.f / 2, width.f, s.h.f)
        shapeRenderer.rect(mid.x.f + s.w.f / 2 - width.f, mid.y.f - s.h.f / 2, width.f, s.h.f)
    }

    fun polyLine(ps: List<Point>, offset: Point = Point(), width: Double = 1.0) {
        for (i in 0 until ps.size - 1) {
            val x1 = ps[i].x + offset.x
            val y1 = ps[i].y + offset.y
            val x2 = ps[i + 1].x + offset.x
            val y2 = ps[i + 1].y + offset.y

            // Extend the line slightly to hide the gap between lines
            val angle = atan2(y2 - y1, x2 - x1)

            // This is a magic number that seems to work well
            val widthScaling = 3.0

            val x1e = x1 - cos(angle) * width / widthScaling
            val y1e = y1 - sin(angle) * width / widthScaling
            val x2e = x2 + cos(angle) * width / widthScaling
            val y2e = y2 + sin(angle) * width / widthScaling

            line(x1e, y1e, x2e, y2e, width)
        }
    }

    fun arcFrom0(p: Point, radius: Double, fraction: Double, segments: Int = 20) {
        arc(p, radius, 0.0, clamp(fraction * Math.PI * 2, 0.0, Math.PI * 2), segments)
    }

    fun arc(p: Point, radius: Double, start: Double, end: Double, segments: Int = 20) {
        shapeRenderer.arc(p.x.f, p.y.f, radius.f, start.toDegrees.f, end.toDegrees.f, segments)
    }

    fun poly(points: Collection<Point>, offset: Point, angle: Double) {
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

    fun poly(vertices: FloatArray, scale1: Double, offsetX1: Double, offsetY1: Double) {
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

    private val starFloatArr: FloatArray = FloatArray(20)
    fun star(center: Point, radius: Double, angle: Double = Math.PI / 10) {
        for (i in 0 until 5) {
            val angle1 = angle + i * 2 * PI / 5
            val angle2 = angle + (i + 0.5) * 2 * PI / 5
            starFloatArr[i * 4] = (center.x + cos(angle1) * radius).f
            starFloatArr[i * 4 + 1] = (center.y + sin(angle1) * radius).f
            starFloatArr[i * 4 + 2] = (center.x + cos(angle2) * radius / 2).f
            starFloatArr[i * 4 + 3] = (center.y + sin(angle2) * radius / 2).f
        }
        poly(starFloatArr, 1.0, 0.0, 0.0)
    }

    fun circle(p: Point, rad: Double, segments: Int = 20) {
        shapeRenderer.circle(p.x.f, p.y.f, rad.f, segments)
    }

    fun circle(x: Double, y: Double, rad: Double, segments: Int = 20) {
        shapeRenderer.circle(x.f, y.f, rad.f, segments)
    }

    fun lineCircle(p: Point, radius: Double, width: Double, segments: Int = 20) {
        lineCircle(p.x, p.y, radius, width, segments)
    }

    fun line(a: Point, b: Point, width: Double) {
        line(a.x, a.y, b.x, b.y, width)
    }

    fun line(x1: Double, y1: Double, x2: Double, y2: Double, width: Double) {
        shapeRenderer.rectLine(x1.f, y1.f, x2.f, y2.f, width.f)
    }

    fun ngon(p: Point, radius: Double, angle: Double, sides: Int) {
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

    fun ngonLine(p: Point, radius: Double, angle: Double, width: Double, sides: Int) {
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
