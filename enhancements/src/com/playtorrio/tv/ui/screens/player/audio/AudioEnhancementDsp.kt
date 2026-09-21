package com.playtorrio.tv.ui.screens.player.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Low-latency, channel-linked playback DSP for PlayTorrio.
 *
 * - Loudness equalization: soft-knee downward compression, capped upward gain for quiet program
 *   material, a noise-floor guard, makeup gain and a final limiter.
 * - Voice boost: broad dialogue-presence EQ. The same EQ is applied to every channel so channel
 *   layout assumptions never damage 5.1/7.1 balance.
 *
 * The processor is deliberately duration-preserving and allocation-free inside processInterleaved().
 */
class AudioEnhancementDsp(
    sampleRate: Int,
    channelCount: Int,
) {
    private data class LoudnessPreset(
        val thresholdDb: Double,
        val ratio: Double,
        val kneeDb: Double,
        val attackMs: Double,
        val releaseMs: Double,
        val upwardStrength: Double,
        val maxUpwardGainDb: Double,
        val makeupDb: Double,
    )

    private data class VoicePreset(
        val lowMidCutDb: Double,
        val presenceDb: Double,
        val clarityDb: Double,
        val preampDb: Double,
    )

    @Volatile
    var loudnessLevel: AudioEnhancementLevel = AudioEnhancementLevel.OFF

    @Volatile
    var voiceLevel: AudioEnhancementLevel = AudioEnhancementLevel.OFF

    private var sampleRate: Int = sampleRate
    private var channelCount: Int = channelCount

    private var envelope = 0.0
    private var smoothedGain = 1.0

    private var lowMidFilters: Array<Biquad> = emptyArray()
    private var presenceFilters: Array<Biquad> = emptyArray()
    private var clarityFilters: Array<Biquad> = emptyArray()
    private var configuredVoiceLevel = AudioEnhancementLevel.OFF

    init {
        require(sampleRate > 0)
        require(channelCount > 0)
        rebuildFilters()
    }

    fun reconfigure(sampleRate: Int, channelCount: Int) {
        require(sampleRate > 0)
        require(channelCount > 0)
        if (this.sampleRate == sampleRate && this.channelCount == channelCount) return
        this.sampleRate = sampleRate
        this.channelCount = channelCount
        envelope = 0.0
        smoothedGain = 1.0
        rebuildFilters()
    }

    fun reset() {
        envelope = 0.0
        smoothedGain = 1.0
        lowMidFilters.forEach { it.reset() }
        presenceFilters.forEach { it.reset() }
        clarityFilters.forEach { it.reset() }
    }

    /** Processes interleaved floating-point PCM in place. Samples are expected in [-1, +1]. */
    fun processInterleaved(samples: FloatArray, frames: Int = samples.size / channelCount) {
        if (frames <= 0 || samples.isEmpty()) return
        val loudness = loudnessLevel
        val voice = voiceLevel
        if (loudness == AudioEnhancementLevel.OFF && voice == AudioEnhancementLevel.OFF) return

        if (voice != configuredVoiceLevel) configureVoiceFilters(voice)

        val loudnessPreset = loudnessPreset(loudness)
        val voicePreset = voicePreset(voice)
        val preamp = dbToLinear(voicePreset.preampDb)
        val finalCeiling = dbToLinear(-1.0)

        val attackCoeff = if (loudnessPreset != null) timeCoeff(loudnessPreset.attackMs) else 0.0
        val releaseCoeff = if (loudnessPreset != null) timeCoeff(loudnessPreset.releaseMs) else 0.0
        val makeupLinear = if (loudnessPreset != null) dbToLinear(loudnessPreset.makeupDb) else 1.0

        var frame = 0
        while (frame < frames) {
            val base = frame * channelCount

            // One detector for all channels preserves relative channel balance and stereo image.
            var peak = 0.0
            var ch = 0
            while (ch < channelCount) {
                peak = max(peak, abs(samples[base + ch].toDouble()))
                ch++
            }

            var loudnessGain = 1.0
            if (loudnessPreset != null) {
                // Envelope follower. Fast when the signal rises, slower when it falls.
                val detectorCoeff = if (peak > envelope) attackCoeff else releaseCoeff
                envelope = detectorCoeff * envelope + (1.0 - detectorCoeff) * peak
                val levelDb = linearToDb(max(envelope, 1e-9))

                val downwardDb = compressorGainDb(
                    levelDb = levelDb,
                    thresholdDb = loudnessPreset.thresholdDb,
                    ratio = loudnessPreset.ratio,
                    kneeDb = loudnessPreset.kneeDb,
                )

                // Do not raise near-silence/noise. Quiet *program material* gets a capped lift.
                val upwardDb = if (levelDb > NOISE_FLOOR_DB && levelDb < loudnessPreset.thresholdDb) {
                    min(
                        loudnessPreset.maxUpwardGainDb,
                        (loudnessPreset.thresholdDb - levelDb) * loudnessPreset.upwardStrength,
                    )
                } else {
                    0.0
                }

                val targetGain = dbToLinear(downwardDb + upwardDb) * makeupLinear
                // Smooth gain changes further to prevent pumping/clicks on preset transitions.
                val gainCoeff = if (targetGain < smoothedGain) attackCoeff else releaseCoeff
                smoothedGain = gainCoeff * smoothedGain + (1.0 - gainCoeff) * targetGain
                loudnessGain = smoothedGain
            }

            ch = 0
            while (ch < channelCount) {
                var x = samples[base + ch] * loudnessGain.toFloat()

                if (voice != AudioEnhancementLevel.OFF) {
                    x *= preamp.toFloat()
                    x = lowMidFilters[ch].process(x)
                    x = presenceFilters[ch].process(x)
                    x = clarityFilters[ch].process(x)
                }

                // Transparent hard safety ceiling. It only acts on peaks after the EQ/compressor.
                val y = x.coerceIn(-finalCeiling.toFloat(), finalCeiling.toFloat())
                samples[base + ch] = y
                ch++
            }
            frame++
        }
    }

    private fun rebuildFilters() {
        lowMidFilters = Array(channelCount) { Biquad() }
        presenceFilters = Array(channelCount) { Biquad() }
        clarityFilters = Array(channelCount) { Biquad() }
        configuredVoiceLevel = AudioEnhancementLevel.OFF
        configureVoiceFilters(voiceLevel)
    }

    private fun configureVoiceFilters(level: AudioEnhancementLevel) {
        val preset = voicePreset(level)
        var ch = 0
        while (ch < channelCount) {
            lowMidFilters[ch].setPeak(sampleRate, 280.0, 0.85, preset.lowMidCutDb)
            presenceFilters[ch].setPeak(sampleRate, 2000.0, 0.90, preset.presenceDb)
            clarityFilters[ch].setPeak(sampleRate, 3900.0, 1.00, preset.clarityDb)
            lowMidFilters[ch].reset()
            presenceFilters[ch].reset()
            clarityFilters[ch].reset()
            ch++
        }
        configuredVoiceLevel = level
    }

    private fun loudnessPreset(level: AudioEnhancementLevel): LoudnessPreset? = when (level) {
        AudioEnhancementLevel.OFF -> null
        AudioEnhancementLevel.LOW -> LoudnessPreset(
            thresholdDb = -16.0,
            ratio = 2.0,
            kneeDb = 6.0,
            attackMs = 15.0,
            releaseMs = 300.0,
            upwardStrength = 0.24,
            maxUpwardGainDb = 3.0,
            makeupDb = 1.5,
        )
        AudioEnhancementLevel.MEDIUM -> LoudnessPreset(
            thresholdDb = -20.0,
            ratio = 3.0,
            kneeDb = 6.0,
            attackMs = 10.0,
            releaseMs = 350.0,
            upwardStrength = 0.34,
            maxUpwardGainDb = 6.0,
            makeupDb = 3.0,
        )
        AudioEnhancementLevel.MAX -> LoudnessPreset(
            thresholdDb = -24.0,
            ratio = 5.0,
            kneeDb = 6.0,
            attackMs = 5.0,
            releaseMs = 450.0,
            upwardStrength = 0.44,
            maxUpwardGainDb = 9.0,
            makeupDb = 4.5,
        )
    }

    private fun voicePreset(level: AudioEnhancementLevel): VoicePreset = when (level) {
        AudioEnhancementLevel.OFF -> VoicePreset(0.0, 0.0, 0.0, 0.0)
        AudioEnhancementLevel.LOW -> VoicePreset(-0.75, 2.0, 1.0, -0.5)
        AudioEnhancementLevel.MEDIUM -> VoicePreset(-1.5, 3.5, 2.0, -1.25)
        AudioEnhancementLevel.MAX -> VoicePreset(-2.0, 5.0, 3.0, -2.0)
    }

    private fun compressorGainDb(
        levelDb: Double,
        thresholdDb: Double,
        ratio: Double,
        kneeDb: Double,
    ): Double {
        val x = levelDb - thresholdDb
        val halfKnee = kneeDb / 2.0
        if (x <= -halfKnee) return 0.0
        if (x >= halfKnee) {
            val outputDb = thresholdDb + x / ratio
            return outputDb - levelDb
        }

        // Standard quadratic soft knee.
        val y = x + halfKnee
        return (1.0 / ratio - 1.0) * y * y / (2.0 * kneeDb)
    }

    private fun timeCoeff(milliseconds: Double): Double =
        exp(-1.0 / (sampleRate.toDouble() * milliseconds / 1000.0))

    private fun dbToLinear(db: Double): Double = 10.0.pow(db / 20.0)

    private fun linearToDb(linear: Double): Double = 20.0 * ln(linear) / ln(10.0)

    private companion object {
        const val NOISE_FLOOR_DB = -55.0
    }
}
