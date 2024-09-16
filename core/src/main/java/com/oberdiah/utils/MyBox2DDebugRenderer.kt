package com.oberdiah.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World

class MyBox2DDebugRenderer : Box2DDebugRenderer() {
    override fun render(world: World, camera: Matrix4) {
        Gdx.gl.glLineWidth(2f)
        super.render(world, camera)
    }
}