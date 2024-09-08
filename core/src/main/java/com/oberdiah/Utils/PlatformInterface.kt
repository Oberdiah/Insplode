package com.oberdiah.Utils

import games.rednblack.miniaudio.MiniAudio

interface PlatformInterface {
    fun print(string: String)
    fun injectAssetManager(miniAudio: MiniAudio)
}