package com.oberdiah.sounds

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.oberdiah.SoundData
import com.oberdiah.f
import com.oberdiah.listFolder
import com.oberdiah.platformInterface
import games.rednblack.miniaudio.MAFormatType
import games.rednblack.miniaudio.MASound
import games.rednblack.miniaudio.MiniAudio
import games.rednblack.miniaudio.effect.MADelayNode
import games.rednblack.miniaudio.filter.MALowPassFilter
import games.rednblack.miniaudio.mix.MASplitter
import kotlin.random.Random

class MiniAudioCore: SoundCore {
    val MAX_NUM_SOUNDS_PLAYING = 30

    private var SOUND_POOL: Map<String, List<MASound>> = mutableMapOf()
    private lateinit var miniAudio: MiniAudio
    private lateinit var caveSplitter: MASplitter
    private val allSoundsPlaying = sortedSetOf<PlayingSound>()

    override fun tick() {
        allSoundsPlaying.removeAll { !it.sound.isPlaying }
    }

    override fun playSound(soundData: SoundData) {
        // Get the first sound that isn't playing (sound.isPlaying())
        val soundPool = SOUND_POOL[soundData.name]
        if (soundPool == null) {
            println("Sound not found: ${soundData.name}")
            return
        }

        val sound = soundPool.firstOrNull { !it.isPlaying }
        if (sound == null) {
            return
        }

        // We know we are more important than the quietest sound playing, so we can kick them out
        if (allSoundsPlaying.size >= MAX_NUM_SOUNDS_PLAYING) {
            if (soundData.volume < (allSoundsPlaying.firstOrNull()?.data?.volume ?: 0.0)) {
                // We're too quiet to play this sound right now
                return
            }

            allSoundsPlaying.first().sound.stop()
            allSoundsPlaying.remove(allSoundsPlaying.first())
        }

        sound.setVolume(soundData.volume.f)
        sound.setPitch(soundData.pitch.f)
        // Random pan
        sound.setPan(Random.nextDouble(-0.5, 0.5).f)
        sound.play()

        var splitter = if (soundData.withReverb) caveSplitter else null

        if (splitter != null) {
            splitter.attachToThisNode(sound, 0)
        } else {
            miniAudio.attachToEngineOutput(sound, 0)
        }

        allSoundsPlaying.add(PlayingSound(sound, soundData))
    }

    override fun pause() {
        miniAudio.stopEngine();
    }

    override fun resume() {
        miniAudio.startEngine();
    }

    override fun initialize() {
        miniAudio = MiniAudio(null, false, true, false)
        var latencyMillis = 0 // Default
        miniAudio.initEngine(1, -1, -1, 0, latencyMillis, 0, 0, MAFormatType.F32, false, false, true)

        platformInterface.injectAssetManager(miniAudio)
        caveSplitter = MASplitter(miniAudio)
        val lowPassFilter = MALowPassFilter(miniAudio, 550.0, 8)
        val delayNode = MADelayNode(miniAudio, 0.1f, 0.1f)

        miniAudio.attachToEngineOutput(lowPassFilter, 0)
        miniAudio.attachToEngineOutput(delayNode, 0)

        lowPassFilter.attachToThisNode(caveSplitter, 0)
        delayNode.attachToThisNode(caveSplitter, 1)

        val numSoundsInPoolPerAsset = 10

        listFolder("Sounds/").forEach {
            val path = it.path()
            val sounds = (0 until numSoundsInPoolPerAsset).map {
                miniAudio.createSound(path)
            }
            SOUND_POOL += it.nameWithoutExtension() to sounds
        }
    }

    override fun dispose() {
        // I trust Windows to just do the cleanup for us - I'd like to be more responsible
        // on Android
        if (Gdx.app.type != Application.ApplicationType.Desktop) {
            SOUND_POOL.forEach { (_, sounds) ->
                sounds.forEach { it.dispose() }
            }

            miniAudio.dispose()
        }
    }

    data class PlayingSound(
        val sound: MASound,
        val data: SoundData
    ) : Comparable<PlayingSound> {
        override fun compareTo(other: PlayingSound): Int {
            return data.volume.compareTo(other.data.volume)
        }
    }
}