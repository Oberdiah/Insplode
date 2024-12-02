package com.oberdiah

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.*
import com.oberdiah.level.Jewel
import com.oberdiah.level.Level
import com.oberdiah.level.RenderLevel
import com.oberdiah.level.renderLevelController
import com.oberdiah.level.resetLevelCollapse
import com.oberdiah.level.resetLevelController
import com.oberdiah.level.tickCollapse
import com.oberdiah.level.tickLevelController
import com.oberdiah.player.PlayerInputs
import com.oberdiah.player.player
import com.oberdiah.player.spawnPlayerAtUpgrades
import com.oberdiah.ui.Banner
import com.oberdiah.ui.PauseButton
import com.oberdiah.ui.renderDiegeticMenuWorldSpace
import com.oberdiah.utils.*
import com.oberdiah.ui.renderUIScreenSpace
import com.oberdiah.ui.tickDiegeticMenu
import com.oberdiah.upgrades.UpgradeController


lateinit var worldSpaceRenderer: Renderer
lateinit var uiRenderer: Renderer
lateinit var platformInterface: PlatformInterface

fun endGame() {
    GAME_STATE = GameState.DiegeticMenu
//    Perlin.randomize()
    resetCamera()

    resetPhysicsObjects()
    updateTilePhysics()

    PointOrbs.reset()
    resetLevelController()
    resetLevelCollapse()
    setCameraGlobalsThisFrame()
    updateCamera()
    Level.updateStorage()
}

fun startGame(upgradesSpawnPoint: Boolean) {
    spawnPlayerAtUpgrades = upgradesSpawnPoint

    MusicCoordinator.stopPlayingMusic()
    GAME_STATE = GameState.InGame

    // We might have upgraded or changed settings, so we want to re-reset a bunch of stuff.
    resetPhysicsObjects()
    resetLevelController()
    // We reset level at the start of the game because we need to be able to apply any upgrades
    // the player has bought.
    Level.reset()

    ScoreSystem.registerGameStart()
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
        RenderLevel.init()
        UpgradeController.init()
        ScoreSystem.init()
        PauseButton.init()

        worldSpaceRenderer = Renderer("World Space Renderer", camera)
        uiRenderer = Renderer("UI Renderer", screenCamera)
        initWorld()
        Level.initTiles()

        loadFiles()
        loadFonts()
        loadSounds()
        Sprites.init()

        leftWall = createWall(Rect(Point(), Size(1, WALL_HEIGHT)))
        rightWall = createWall(Rect(Point(), Size(1, WALL_HEIGHT)))
        ceiling = createWall(Rect(Point(), Size(SCREEN_WIDTH_IN_UNITS, 1)))
        floor = createWall(Rect(Point(), Size(SCREEN_WIDTH_IN_UNITS, 1)))

        initCamera()
        endGame()

        MusicCoordinator.startPlayingMusic()
    }

    override fun render() {
        val clearColor = colorScheme.backgroundA

        Gdx.gl.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a)
        // When the software delay happens - let's do calculations after that.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        timerStart()

        time("Set globals this frame") { setGlobalsThisFrame() }
        time("Tick game time") { GameTime.tick() }
        time("Collect input events") { calculateInputGlobals() }

        // All the above stuff has to happen *really* early.

        time("Save State") { saveState() }
        time("Tick Sounds") { tickSounds() }
        time("Tick Music Coordinator") { MusicCoordinator.tick() }

        time("Camera") { updateCamera() }
        setCameraGlobalsThisFrame()

        time("Update level storage") { Level.updateStorage() }
        time("Update tile changes") { updateTileChanges() }
        if (GAME_STATE != GameState.PausedPopup) {
            time("Tick Particles") { tickParticles() }
        }

        if (GAME_IS_RUNNING) {
            // 'Coz Box2d stupid, these are the centres of the positions.
            leftWall.setTransform(Point(-0.5, Level.LOWEST_TILE_Y_UNITS + WALL_HEIGHT / 2), 0f)
            rightWall.setTransform(Point(10.5, Level.LOWEST_TILE_Y_UNITS + WALL_HEIGHT / 2), 0f)

            if (CAMERA_FOLLOWING == CameraFollowing.Player) {
                ceiling.setTransform(
                    Point(
                        SCREEN_WIDTH_IN_UNITS / 2.0,
                        Level.LOWEST_TILE_Y_UNITS + WALL_HEIGHT
                    ), 0f
                )
                floor.setTransform(
                    Point(SCREEN_WIDTH_IN_UNITS / 2.0, Level.LOWEST_TILE_Y_UNITS),
                    0f
                )
            } else {
                ceiling.setTransform(Point(-5, 0), 0f)
                floor.setTransform(Point(-5, 0), 0f)
            }


            if (GAME_STATE == GameState.InGame && !player.state.isDead) {
                time("Tick level controller") { tickLevelController() }
            }
            time("Tick Collapse") { tickCollapse() }
            time("Tick Physics Objects") { tickPhysicsObjects() }
            time("Tick Point Orbs") { PointOrbs.tick() }
            time("Tick Bombs") { tickBombController() }
            time("Tick Jewel") { Jewel.tick() }
        } else {
            time("Tick Diegetic Menu") { tickDiegeticMenu() }
            time("Tick Upgrade Controller") { UpgradeController.tick() }
        }
        time("Tick pause button") { PauseButton.tick() }
        time("Tick Score System") { ScoreSystem.tick() }

        time("Update tile physics") { updateTilePhysics() }
        if (GAME_IS_RUNNING) {
            time("Do physics step") { doPhysicsStep() }
            time("Tick physics wrapper") { tickPhysicsWrapper() }
        }

        worldSpaceRenderer.begin()
        time("Render background") { RenderLevel.renderBackground(worldSpaceRenderer) }
        time("Render clouds") { RenderLevel.renderForeground(worldSpaceRenderer) }
        time("Render player inputs") { PlayerInputs.render(worldSpaceRenderer) }
        time("Render world space UI") { renderDiegeticMenuWorldSpace(worldSpaceRenderer) }
        worldSpaceRenderer.end()

        time("Render level") { RenderLevel.render() }

        worldSpaceRenderer.begin()
        time("Render level sprites") { Jewel.renderJewelInLevel(worldSpaceRenderer) }
        time("Render physics objects") { renderPhysicsObjects(worldSpaceRenderer) }
        time("Render level controller") { renderLevelController(worldSpaceRenderer) }
        time("Render particles") { renderParticles(worldSpaceRenderer) }
        worldSpaceRenderer.end()


        if (DO_PHYSICS_DEBUG_RENDER) {
            debugRenderWorld()
        }

        uiRenderer.begin()
        time("Score System Text") { ScoreSystem.renderDiegeticText(uiRenderer) }
        time("Render banner") { Banner.renderBanners(uiRenderer) }
        time("Render UI") { renderUIScreenSpace(uiRenderer) }
        uiRenderer.end()


        timerEnd()
    }

    override fun dispose() {
        disposeSounds()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
        pauseSounds()
    }

    override fun resume() {
        resumeSounds()
    }
}