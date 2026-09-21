package com.playtorrio.tv.ui.screens.player.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** RBJ peaking EQ biquad. One state object is required per channel. */
internal class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    private var z1 = 0.0
    private var z2 = 0.0

    fun setPeak(sampleRate: Int, frequencyHz: Double, q: Double, gainDb: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * frequencyHz / sampleRate.toDouble()
        val alpha = sin(omega) / (2.0 * q)
        val c = cos(omega)

        val rawB0 = 1.0 + alpha * a
        val rawB1 = -2.0 * c
        val rawB2 = 1.0 - alpha * a
        val rawA0 = 1.0 + alpha / a
        val rawA1 = -2.0 * c
        val rawA2 = 1.0 - alpha / a

        b0 = rawB0 / rawA0
        b1 = rawB1 / rawA0
        b2 = rawB2 / rawA0
        a1 = rawA1 / rawA0
        a2 = rawA2 / rawA0
    }

    fun process(input: Float): Float {
        val x = input.toDouble()
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y.toFloat()
    }

    fun reset() {
        z1 = 0.0
        z2 = 0.0
    }
}
