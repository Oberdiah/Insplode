package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

lateinit var fontSmall: BitmapFont
lateinit var fontSmallish: BitmapFont
lateinit var fontMedium: BitmapFont
lateinit var fontLarge: BitmapFont

fun loadFonts() {
    val g = FreeTypeFontGenerator(Gdx.files.internal("Lato-Medium.ttf"))

    fontSmall = createFont(g, HEIGHT/50)
    fontSmallish = createFont(g, HEIGHT/40)
    fontMedium = createFont(g, HEIGHT/30)
    fontLarge = createFont(g, HEIGHT/20)

    g.dispose()
}

fun createFont(g: FreeTypeFontGenerator, size: Number): BitmapFont {
    val parameter = FreeTypeFontParameter()
    parameter.size = size.i
    parameter.minFilter = Texture.TextureFilter.Linear
    parameter.magFilter = Texture.TextureFilter.Linear
    return g.generateFont(parameter)
}