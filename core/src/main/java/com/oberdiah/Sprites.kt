package com.oberdiah

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite

object Sprites {
    private val sprites = mutableMapOf<String, Sprite>()

    fun init() {
        listFolder("Icons/").forEach {
            val path = it.path()
            val sprite = Sprite(Texture(path))
            sprites[it.nameWithoutExtension()] = sprite
        }
    }

    fun getSprite(name: String): Sprite {
        return sprites[name] ?: sprites["Not Found"]!!
    }
}