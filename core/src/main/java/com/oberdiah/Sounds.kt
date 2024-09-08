package com.oberdiah

import com.badlogic.gdx.assets.AssetManager
import games.rednblack.miniaudio.MASound
import games.rednblack.miniaudio.MiniAudio
import games.rednblack.miniaudio.effect.MADelayNode
import games.rednblack.miniaudio.filter.MALowPassFilter
import games.rednblack.miniaudio.loader.MASoundLoader
import games.rednblack.miniaudio.mix.MASplitter
import kotlin.math.pow
import kotlin.random.Random


private var SOUND_POOL: Map<String, List<MASound>> = mutableMapOf()
lateinit var miniAudio: MiniAudio
lateinit var assetManager: AssetManager
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

    assetManager = AssetManager()
    assetManager.setLoader(
        MASound::class.java,
        MASoundLoader(miniAudio, assetManager.getFileHandleResolver())
    )

    for (file in listFolder("Sounds/")) {
        assetManager.load(file.path(), MASound::class.java)
    }

    assetManager.finishLoading()

    val numSoundsInPoolPerAsset = 25

    listFolder("Sounds/").forEach {
        val path = it.path()
        val sounds = (0 until numSoundsInPoolPerAsset).map {
            assetManager.get(
                path,
                MASound::class.java
            )
        }
        SOUND_POOL += it.nameWithoutExtension() to sounds
    }
}

fun disposeSounds() {
    // Dispose all sounds
    SOUND_POOL.forEach { (_, sounds) ->
        sounds.forEach { it.dispose() }
    }

    assetManager.dispose()
    miniAudio.dispose();
}

fun playSound(
    soundName: String,
    pitch: Double = 1.0,
    volume: Double = 1.0,
    splitter: MASplitter? = null
) {
    // Get the first sound that isn't playing (sound.isPlaying())
    val sound = SOUND_POOL[soundName]?.firstOrNull { !it.isPlaying }

    if (sound == null) {
        return
    }

    if (volume < 0.005) return
    sound.setVolume(volume.f)
    sound.setPitch(pitch.f)
    // Random pan
    sound.setPan(Random.nextDouble(-0.5, 0.5).f)
    sound.fadeIn(0.1f)

    splitter?.attachToThisNode(sound, 0)

    sound.play()
}

fun playExplosionSound(force: Double) {
    // Force generally ranges from 0 to 2 for the very large explosions

    var pitch = 0.7 - force * 0.3

    /// Vary the pitch a bit
    pitch += Random.nextDouble(-0.05, 0.05)

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
//        playSound("trim_metal hits metal 7", ReasonSoundCreated.Explosion, 0.05, volume = 0.5)
        playSound(
            "trim_orange hit hard ${Random.nextInt(1, 10)}",
            max(pitch, 0.1),
            volume = 0.5
        )
        playSound(
            "trim_trim_bat hit 4",
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

fun playParticleSound(velocity: Double, size: Double) {
    // Randomly drop some sounds
    if (Random.nextDouble() > 0.5) return

    val impact = velocity * size * 25.0
    val volume = saturate((impact - 2.0) / 10).pow(1.5) * 0.25
    val pitch = Random.nextDouble(1.0, 2.6)
    playSound(
        "trim_glass clink ${Random.nextInt(2, 6)}",
        pitch,
        volume,
        caveSplitter
    )
}