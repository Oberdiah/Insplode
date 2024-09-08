package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import kotlin.random.Random

private lateinit var ALL_SOUNDS: Map<String, Sound>

fun loadSounds() {
    val soundFiles =
        listFolder("Sounds/").map { it.nameWithoutExtension() to Gdx.audio.newSound(it) }

    println("Loaded sounds: ${soundFiles.map { it.first }}")

    ALL_SOUNDS = soundFiles.toMap()
}

fun playSound(soundName: String, pitch: Double = 1.0, volume: Double = 1.0) {
    val sound = ALL_SOUNDS[soundName]

    if (sound == null) {
        println("Sound $soundName not found")
        return
    }

    val soundId = sound.play()
    sound.setPitch(soundId, pitch.f)
    sound.setVolume(soundId, volume.f)
}

fun playExplosionSound(force: Double) {
    var pitch = max(0.7 - force * 0.1, 0.1)

    /// Vary the pitch a bit
    pitch += Random.nextDouble(-0.05, 0.05)

    playSound("trim_orange hit hard ${Random.nextInt(1, 10)}", pitch)
}

fun playBombBumpSound() {
    playSound("trim_orange hit soft ${Random.nextInt(1, 10)}", 1.0, 0.5)
}