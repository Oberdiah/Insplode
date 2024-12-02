package com.oberdiah.sounds

import com.oberdiah.SoundData

interface SoundCore {
    fun initialize()
    fun dispose()
    fun tick()
    fun pause()
    fun resume()
    fun playSound(soundData: SoundData)
}