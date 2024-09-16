package com.oberdiah.utils

import com.badlogic.gdx.graphics.Color
import com.oberdiah.colorFrom
import com.oberdiah.forceFullScreenRefresh
import com.oberdiah.withAlpha

object Colors {
    val transparent = Color.BLACK.withAlpha(0.0)
}

// Invisible
private val schemes = mutableListOf(
    Grassy(),
    Grassy2(),
    Oceany(),
    Deserty(),
    Classic(),
    Castle(),
    Canyon(),
    Princess()
)
private var schemeNum = 0

fun nextColorScheme() {
    schemeNum = (schemeNum + 1) % schemes.size
    colorScheme = schemes[schemeNum]
    forceFullScreenRefresh()
}

var colorScheme: ColorScheme = Grassy()

class Grassy : ColorScheme("Forest") {
    init {
        player = colorFrom(0xbc4749)
        playerSlamming = colorFrom(0xd27c2d)
        bombPrimary = colorFrom(0xbc4749)
        backgroundA = colorFrom(0xf2e8cf)
        backgroundB = colorFrom(0xf2e8cf).mul(1.03f)
        stone = colorFrom(0x303930)
        dirt = colorFrom(0x54793E)
        grass = colorFrom(0x103E1F)
    }
}

class Grassy2 : ColorScheme("Grass Lands") {
    init {
        player = colorFrom(0xbc4749)
        playerSlamming = colorFrom(0xd27c2d)
        bombPrimary = colorFrom(0xbc4749)
        backgroundA = colorFrom(0xf2e8cf)
        backgroundB = colorFrom(0xf2e8cf).mul(1.03f)
        stone = colorFrom(0x386641)
        dirt = colorFrom(0x6a994e)
        grass = colorFrom(0xa7c957)
    }
}

class Oceany : ColorScheme("Ocean") {
    init {
        player = colorFrom(0x77b2f2)
        playerSlamming = colorFrom(0xd27c2d)
        bombPrimary = colorFrom(0xe63946)
        backgroundA = colorFrom(0xf1faee)
        backgroundB = colorFrom(0xf1faee).mul(1.03f)
        stone = colorFrom(0x1d3557)
        dirt = colorFrom(0x457b9d)
        grass = colorFrom(0xa8dadc)
    }
}

class Deserty : ColorScheme("Desert") {
    init {
        player = colorFrom(0xFFFCF2)
        playerSlamming = colorFrom(0xd27c2d)
        bombPrimary = Color.RED.cpy().mul(0.8f)
        backgroundA = colorFrom(0x90AACB)
        backgroundB = colorFrom(0x90AACB).mul(1.03f)
        stone = colorFrom(0xFEF1E6)
        dirt = colorFrom(0xF9D5A7)
        grass = colorFrom(0xFFB085)
    }
}

class Classic : ColorScheme("Classic") {
    init {
        player = colorFrom(0xFFFCF2)
        playerSlamming = colorFrom(0xd27c2d)
        bombPrimary = Color.RED.cpy().mul(0.8f)
        backgroundA = colorFrom(0x9DCFEA)
        backgroundB = colorFrom(0x9DCFEA).mul(1.03f)
        stone = colorFrom(0x404040)
        dirt = colorFrom(0xA07759)
        grass = colorFrom(0x439E57)
    }
}

class Castle : ColorScheme("Castle") {
    init {
        player = colorFrom(0x9c7878)
        playerSlamming = colorFrom(0xd27c2d)
        bombPrimary = colorFrom(0x000000).mul(0.8f)
        backgroundA = colorFrom(0xe0ac4a)
        backgroundB = colorFrom(0xe0ac4a).mul(1.03f)
        stone = colorFrom(0x2b2b2b)
        dirt = colorFrom(0x666666)
        grass = colorFrom(0x910f11)
    }
}

class Canyon : ColorScheme("Canyon") {
    init {
        player = colorFrom(0xffe712)
        playerSlamming = colorFrom(0xd27c2d)
        bombPrimary = colorFrom(0x000000).mul(0.8f)
        backgroundA = colorFrom(0x4e9fc2)
        backgroundB = colorFrom(0x4e9fc2).mul(1.03f)
        stone = colorFrom(0xad9311)
        dirt = colorFrom(0x7a5828)
        grass = colorFrom(0xf7e672)
    }
}

class Princess : ColorScheme("Princess") {
    init {
        player = colorFrom(0xf587ed)
        playerSlamming = colorFrom(0xd27c2d)
        bombPrimary = colorFrom(0x000000).mul(0.8f)
        backgroundA = colorFrom(0x9DCFEA)
        backgroundB = colorFrom(0x9DCFEA).mul(1.03f)
        stone = colorFrom(0xb757d4)
        dirt = colorFrom(0xf0b1ec)
        grass = colorFrom(0x57d4b9)
    }
}


open class ColorScheme(val name: String) {
    var playerNoJump: Color = Color.GRAY
    var overlay: Color = Color.WHITE.withAlpha(0.3)
    var textColor: Color = colorFrom(0x242423)
    lateinit var player: Color
    lateinit var playerSlamming: Color
    lateinit var bombPrimary: Color
    lateinit var backgroundA: Color
    lateinit var backgroundB: Color
    lateinit var stone: Color
    lateinit var dirt: Color
    lateinit var grass: Color
}

// https://coolors.co/6a994e-54793e-003e1f-f2e8cf-bc4648-404840-242423

enum class TileType(var color: () -> Color) {
    Stone({ colorScheme.stone }),
    Dirt({ colorScheme.dirt }),
    Grass({ colorScheme.grass }),
    Air({ Colors.transparent })
}