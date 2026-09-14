package com.playtorrio.tv.ui.screens.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStallWatchdogPolicyTest {

    @Test
    fun `user paused manually skips watchdog`() {
        val decision = PlayerStallWatchdogPolicy.evaluate(
            PlayerStallWatchdogPolicy.Input(
                bufferedPositionMs = 5000L,
                playheadMs = 5000L,
                durationMs = 60000L,
                stalledForMs = 10000L,
                userPausedManually = true,
                isLive = true
            )
        )
        assertEquals(PlayerStallWatchdogPolicy.Decision.SkipUserPaused, decision)
    }

    @Test
    fun `live stream while playing keeps waiting before threshold`() {
        val decision = PlayerStallWatchdogPolicy.evaluate(
            PlayerStallWatchdogPolicy.Input(
                bufferedPositionMs = 5000L,
                playheadMs = 5000L,
                durationMs = C.TIME_UNSET,
                stalledForMs = 2900L,
                isLive = true,
                hasRenderedFirstFrame = true
            )
        )
        assertEquals(PlayerStallWatchdogPolicy.Decision.KeepWaiting, decision)
    }

    @Test
    fun `live stream while playing reconnects at or past 3000ms threshold`() {
        val decision = PlayerStallWatchdogPolicy.evaluate(
            PlayerStallWatchdogPolicy.Input(
                bufferedPositionMs = 5000L,
                playheadMs = 5000L,
                durationMs = C.TIME_UNSET,
                stalledForMs = 3000L,
                isLive = true,
                hasRenderedFirstFrame = true
            )
        )
        assertEquals(PlayerStallWatchdogPolicy.Decision.ReconnectLiveStream, decision)
    }

    @Test
    fun `live stream during startup keeps waiting before 6000ms threshold`() {
        val decision = PlayerStallWatchdogPolicy.evaluate(
            PlayerStallWatchdogPolicy.Input(
                bufferedPositionMs = 0L,
                playheadMs = 0L,
                durationMs = C.TIME_UNSET,
                stalledForMs = 5500L,
                isLive = true,
                hasRenderedFirstFrame = false
            )
        )
        assertEquals(PlayerStallWatchdogPolicy.Decision.KeepWaiting, decision)
    }

    @Test
    fun `live stream during startup reconnects at 6000ms threshold`() {
        val decision = PlayerStallWatchdogPolicy.evaluate(
            PlayerStallWatchdogPolicy.Input(
                bufferedPositionMs = 0L,
                playheadMs = 0L,
                durationMs = C.TIME_UNSET,
                stalledForMs = 6000L,
                isLive = true,
                hasRenderedFirstFrame = false
            )
        )
        assertEquals(PlayerStallWatchdogPolicy.Decision.ReconnectLiveStream, decision)
    }

    @Test
    fun `vod stream with unknown duration skips seek`() {
        val decision = PlayerStallWatchdogPolicy.evaluate(
            PlayerStallWatchdogPolicy.Input(
                bufferedPositionMs = 5000L,
                playheadMs = 5000L,
                durationMs = C.TIME_UNSET,
                stalledForMs = 16000L,
                thresholdMs = 15000L,
                isLive = false
            )
        )
        assertEquals(PlayerStallWatchdogPolicy.Decision.SkipUnknownDuration, decision)
    }

    @Test
    fun `vod stream stalled with buffer ahead seeks past buffered edge`() {
        val decision = PlayerStallWatchdogPolicy.evaluate(
            PlayerStallWatchdogPolicy.Input(
                bufferedPositionMs = 10000L,
                playheadMs = 5000L,
                durationMs = 60000L,
                stalledForMs = 16000L,
                thresholdMs = 15000L,
                skipPastBufferedMs = 250L,
                isLive = false
            )
        )
        assertTrue(decision is PlayerStallWatchdogPolicy.Decision.SeekPastBufferedEdge)
        assertEquals(10250L, (decision as PlayerStallWatchdogPolicy.Decision.SeekPastBufferedEdge).targetMs)
    }
}
