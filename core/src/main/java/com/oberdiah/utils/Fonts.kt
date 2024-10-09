package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

lateinit var fontTiny: BitmapFont
lateinit var fontSmall: BitmapFont
lateinit var fontSmallish: BitmapFont
lateinit var fontMedium: BitmapFont
lateinit var fontLarge: BitmapFont
private var fontCache = mutableMapOf<String, BitmapFontCache>()

fun loadFonts() {
    val g = FreeTypeFontGenerator(Gdx.files.internal("Lato-Medium.ttf"))

    fontTiny = createFont(g, HEIGHT / 60)
    fontSmall = createFont(g, HEIGHT / 50)
    fontSmallish = createFont(g, HEIGHT / 40)
    fontMedium = createFont(g, HEIGHT / 30)
    fontLarge = createFont(g, HEIGHT / 20)

    g.dispose()
}

fun getFontCache(font: BitmapFont, align: Int, text: String): BitmapFontCache {
    val cacheKey = "$font || $align || $text"

    if (fontCache[cacheKey] == null) {
        fontCache[cacheKey] = BitmapFontCache(font)
        fontCache[cacheKey]!!.addText(text, 0f, 0f, 0f, align, false)
    }

    // Warn if the cache is too big
    if (fontCache.size > 200) {
        println("Warning: font cache is getting large!")
        println("Strings cached (font || align || key):")
        fontCache.keys.forEach {
            println(" - `$it`")
        }
    }

    return fontCache[cacheKey]!!
}

fun createFont(g: FreeTypeFontGenerator, size: Number): BitmapFont {
    val parameter = FreeTypeFontParameter()
    parameter.size = size.i
    parameter.minFilter = Texture.TextureFilter.Linear
    parameter.magFilter = Texture.TextureFilter.Linear
    val font = g.generateFont(parameter)

    return font
}