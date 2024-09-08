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

fun playBombBumpSound(velocity: Double, mass: Double, hitObject: Any?) {
    if (velocity < 2.0) return
    // Velocity generally ranges from about 5 on a rebound to 25 when coming in from the sky.
    val volume = saturate((velocity - 3.0) / 20) / 2
    var pitch = Random.nextDouble(0.2, 0.3)
    // Mass generally ranges from 1 to 5
    // Higher mass means lower pitch
    pitch += (1 - mass / 5) * 0.3

    if (hitObject is Bomb) {
        playSound("trim_glass clink ${Random.nextInt(1, 6)}", pitch, volume)
    } else if (hitObject is Player) {
        // Nothing for now
    } else {
        // We've hit ground
        playSound("trim_orange hit soft ${Random.nextInt(1, 10)}", pitch, volume)
        playSound("trim_glass clink ${Random.nextInt(1, 6)}", pitch, volume * 0.1)
    }
}