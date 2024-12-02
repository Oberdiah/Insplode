package com.oberdiah.sounds

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.oberdiah.SoundData
import com.oberdiah.listFolder

class DefaultAudioCore: SoundCore {
    private var sounds: MutableMap<String, Sound> = mutableMapOf()

    override fun initialize() {
        listFolder("Sounds/").forEach {
            sounds[it.nameWithoutExtension()] = Gdx.audio.newSound(it)
        }
    }

    override fun playSound(soundData: SoundData) {
        val sound = sounds[soundData.name]
        if (sound == null) {
            println("Sound not found: ${soundData.name}")
            return
        }

        var soundId = sound.play(soundData.volume.toFloat())
        sound.setPitch(soundId, soundData.pitch.toFloat())
    }

    override fun dispose() {
    }

    override fun tick() {
    }

    override fun pause() {
    }

    override fun resume() {
    }
}