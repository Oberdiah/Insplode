package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.oberdiah.level.getTile
import com.oberdiah.utils.Colors
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme

private lateinit var fbo: FrameBuffer
lateinit var levelShapeRenderer: ShapeRenderer
lateinit var levelTexRenderer: SpriteBatch
lateinit var levelOverlayRenderer: SpriteBatch
val cam = OrthographicCamera()
val cam2 = OrthographicCamera()
val fboLoopSizeInUnits
    // Add 1 so that even on screens where the number of tiles matches perfectly, wrapping isn't seen.
    get() = ceil(SCREEN_HEIGHT_IN_UNITS + 1)
val fboLoopHeightInTiles
    get() = fboLoopSizeInUnits * TILES_PER_UNIT

fun initLevelRender() {
    fbo =
        FrameBuffer(
            Pixmap.Format.RGBA8888,
            WIDTH.i,
            (fboLoopSizeInUnits * UNIT_SIZE_IN_PIXELS).i,
            false
        )
    levelShapeRenderer = ShapeRenderer()
    levelTexRenderer = SpriteBatch()
    levelOverlayRenderer = SpriteBatch()

    cam.setToOrtho(true, WIDTH.f, HEIGHT.f)
    cam2.setToOrtho(false, UNITS_WIDE.f, fboLoopSizeInUnits.f)
    cam.update()
    cam2.position.x = UNITS_WIDE.f / 2
    cam2.update()
    levelTexRenderer.projectionMatrix = cam.combined
    levelOverlayRenderer.projectionMatrix = cam.combined
    levelOverlayRenderer.enableBlending()
}

var fboBaseLocation = 0
var previousLowestTile = 0
fun renderLevel() {
    fboBaseLocation = -floor(CAMERA_POS_Y / fboLoopSizeInUnits)
//    DEBUG_STRING = "${fboBaseLocation}"
    cam2.position.y = fboLoopSizeInUnits.f / 2
    cam2.update()
    levelShapeRenderer.projectionMatrix = cam2.combined

    fbo.begin()
    Gdx.gl.glDisable(GL30.GL_BLEND)
    Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA)
    levelShapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    levelShapeRenderer.color = Colors.transparent

    val lowestTileOnScreen = (CAMERA_POS_Y * TILES_PER_UNIT - 1).i
    val highestTileOnScreen = lowestTileOnScreen + ceil(SCREEN_HEIGHT_IN_UNITS * TILES_PER_UNIT) + 1
    val diff = lowestTileOnScreen - previousLowestTile
    if (diff > 0) {
        // Going up
        for (y in 0..diff) {
            for (tileX in 0 until NUM_TILES_ACROSS) {
                val tile = getTile(tileX, highestTileOnScreen - y)
                renderTile(tile, tileX, highestTileOnScreen - y)
            }
        }

    } else if (diff < 0) {
        // Going down
        for (y in 0..-diff) {
            for (tileX in 0 until NUM_TILES_ACROSS) {
                val tile = getTile(tileX, lowestTileOnScreen + y)
                renderTile(tile, tileX, lowestTileOnScreen + y)
            }
        }
    }

    for (tileId in tileIdsChangedLastFrameMarchingCubes) {
        if (tileId.y in lowestTileOnScreen..highestTileOnScreen) {
            renderTile(getTile(tileId), tileId.x, tileId.y)
        }
    }
    levelShapeRenderer.end()
    fbo.end()

    val yOffset = (-Point(0, 0).ui.y.f + HEIGHT.f - fbo.height.f).i
    val topTexY = (yOffset + ((fboBaseLocation - 1) * fbo.height)).f
    val bottomTexY = (yOffset + ((fboBaseLocation) * fbo.height)).f
    levelTexRenderer.begin()
    levelTexRenderer.draw(fbo.colorBufferTexture, 0f, topTexY, WIDTH.f, fbo.height.f)
    levelTexRenderer.draw(fbo.colorBufferTexture, 0f, bottomTexY, WIDTH.f, fbo.height.f)
    levelTexRenderer.end()

    val translateY = (UNIT_SIZE_IN_PIXELS * TILE_SIZE_IN_UNITS / 2).f
    Gdx.gl.glBlendEquation(GL30.GL_FUNC_REVERSE_SUBTRACT)
    levelOverlayRenderer.begin()
    val worldSprite = Sprite(fbo.colorBufferTexture)
    worldSprite.x = 0f
    worldSprite.y = topTexY + translateY
    worldSprite.color = Color(1f, 1f, 1f, 0.15f)
    worldSprite.draw(levelOverlayRenderer)
    worldSprite.y = bottomTexY + translateY
    worldSprite.draw(levelOverlayRenderer)
    levelOverlayRenderer.end()
    Gdx.gl.glBlendEquation(GL30.GL_FUNC_ADD)

    previousLowestTile = lowestTileOnScreen
}

private fun renderTile(tile: TileLike, tileX: Int, tileY: Int) {
    levelShapeRenderer.color = Colors.transparent

    val yRenderingOffset = -floor(tileY / fboLoopHeightInTiles.f) * fboLoopSizeInUnits

    val x = (tileX * TILE_SIZE_IN_UNITS).f
    val y = (tileY * TILE_SIZE_IN_UNITS).f + yRenderingOffset

    // Clear the old tile
    levelShapeRenderer.rect(x, y, TILE_SIZE_IN_UNITS.f, TILE_SIZE_IN_UNITS.f)

    if (tile !is Tile) {
        return
    }

    val bottomLeft = tile
    val topLeft = tile.data.tm
    val bottomRight = tile.data.rm
    val topRight = tile.data.tr

    for (tileType in TileType.values()) {
        // Fun wee rendering hack to fill in holes
        val bl = bottomLeft.doesExist() && bottomLeft.getTileType().ordinal >= tileType.ordinal
        val tl = topLeft.doesExist() && topLeft.getTileType().ordinal >= tileType.ordinal
        val br = bottomRight.doesExist() && bottomRight.getTileType().ordinal >= tileType.ordinal
        val tr = topRight.doesExist() && topRight.getTileType().ordinal >= tileType.ordinal

        if (bl || br || tl || tr) {
            levelShapeRenderer.color = tileType.color()
            for (tri in marchingSquaresTriangles(bl, br, tl, tr)) {
                levelShapeRenderer.triangle(
                    tri.x1 + x,
                    tri.y1 + y,
                    tri.x2 + x,
                    tri.y2 + y,
                    tri.x3 + x,
                    tri.y3 + y
                )
            }
        }
    }
}

fun renderBackground(r: Renderer) {
    for (tx in 0 until UNITS_WIDE) {
        val numYSquares = (SCREEN_HEIGHT_IN_UNITS + 2).i
        for (y in 0 until numYSquares) {
            val ty = y + floor(CAMERA_POS_Y)

            var thisColor = colorScheme.backgroundA
            if ((ty + tx) % 2 == 0) {
                thisColor = colorScheme.backgroundB
            }

            // Make thisColor darker as we go up (ty increases)
            thisColor = thisColor.cpy().add(
                min(0.05f * (ty / 50f), 0.1f).f,
                min(0.10f * (ty / 50f), 0.1f).f,
                min(0.15f * (ty / 50f), 0.1f).f,
                0.0f
            )

            r.color = thisColor

            r.rect(tx, ty, 1, 1)
        }
    }
}