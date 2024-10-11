package com.oberdiah

import com.oberdiah.utils.GameTime
import kotlin.random.Random

object MusicCoordinator {
    private var musicPlaying = false
    private var chordPlayedLast = 1
    private var chordLastPlayedAt = Double.NEGATIVE_INFINITY
    const val NUM_CHORDS = 5

    fun startPlayingMusic() {
        musicPlaying = true
        chordLastPlayedAt = Double.NEGATIVE_INFINITY
    }

    fun stopPlayingMusic() {
        musicPlaying = false
    }

    fun tick() {
        if (!musicPlaying || statefulPlayMusicSetting.value == false) {
            return
        }

        if (GameTime.APP_TIME - chordLastPlayedAt > 6.0) {
            var newChord = Random.nextInt(1, NUM_CHORDS + 1)

            // new chord can't be the same as the last chord
            while (newChord == chordPlayedLast) {
                newChord = Random.nextInt(1, NUM_CHORDS + 1)
            }

            playChordsSound(newChord)

            chordPlayedLast = newChord
            chordLastPlayedAt = GameTime.APP_TIME
        }
    }
}