package com.oberdiah.utils

import games.rednblack.miniaudio.MiniAudio

interface PlatformInterface {
    fun print(string: String)
    fun injectAssetManager(miniAudio: MiniAudio)
}