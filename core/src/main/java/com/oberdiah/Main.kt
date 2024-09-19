package com.oberdiah

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.*
import com.oberdiah.utils.*
import com.oberdiah.ui.renderUI


lateinit var worldSpaceRenderer: Renderer
lateinit var uiRenderer: Renderer
lateinit var platformInterface: PlatformInterface

fun restartGame() {
    resetParticles()
    resetPhysicsObjects()
    resetLevel()
    resetLevelController()
    updateTilePhysics()
    resetCamera()
    resetScoreSystem()
}

lateinit var leftWall: PhysBody
lateinit var rightWall: PhysBody

class Main(print: PlatformInterface) : InputAdapter(), ApplicationListener {
    init {
        platformInterface = print
    }

    override fun create() {
        resetCamera()
        setGlobalsThisFrame()
        initMarchingSquares()
        initLevelRender()

        worldSpaceRenderer = Renderer("World Space Renderer")
        uiRenderer = Renderer("UI Renderer")
        initWorld()
        initTiles()

        loadFiles()
        loadFonts()
        loadSounds()

        leftWall = createWall(Rect(Point(0, 0), Size(1, SCREEN_HEIGHT_IN_UNITS * 4)))
        rightWall = createWall(Rect(Point(10, 0), Size(1, SCREEN_HEIGHT_IN_UNITS * 4)))

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

        // DEBUG_STRING = "$LOWEST_TILE_Y_STORED"

        if (!PAUSED) {
            time("Camera") { updateCamera() }
            setCameraGlobalsThisFrame()
            time("Update level storage") { updateLevelStorage() }
            time("Update tile changes") { updateTileChanges() }

            // 'Coz Box2d stupid, these are the centres of the positions.
            leftWall.setTransform(Point(-0.5, CAMERA_POS_Y + SCREEN_HEIGHT_IN_UNITS / 2), 0f)
            rightWall.setTransform(Point(10.5, CAMERA_POS_Y + SCREEN_HEIGHT_IN_UNITS / 2), 0f)

            time("Tick level controller") { tickLevelController() }
            time("Tick Collapse") { tickCollapse() }
            time("Tick Particles") { tickParticles() }
            time("Tick Physics Objects") { tickPhysicsObjects() }
            time("Tick Point Orbs") { tickPointOrbs() }
            time("Tick Score System") { tickScoreSystem() }

            // Should happen just before the world step
            time("Update tile physics") { updateTilePhysics() }
            if (!DEBUG_MOVEMENT_MODE) {
                time("Do physics step") { doPhysicsStep() }
            }
            time("Tick physics wrapper") { tickPhysicsWrapper() }
        }

        worldSpaceRenderer.begin()
        time("Render background") { renderBackground(worldSpaceRenderer) }
        time("Render physics objects") { renderPhysicsObjects(worldSpaceRenderer) }
        time("Render particles") { renderParticles(worldSpaceRenderer) }
//        time("Render level") { renderLevelOld(worldSpaceRenderer) }
        worldSpaceRenderer.end()

        time("Render level") { renderLevel() }

        if (DO_PHYSICS_DEBUG_RENDER) {
            debugRenderWorld()
        }

        time("Render UI") {
            uiRenderer.begin()
            renderUI(uiRenderer)
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