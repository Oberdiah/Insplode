package com.oberdiah

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import kotlin.math.pow
import kotlin.random.Random

private lateinit var ALL_SOUNDS: Map<String, Sound>

// https://sonniss.com/gameaudiogdc

fun loadSounds() {
    val soundFiles =
        listFolder("Sounds/").map { it.nameWithoutExtension() to Gdx.audio.newSound(it) }

    println("Loaded sounds: ${soundFiles.map { it.first }}")

    ALL_SOUNDS = soundFiles.toMap()
}

fun tickSounds() {
    SOUNDS_PLAYING = SOUNDS_PLAYING.filter { (reason, time) ->
        val soundDuration = reason.avgDuration
        RUN_TIME_ELAPSED - time < soundDuration
    }
    NUM_OF_EACH_REASON = SOUNDS_PLAYING.groupBy { it.first }.mapValues { it.value.size }
}

enum class ReasonSoundCreated(val avgDuration: Double, val numAcceptable: Int) {
    Explosion(0.6, 5),
    Bump(0.3, 6),
    Particle(0.3, 10)
}

var SOUNDS_PLAYING: List<Pair<ReasonSoundCreated, Double>> = mutableListOf()
var NUM_OF_EACH_REASON = mapOf<ReasonSoundCreated, Int>()

fun playSound(
    soundName: String,
    reason: ReasonSoundCreated,
    pitch: Double = 1.0,
    volume: Double = 1.0
) {
    val sound = ALL_SOUNDS[soundName]

    if (sound == null) {
        println("Sound $soundName not found")
        return
    }

    if ((NUM_OF_EACH_REASON[reason] ?: 0) > reason.numAcceptable) {
        return
    }

    if (volume < 0.005) return
    sound.play(volume.f, 0.75f + pitch.f * 0.25f, 0f)
    SOUNDS_PLAYING += reason to RUN_TIME_ELAPSED
}

fun playExplosionSound(force: Double) {
    // Force generally ranges from 0 to 2 for the very large explosions

    var pitch = 0.7 - force * 0.3

    /// Vary the pitch a bit
    pitch += Random.nextDouble(-0.05, 0.05)

    if (pitch > 0.4) {
        playSound(
            "trim_orange hit hard ${Random.nextInt(1, 10)}",
            ReasonSoundCreated.Explosion,
            pitch,
            volume = 0.5
        )
        playSound(
            "trim_orange hit soft ${Random.nextInt(1, 10)}",
            ReasonSoundCreated.Explosion,
            pitch * 0.5,
            volume = 0.5
        )
    } else {
//        playSound("trim_metal hits metal 7", ReasonSoundCreated.Explosion, 0.05, volume = 0.5)
        playSound(
            "trim_orange hit hard ${Random.nextInt(1, 10)}",
            ReasonSoundCreated.Explosion,
            pitch,
            volume = 0.5
        )
        playSound(
            "trim_trim_bat hit 4",
            ReasonSoundCreated.Explosion,
            max(pitch, 0.1),
        )
    }
}

fun playBombBumpSound(velocity: Double, mass: Double, hitObject: Any?) {
    if (velocity < 2.0) return
    // Velocity generally ranges from about 5 on a rebound to 25 when coming in from the sky.
    val volume = saturate((velocity - 3.0) / 20) * 0.25
    var pitch = Random.nextDouble(0.2, 0.3)
    // Mass generally ranges from 1 to 5
    // Higher mass means lower pitch
    pitch *= saturate(1 - mass / 10) + 0.1

    if (hitObject is Bomb) {
        playSound(
            "trim_glass clink ${Random.nextInt(1, 6)}",
            ReasonSoundCreated.Bump,
            pitch,
            volume
        )
        playSound(
            "trim_metal hit with hammer ${Random.nextInt(2, 7)}",
            ReasonSoundCreated.Bump,
            pitch,
            volume
        )
    } else if (hitObject is Player) {
        // Nothing for now
    } else {
        // We've hit ground
        playSound(
            "trim_trim_bat hit ${Random.nextInt(3, 10)}",
            ReasonSoundCreated.Bump,
            pitch,
            volume
        )
        playSound(
            "trim_glass clink ${Random.nextInt(1, 6)}",
            ReasonSoundCreated.Bump,
            pitch,
            volume * 0.1
        )
    }
}

fun playParticleSound(velocity: Double, size: Double) {
    // Randomly drop some sounds
    if (Random.nextDouble() > 0.5) return

    val impact = velocity * size * 25.0
    val volume = saturate((impact - 2.0) / 10).pow(1.5) * 0.25
    val pitch = Random.nextDouble(1.0, 2.6)
    playSound(
        "trim_glass clink ${Random.nextInt(2, 6)}",
        ReasonSoundCreated.Particle,
        pitch,
        volume
    )
}