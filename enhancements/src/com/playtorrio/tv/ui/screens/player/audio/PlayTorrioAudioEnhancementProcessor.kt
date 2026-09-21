package com.playtorrio.tv.ui.screens.player.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Media3 PCM processor used by the enhanced PlayTorrio build.
 *
 * It intentionally works on PCM 16-bit. EnhancedRenderersFactory disables encoded
 * passthrough/offload and float output so Dolby/DTS/etc. are decoded before reaching here.
 */
@UnstableApi
class PlayTorrioAudioEnhancementProcessor : BaseAudioProcessor() {
    @Volatile
    private var requestedLoudness = AudioEnhancementLevel.OFF

    @Volatile
    private var requestedVoice = AudioEnhancementLevel.OFF

    private var dsp: AudioEnhancementDsp? = null
    private var scratch = FloatArray(0)

    fun setLevels(loudness: AudioEnhancementLevel, voice: AudioEnhancementLevel) {
        requestedLoudness = loudness
        requestedVoice = voice
        dsp?.let {
            it.loudnessLevel = loudness
            it.voiceLevel = voice
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    // Kept on the deprecated no-arg hook deliberately: current Media3 still bridges to it,
    // and this remains source-compatible with the slightly older customized AARs used by
    // PlayTorrio builds.
    @Suppress("DEPRECATION")
    override fun onFlush() {
        val format = inputAudioFormat
        dsp = AudioEnhancementDsp(format.sampleRate, format.channelCount).also {
            it.loudnessLevel = requestedLoudness
            it.voiceLevel = requestedVoice
        }
    }

    override fun onReset() {
        dsp = null
        scratch = FloatArray(0)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val format = inputAudioFormat
        val channels = format.channelCount
        val inputBytes = inputBuffer.remaining()
        val sampleCount = inputBytes / 2
        if (sampleCount == 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        if (scratch.size < sampleCount) scratch = FloatArray(sampleCount)

        val originalOrder = inputBuffer.order()
        inputBuffer.order(ByteOrder.nativeOrder())
        var i = 0
        while (i < sampleCount) {
            scratch[i] = inputBuffer.short / 32768.0f
            i++
        }
        inputBuffer.order(originalOrder)

        val localDsp = dsp ?: AudioEnhancementDsp(format.sampleRate, channels).also { dsp = it }
        localDsp.loudnessLevel = requestedLoudness
        localDsp.voiceLevel = requestedVoice
        localDsp.processInterleaved(scratch, sampleCount / channels)

        val output = replaceOutputBuffer(inputBytes).order(ByteOrder.nativeOrder())
        i = 0
        while (i < sampleCount) {
            val s = (scratch[i].coerceIn(-1.0f, 1.0f) * 32767.0f).toInt()
            output.putShort(s.toShort())
            i++
        }
        output.flip()
    }
}
