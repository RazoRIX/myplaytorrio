package com.playtorrio.tv.ui.screens.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.playtorrio.tv.ui.screens.player.audio.AudioEnhancementController

/**
 * Renderer factory for the enhanced player.
 *
 * The no-context DefaultAudioSink builder intentionally exposes only default PCM output
 * capabilities, which disables encoded passthrough/offload. That guarantees our AudioProcessor
 * receives decoded PCM instead of being bypassed by AC3/EAC3/DTS bitstream output.
 */
@UnstableApi
class EnhancedRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    init {
        // Keep PlayTorrio's extension decoders (including its bundled FFmpeg renderer) preferred.
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
    }

    @Suppress("DEPRECATION")
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink {
        return DefaultAudioSink.Builder()
            .setEnableFloatOutput(false)
            .setAudioProcessors(arrayOf(AudioEnhancementController.processor))
            .build()
    }
}
