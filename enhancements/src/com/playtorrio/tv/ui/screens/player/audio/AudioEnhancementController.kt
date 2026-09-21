package com.playtorrio.tv.ui.screens.player.audio

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.util.Collections
import java.util.WeakHashMap

/**
 * Audio enhancements attached to the AudioTrack session used by Media3/ExoPlayer.
 *
 * This deliberately avoids replacing PlayTorrio's customized renderer factory/audio sink.
 * LoudnessEnhancer raises quieter material and compresses samples that would exceed the
 * platform sample range. Equalizer applies a speech-presence curve for dialogue clarity.
 */
@UnstableApi
object AudioEnhancementController {
    var loudnessLevel by mutableStateOf(AudioEnhancementLevel.OFF)
        private set

    var voiceLevel by mutableStateOf(AudioEnhancementLevel.OFF)
        private set

    private val attachedPlayers = Collections.newSetFromMap(WeakHashMap<Player, Boolean>())

    private var boundSessionId: Int = 0
    private var loudnessEffect: LoudnessEnhancer? = null
    private var equalizerEffect: Equalizer? = null

    @Synchronized
    fun attachToPlayer(player: Player) {
        if (!attachedPlayers.add(player)) return

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                bindSession(audioSessionId)
            }
        })

        runCatching { player.audioSessionId }
            .getOrNull()
            ?.takeIf { it > 0 }
            ?.let(::bindSession)
    }

    @Synchronized
    fun setLoudness(level: AudioEnhancementLevel) {
        loudnessLevel = level
        applyLoudness()
    }

    @Synchronized
    fun setVoice(level: AudioEnhancementLevel) {
        voiceLevel = level
        applyVoiceBoost()
    }

    @Synchronized
    private fun bindSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == boundSessionId && (loudnessEffect != null || equalizerEffect != null)) {
            applyAll()
            return
        }

        releaseEffects()
        boundSessionId = audioSessionId

        loudnessEffect = runCatching {
            LoudnessEnhancer(audioSessionId)
        }.getOrNull()

        equalizerEffect = runCatching {
            Equalizer(0, audioSessionId)
        }.getOrNull()

        applyAll()
    }

    private fun applyAll() {
        applyLoudness()
        applyVoiceBoost()
    }

    /**
     * 3/6/9 dB target gain. LoudnessEnhancer compresses samples which would otherwise
     * exceed the supported sample range, so quieter passages come forward without simply
     * allowing loud peaks to clip.
     */
    private fun applyLoudness() {
        val effect = loudnessEffect ?: return
        val gainMb = when (loudnessLevel) {
            AudioEnhancementLevel.OFF -> 0
            AudioEnhancementLevel.LOW -> 300
            AudioEnhancementLevel.MEDIUM -> 600
            AudioEnhancementLevel.MAX -> 900
        }

        runCatching {
            effect.setTargetGain(gainMb)
            effect.setEnabled(loudnessLevel != AudioEnhancementLevel.OFF)
        }
    }

    /**
     * Dialogue curve: slightly cleans low/low-mid energy and emphasizes the 1-5 kHz
     * speech-presence region. Values are clamped to the EQ implementation's supported range.
     */
    private fun applyVoiceBoost() {
        val eq = equalizerEffect ?: return

        runCatching {
            if (voiceLevel == AudioEnhancementLevel.OFF) {
                // Reset all bands before bypassing so re-enabling starts from a known state.
                for (i in 0 until eq.numberOfBands.toInt()) {
                    eq.setBandLevel(i.toShort(), 0)
                }
                eq.setEnabled(false)
                return@runCatching
            }

            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()

            for (i in 0 until eq.numberOfBands.toInt()) {
                val band = i.toShort()
                val hz = eq.getCenterFreq(band) / 1000.0
                val gainDb = voiceGainDb(hz, voiceLevel)
                val gainMb = (gainDb * 100.0).toInt().coerceIn(minMb, maxMb)
                eq.setBandLevel(band, gainMb.toShort())
            }
            eq.setEnabled(true)
        }
    }

    private fun voiceGainDb(centerHz: Double, level: AudioEnhancementLevel): Double {
        // Piecewise curve chosen to preserve music/effects while increasing consonant clarity.
        return when (level) {
            AudioEnhancementLevel.OFF -> 0.0
            AudioEnhancementLevel.LOW -> when {
                centerHz < 250.0 -> -0.5
                centerHz < 800.0 -> -0.5
                centerHz < 1600.0 -> 1.0
                centerHz < 5000.0 -> 2.5
                centerHz < 9000.0 -> 0.75
                else -> 0.0
            }
            AudioEnhancementLevel.MEDIUM -> when {
                centerHz < 250.0 -> -1.0
                centerHz < 800.0 -> -1.0
                centerHz < 1600.0 -> 1.5
                centerHz < 5000.0 -> 4.0
                centerHz < 9000.0 -> 1.25
                else -> 0.0
            }
            AudioEnhancementLevel.MAX -> when {
                centerHz < 250.0 -> -1.5
                centerHz < 800.0 -> -2.0
                centerHz < 1600.0 -> 2.0
                centerHz < 5000.0 -> 6.0
                centerHz < 9000.0 -> 2.0
                else -> 0.0
            }
        }
    }

    @Synchronized
    fun releaseEffects() {
        runCatching { loudnessEffect?.setEnabled(false) }
        runCatching { equalizerEffect?.setEnabled(false) }
        runCatching { loudnessEffect?.release() }
        runCatching { equalizerEffect?.release() }
        loudnessEffect = null
        equalizerEffect = null
        boundSessionId = 0
    }
}
