package com.oberdiah

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import games.rednblack.miniaudio.MASound
import games.rednblack.miniaudio.MiniAudio
import games.rednblack.miniaudio.effect.MADelayNode
import games.rednblack.miniaudio.filter.MALowPassFilter
import games.rednblack.miniaudio.mix.MASplitter
import kotlin.math.pow
import kotlin.random.Random


private var SOUND_POOL: Map<String, List<MASound>> = mutableMapOf()
lateinit var miniAudio: MiniAudio
lateinit var caveSplitter: MASplitter

// https://sonniss.com/gameaudiogdc

fun loadSounds() {
    //Create only one MiniAudio object!
    miniAudio = MiniAudio()
    platformInterface.injectAssetManager(miniAudio)
    caveSplitter = MASplitter(miniAudio)
    val lowPassFilter = MALowPassFilter(miniAudio, 550.0, 8)
    val delayNode = MADelayNode(miniAudio, 0.15f, 0.3f)

    miniAudio.attachToEngineOutput(lowPassFilter, 0)
    miniAudio.attachToEngineOutput(delayNode, 0)

    lowPassFilter.attachToThisNode(caveSplitter, 0)
    delayNode.attachToThisNode(caveSplitter, 1)

    val allPaths = listFolder("Sounds/").map {
        var path = it.path()

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            path = "C:\\Users\\richa\\Documents\\MyCodeProjects\\WeeklyGame2\\assets\\$path"
        }

        Pair(path, it.nameWithoutExtension())
    }

    val numSoundsInPoolPerAsset = 30

    allPaths.forEach {
        val path = it.first
        val sounds = (0 until numSoundsInPoolPerAsset).map {
            miniAudio.createSound(path)
        }
        SOUND_POOL += it.second to sounds
    }
}

val scheduledSounds = mutableListOf<ASound>()
fun tickSounds() {
    scheduledSounds.forEach {
        if (it.scheduledAt!! < RUN_TIME_ELAPSED) {
            playSound(it.name, it.pitch, it.volume, it.splitter)
        }
    }
    scheduledSounds.removeAll { it.scheduledAt!! < RUN_TIME_ELAPSED }
}

fun disposeSounds() {
    // I trust Windows to just do the cleanup for us - I'd like to be more responsible
    // on Android
    if (Gdx.app.type != Application.ApplicationType.Desktop) {
        SOUND_POOL.forEach { (_, sounds) ->
            sounds.forEach { it.dispose() }
        }

        miniAudio.dispose()
    }
}

data class ASound(
    val name: String,
    val pitch: Double,
    val volume: Double,
    val splitter: MASplitter?,
    val scheduledAt: Double?
)

fun playSound(
    soundName: String,
    pitch: Double = 1.0,
    volume: Double = 1.0,
    splitter: MASplitter? = null,
    delay: Double? = null
) {
    if (volume < 0.005) return

    // Get the first sound that isn't playing (sound.isPlaying())
    val soundPool = SOUND_POOL[soundName]
    if (soundPool == null) {
        println("Sound not found: $soundName")
        return
    }

    val sound = soundPool.firstOrNull { !it.isPlaying }

    if (sound == null) {
        return
    }

    if (delay != null) {
        scheduledSounds.add(ASound(soundName, pitch, volume, splitter, RUN_TIME_ELAPSED + delay))
        return
    }

    sound.setVolume(volume.f)
    sound.setPitch(pitch.f)
    // Random pan
    sound.setPan(Random.nextDouble(-0.5, 0.5).f)
    sound.fadeIn(0.1f)

    if (splitter != null) {
        splitter.attachToThisNode(sound, 0)
    } else {
        miniAudio.attachToEngineOutput(sound, 0)
    }

    sound.play()
}

fun playExplosionSound(force: Double) {
    // Force generally ranges from 0 to 2 for the very large explosions

    var pitch = max(0.7 - force * 0.3, 0.1)

    /// Vary the pitch a bit
    pitch *= Random.nextDouble(0.9, 1.1)

    if (pitch > 0.4) {
        playSound(
            "trim_orange hit hard ${Random.nextInt(1, 10)}",
            pitch,
            volume = 0.5
        )
        playSound(
            "trim_orange hit soft ${Random.nextInt(1, 10)}",
            pitch * 0.5,
            volume = 0.5
        )
    } else {
        val splitter = if (force > 1.8) {
            caveSplitter
        } else {
            null
        }

        playSound(
            "trim_orange hit hard ${Random.nextInt(1, 10)}",
            pitch,
            volume = 0.5,
            splitter = splitter,
        )
        playSound(
            "trim_trim_bat hit 4",
            pitch,
            splitter = splitter,
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
            pitch,
            volume
        )
        playSound(
            "trim_metal hit with hammer ${Random.nextInt(2, 7)}",
            pitch,
            volume
        )
    } else if (hitObject is Player) {
        // Nothing for now
    } else {
        // We've hit ground
        playSound(
            "trim_trim_bat hit ${Random.nextInt(3, 10)}",
            pitch,
            volume
        )
        playSound(
            "trim_glass clink ${Random.nextInt(1, 6)}",
            pitch,
            volume * 0.1
        )
    }
}

fun playRockCrumbleSound() {
    if (Random.nextDouble() > 0.25) return
    playSound(
        "trim_rocks handle ${Random.nextInt(13, 17)} low",
        Random.nextDouble(1.1, 1.3),
        Random.nextDouble(0.05, 0.1)
    )
}

fun playParticleHitSound(velocity: Double, size: Double) {
    // Randomly drop some sounds
//    if (Random.nextDouble() > 0.5) return

    val impact = velocity * size * 25.0
    val volume = saturate((impact - 2.0) / 10).pow(1.5) * 0.25
    val pitch = Random.nextDouble(1.9, 2.1)

    playSound(
        "trim_rock ${Random.nextInt(12, 18)} low",
        pitch,
        volume * 0.5,
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
        "trim_glass ding ${Random.nextInt(1, 3)}",
        pitch,
        volume.pow(2) * 0.25,
        caveSplitter,
    )
}