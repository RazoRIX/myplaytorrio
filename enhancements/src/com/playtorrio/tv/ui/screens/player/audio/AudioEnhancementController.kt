package com.playtorrio.tv.ui.screens.player.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi

/** Process-wide playback enhancement state. It survives normal PlayerScreen recreation. */
@UnstableApi
object AudioEnhancementController {
    val processor = PlayTorrioAudioEnhancementProcessor()

    var loudnessLevel by mutableStateOf(AudioEnhancementLevel.OFF)
        private set

    var voiceLevel by mutableStateOf(AudioEnhancementLevel.OFF)
        private set

    fun setLoudness(level: AudioEnhancementLevel) {
        loudnessLevel = level
        processor.setLevels(loudnessLevel, voiceLevel)
    }

    fun setVoice(level: AudioEnhancementLevel) {
        voiceLevel = level
        processor.setLevels(loudnessLevel, voiceLevel)
    }
}
