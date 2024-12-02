package com.oberdiah

import com.oberdiah.level.RUN_TIME_ELAPSED
import com.oberdiah.player.Player
import com.oberdiah.sounds.MiniAudioCore
import com.oberdiah.sounds.SoundCore
import kotlin.math.pow
import kotlin.random.Random

private lateinit var soundEngine: SoundCore

fun loadSounds() {
    soundEngine = MiniAudioCore()
    soundEngine.initialize()
}

fun disposeSounds() {
    soundEngine.dispose()
}

fun pauseSounds() {
    soundEngine.pause()
}

fun resumeSounds() {
    soundEngine.resume()
}

/** Sorted from lowest to highest volume */
val scheduledSounds = mutableListOf<SoundData>()
fun tickSounds() {
    scheduledSounds.forEach {
        if (it.scheduledAt!! < RUN_TIME_ELAPSED) {
            playSound(it.name, it.pitch, it.volume, withReverb = it.withReverb)
        }
    }
    scheduledSounds.removeAll { it.scheduledAt!! < RUN_TIME_ELAPSED }
}

data class SoundData(
    val name: String,
    val pitch: Double,
    val volume: Double,
    var withReverb: Boolean = false,
    val scheduledAt: Double?
)

fun playSound(
    soundName: String,
    pitch: Double = 1.0,
    volume: Double = 1.0,
    withReverb: Boolean = false,
    delay: Double? = null,
) {
    if (volume < 0.005) return
    if (statefulPlaySoundSetting.value == false) return

    val soundData =
        SoundData(soundName, pitch, volume, withReverb, RUN_TIME_ELAPSED + (delay ?: 0.0));

    if (delay != null) {
        scheduledSounds.add(soundData)
        return
    }

    soundEngine.playSound(soundData)
}

fun playExplosionSound(force: Double) {
    // Force generally ranges from 0 to 2 for the very large explosions

    var pitch = max(0.7 - force * 0.3, 0.1)

    /// Vary the pitch a bit
    pitch *= Random.nextDouble(0.9, 1.1)

    if (pitch > 0.4) {
        playSound(
            "Orange hit hard ${Random.nextInt(1, 10)}",
            pitch,
            volume = 0.5
        )
        playSound(
            "Orange hit soft ${Random.nextInt(1, 5)}",
            pitch * 0.5,
            volume = 0.5
        )
    } else {
        val withReverb = force > 1.8

        playSound(
            "Bat hit 4",
            pitch,
            withReverb = withReverb,
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
    pitch *= max(saturate(1 - mass / 10) + 0.1, 0.7)

    if (hitObject is Bomb) {
        playSound(
            "Ding ${Random.nextInt(1, 6)}",
            pitch,
            volume * 0.5
        )
        playSound(
            "Can hit ${Random.nextInt(1, 8)}",
            pitch,
            volume
        )
    } else if (hitObject is Player) {
        // Nothing for now
    } else {
        // We've hit ground
        playSound(
            "Can hit ${Random.nextInt(1, 8)}",
            pitch * 1.5,
            volume
        )
    }
}

fun playRockCrumbleSound() {
    if (Random.nextDouble() > 0.25) return
    playSound(
        "Crunch ${Random.nextInt(1, 6)}",
        Random.nextDouble(0.6, 0.8),
        Random.nextDouble(0.05, 0.2)
    )
}

fun playParticleHitSound(velocity: Double, size: Double) {
    val impact = velocity * size * 25.0
    val volume = saturate((impact - 2.0) / 10).pow(1.5) * 0.25
    val pitch = Random.nextDouble(0.4, 0.7)

    playSound(
        "Tap ${Random.nextInt(1, 6)}",
        pitch,
        volume,
    )
}

fun playPlayerLandSound() {
    playSound(
        "Tap ${Random.nextInt(1, 6)}",
        Random.nextDouble(0.9, 1.1),
        Random.nextDouble(0.75, 0.9)
    )
}

fun playChordsSound(chord: Int) {
    playSound(
        "Chords $chord",
        Random.nextDouble(0.995, 1.005),
        Random.nextDouble(0.75, 0.85)
    )
}

const val PICKUP_CHAIN_LENGTH = 40

fun playPickupSound(numberInChain: Int) {
    playPickupSoundInternal((numberInChain + 0 * PICKUP_CHAIN_LENGTH / 3) % PICKUP_CHAIN_LENGTH)
    playPickupSoundInternal((numberInChain + 1 * PICKUP_CHAIN_LENGTH / 3) % PICKUP_CHAIN_LENGTH)
    playPickupSoundInternal((numberInChain + 2 * PICKUP_CHAIN_LENGTH / 3) % PICKUP_CHAIN_LENGTH)
}

private fun playPickupSoundInternal(loopId: Int) {
    val pitch = 1.0 + loopId * 0.05
    val volume = 1.0 - abs(PICKUP_CHAIN_LENGTH / 2 - loopId).d / (PICKUP_CHAIN_LENGTH / 2)

    playSound(
        "Ding ${Random.nextInt(1, 6)}",
        pitch,
        volume.pow(2) * 0.25,
        withReverb = true,
    )
}

fun playMultiplierSound(multiplier: Int) {
    playSound(
        "Power up ${clamp(multiplier, 1, 10).i}",
        1.0,
        0.85
    )
}

fun playMultiplierLossSound() {
    playSound(
        "Power down",
        1.0,
        0.5
    )
}

var lastNotePlayed = mutableMapOf('C' to 0, 'G' to 0)
fun playChordNote(note: Char) {
    var noteToPlay = Random.nextInt(1, 5)
    while (noteToPlay == lastNotePlayed[note]) {
        noteToPlay = Random.nextInt(1, 5)
    }
    lastNotePlayed[note] = noteToPlay
    playSound(
        "Twang $note $noteToPlay",
        Random.nextDouble(0.995, 1.005),
        Random.nextDouble(0.75, 0.85)
    )
}