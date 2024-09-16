package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.oberdiah.utils.Colors
import com.oberdiah.utils.TileType
import com.oberdiah.utils.colorScheme

private lateinit var fbo: FrameBuffer
lateinit var levelShapeRenderer: ShapeRenderer
lateinit var levelTexRenderer: SpriteBatch
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

    cam.setToOrtho(true, WIDTH.f, HEIGHT.f)
    cam2.setToOrtho(false, UNITS_WIDE.f, fboLoopSizeInUnits.f)
    cam.update()
    cam2.position.x = UNITS_WIDE.f / 2
    cam2.update()
    levelTexRenderer.projectionMatrix = cam.combined
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

    for (tileId in tileIdsChangedLastFrame) {
        if (tileId.y in lowestTileOnScreen..highestTileOnScreen) {
            renderTile(getTile(tileId), tileId.x, tileId.y)
        }
    }
    levelShapeRenderer.end()
    fbo.end()

    val yOffset = -Point(0, 0).ui.y.f + HEIGHT.f - fbo.height.f
    levelTexRenderer.begin()
    levelTexRenderer.draw(
        fbo.colorBufferTexture,
        0f,
        yOffset + ((fboBaseLocation - 1) * fbo.height.f),
        WIDTH.f,
        fbo.height.f
    )
    levelTexRenderer.draw(
        fbo.colorBufferTexture,
        0f,
        yOffset + ((fboBaseLocation) * fbo.height.f),
        WIDTH.f,
        fbo.height.f
    )
    levelTexRenderer.end()


    previousLowestTile = lowestTileOnScreen
//    levelShapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
//    renderLevelInFull(levelShapeRenderer)
//    levelShapeRenderer.end()
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

            r.color = colorScheme.backgroundA

            if ((ty + tx) % 2 == 0) {
                r.color = colorScheme.backgroundB
            }

            r.rect(tx, ty, 1, 1)
        }
    }
}


fun renderLevelOld(r: Renderer) {
    for (tileType in TileType.values()) {
        for (bottomLeft in levelOnScreen) {
            val x = bottomLeft.x
            val y = bottomLeft.y

            val topLeft = getTile(x, y + 1)
            val bottomRight = getTile(x + 1, y)
            val topRight = getTile(x + 1, y + 1)

//            val tileType = bottomLeft.tileType

            // Fun wee rendering hack to fill in holes
            val bl = bottomLeft.doesExist() && bottomLeft.getTileType().ordinal >= tileType.ordinal
            val tl = topLeft.doesExist() && topLeft.getTileType().ordinal >= tileType.ordinal
            val br =
                bottomRight.doesExist() && bottomRight.getTileType().ordinal >= tileType.ordinal
            val tr = topRight.doesExist() && topRight.getTileType().ordinal >= tileType.ordinal

            if (bl || br || tl || tr) {
//                r.color = tileType.color().cpy().add(-0.3f, -0.3f, -0.3f, 0.0f)
//                r.poly(
//                    marchingSquaresFAScaled(bl, br, tl, tr),
//                    1,
//                    x * TILE_SIZE + 0.1,
//                    y * TILE_SIZE + 0.1
//                )
                r.color = tileType.color()
                r.poly(
                    marchingSquaresFAScaled(bl, br, tl, tr),
                    1,
                    x * TILE_SIZE_IN_UNITS,
                    y * TILE_SIZE_IN_UNITS
                )
            }
        }
    }
}