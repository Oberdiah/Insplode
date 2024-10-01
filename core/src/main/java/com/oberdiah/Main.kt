package com.oberdiah

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.*
import com.oberdiah.level.LOWEST_TILE_Y_UNITS
import com.oberdiah.level.initTiles
import com.oberdiah.level.renderLaser
import com.oberdiah.level.resetLevel
import com.oberdiah.level.resetLevelCollapse
import com.oberdiah.level.resetLevelController
import com.oberdiah.level.tickCollapse
import com.oberdiah.level.tickLevelController
import com.oberdiah.level.updateLevelStorage
import com.oberdiah.player.playerInputs
import com.oberdiah.player.playerState
import com.oberdiah.utils.*
import com.oberdiah.ui.renderUIScreenSpace
import com.oberdiah.ui.renderUIWorldSpace
import com.oberdiah.upgrades.initUpgradeController
import com.oberdiah.upgrades.renderUpgradeMenuScreenSpace
import com.oberdiah.upgrades.tickUpgradeController


lateinit var worldSpaceRenderer: Renderer
lateinit var uiRenderer: Renderer
lateinit var platformInterface: PlatformInterface

fun restartGame() {
    resetParticles()
    resetPhysicsObjects()
    resetLevel()
    resetLevelController()
    resetLevelCollapse()
    updateTilePhysics()
    resetCamera()
    resetScoreSystem()
    setCameraGlobalsThisFrame()
    updateCamera()
    updateLevelStorage()
}

private lateinit var leftWall: PhysBody
private lateinit var rightWall: PhysBody
private lateinit var ceiling: PhysBody
private lateinit var floor: PhysBody

class Main(print: PlatformInterface) : InputAdapter(), ApplicationListener {
    init {
        platformInterface = print
    }

    override fun create() {
        resetCamera()
        setGlobalsThisFrame()
        initMarchingSquares()
        initLevelRender()
        initUpgradeController()

        worldSpaceRenderer = Renderer("World Space Renderer", camera)
        uiRenderer = Renderer("UI Renderer", screenCamera)
        initWorld()
        initTiles()

        loadFiles()
        loadFonts()
        loadSounds()

        leftWall = createWall(Rect(Point(), Size(1, WALL_HEIGHT)))
        rightWall = createWall(Rect(Point(), Size(1, WALL_HEIGHT)))
        ceiling = createWall(Rect(Point(), Size(SCREEN_WIDTH_IN_UNITS, 1)))
        floor = createWall(Rect(Point(), Size(SCREEN_WIDTH_IN_UNITS, 1)))

        restartGame()
    }

    override fun render() {
        Gdx.gl.glClearColor(0.0f, 0.2f, 0.3f, 1f)
        // When the software delay happens - let's do calculations after that.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        timerStart()

        time("Set globals this frame") { setGlobalsThisFrame() }
        time("Calculate globals") { calculateGlobals() }
        time("Save State") { saveState() }
        time("Tick Sounds") { tickSounds() }

        time("Camera") { updateCamera() }
        setCameraGlobalsThisFrame()

        time("Update level storage") { updateLevelStorage() }
        time("Update tile changes") { updateTileChanges() }
        if (GAME_STATE != GameState.PausedPopup) {
            time("Tick Particles") { tickParticles() }
        }

        if (GAME_IS_RUNNING) {
            // 'Coz Box2d stupid, these are the centres of the positions.
            leftWall.setTransform(Point(-0.5, LOWEST_TILE_Y_UNITS + WALL_HEIGHT / 2), 0f)
            rightWall.setTransform(Point(10.5, LOWEST_TILE_Y_UNITS + WALL_HEIGHT / 2), 0f)

            if (CAMERA_FOLLOWING == CameraFollowing.Player) {
                ceiling.setTransform(
                    Point(
                        SCREEN_WIDTH_IN_UNITS / 2.0,
                        LOWEST_TILE_Y_UNITS + WALL_HEIGHT
                    ), 0f
                )
                floor.setTransform(Point(SCREEN_WIDTH_IN_UNITS / 2.0, LOWEST_TILE_Y_UNITS), 0f)
            } else {
                ceiling.setTransform(Point(-5, 0), 0f)
                floor.setTransform(Point(-5, 0), 0f)
            }


            if (GAME_STATE == GameState.InGame && !playerState.isDead) {
                time("Tick level controller") { tickLevelController() }
            }
            time("Tick Collapse") { tickCollapse() }
            time("Tick Physics Objects") { tickPhysicsObjects() }
            time("Tick Bombs") { tickBombController() }
            time("Tick Point Orbs") { tickPointOrbs() }
            time("Tick Score System") { tickScoreSystem() }
        } else {
            time("Tick Upgrade Controller") { tickUpgradeController() }
        }

        time("Update tile physics") { updateTilePhysics() }
        if (GAME_IS_RUNNING) {
            time("Do physics step") { doPhysicsStep() }
            time("Tick physics wrapper") { tickPhysicsWrapper() }
        }

        worldSpaceRenderer.begin()
        time("Render background") { renderBackground(worldSpaceRenderer) }
        time("Render player inputs") { playerInputs.render(worldSpaceRenderer) }
        worldSpaceRenderer.end()

        time("Render level") { renderLevel() }

        worldSpaceRenderer.begin()
        time("Render physics objects") { renderPhysicsObjects(worldSpaceRenderer) }
        time("Render laser") { renderLaser(worldSpaceRenderer) }
        time("Render particles") { renderParticles(worldSpaceRenderer) }
        time("Render world space UI") { renderUIWorldSpace(worldSpaceRenderer) }
        worldSpaceRenderer.end()


        if (DO_PHYSICS_DEBUG_RENDER) {
            debugRenderWorld()
        }

        time("Render UI") {
            uiRenderer.begin()
            renderUIScreenSpace(uiRenderer)
            renderUpgradeMenuScreenSpace(uiRenderer)
            uiRenderer.end()
        }

        timerEnd()
    }

    override fun dispose() {
        disposeSounds()
    }

    override fun resume() {
        miniAudio.startEngine();
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
        miniAudio.stopEngine();
    }
}