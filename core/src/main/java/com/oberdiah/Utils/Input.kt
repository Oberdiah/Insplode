package com.oberdiah.Utils

import com.badlogic.gdx.Gdx

fun isKeyPressed(key: Int): Boolean {
    return Gdx.input.isKeyPressed(key)
}