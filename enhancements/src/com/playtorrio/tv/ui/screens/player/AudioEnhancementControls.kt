package com.playtorrio.tv.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.util.UnstableApi
import com.playtorrio.tv.ui.screens.player.audio.AudioEnhancementController
import com.playtorrio.tv.ui.screens.player.audio.AudioEnhancementLevel

/** Two remote-friendly controls intended to sit directly beside the player's CC button. */
@UnstableApi
@Composable
fun AudioEnhancementControls(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EnhancementButton(
            title = "Loudness Equalization",
            level = AudioEnhancementController.loudnessLevel,
            icon = EnhancementIcon.EQUALIZER,
            onSelected = AudioEnhancementController::setLoudness,
        )
        EnhancementButton(
            title = "Voice Boost",
            level = AudioEnhancementController.voiceLevel,
            icon = EnhancementIcon.VOICE,
            onSelected = AudioEnhancementController::setVoice,
        )
    }
}

private enum class EnhancementIcon { EQUALIZER, VOICE }

@Composable
private fun EnhancementButton(
    title: String,
    level: AudioEnhancementLevel,
    icon: EnhancementIcon,
    onSelected: (AudioEnhancementLevel) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var selectorOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.22f)
                    level != AudioEnhancementLevel.OFF -> Color.White.copy(alpha = 0.13f)
                    else -> Color.Black.copy(alpha = 0.35f)
                },
            )
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.25f),
                shape = CircleShape,
            )
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = "$title: ${level.displayName()}" }
            .clickable { selectorOpen = true },
        contentAlignment = Alignment.Center,
    ) {
        EnhancementGlyph(icon = icon, active = level != AudioEnhancementLevel.OFF)
        if (level != AudioEnhancementLevel.OFF) {
            BasicText(
                text = level.shortName(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }

    if (selectorOpen) {
        EnhancementLevelPopup(
            title = title,
            selected = level,
            onSelected = {
                onSelected(it)
                selectorOpen = false
            },
            onDismiss = { selectorOpen = false },
        )
    }
}

@Composable
private fun EnhancementGlyph(icon: EnhancementIcon, active: Boolean) {
    val color = if (active) Color.White else Color.White.copy(alpha = 0.82f)
    Canvas(modifier = Modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.1.dp.toPx()
        when (icon) {
            EnhancementIcon.EQUALIZER -> {
                val xs = listOf(w * 0.24f, w * 0.50f, w * 0.76f)
                val knobs = listOf(h * 0.36f, h * 0.68f, h * 0.46f)
                xs.indices.forEach { i ->
                    drawLine(
                        color = color,
                        start = Offset(xs[i], h * 0.14f),
                        end = Offset(xs[i], h * 0.86f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = color,
                        radius = 3.3.dp.toPx(),
                        center = Offset(xs[i], knobs[i]),
                    )
                }
            }
            EnhancementIcon.VOICE -> {
                drawCircle(
                    color = color,
                    radius = w * 0.15f,
                    center = Offset(w * 0.35f, h * 0.31f),
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = color,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.38f),
                    size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.40f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = -52f,
                    sweepAngle = 104f,
                    useCenter = false,
                    topLeft = Offset(w * 0.47f, h * 0.28f),
                    size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.44f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = -52f,
                    sweepAngle = 104f,
                    useCenter = false,
                    topLeft = Offset(w * 0.54f, h * 0.17f),
                    size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.66f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun EnhancementLevelPopup(
    title: String,
    selected: AudioEnhancementLevel,
    onSelected: (AudioEnhancementLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    val levels = AudioEnhancementLevel.values().toList()
    val requesters = remember { List(levels.size) { FocusRequester() } }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(330.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF21A1A1A))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .padding(18.dp),
        ) {
            BasicText(
                text = title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.height(12.dp))
            levels.forEachIndexed { index, level ->
                LevelRow(
                    label = level.displayName(),
                    selected = level == selected,
                    focusRequester = requesters[index],
                    onClick = { onSelected(level) },
                )
                if (index != levels.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
    }

    LaunchedEffect(selected) {
        requesters[selected.ordinal].requestFocus()
    }
}

@Composable
private fun LevelRow(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.20f)
                    selected -> Color.White.copy(alpha = 0.11f)
                    else -> Color.Transparent
                },
            )
            .border(
                if (focused) 1.5.dp else 0.dp,
                if (focused) Color.White else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = if (selected) "✓" else " ",
            style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.width(12.dp))
        BasicText(
            text = label,
            style = TextStyle(
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}

private fun AudioEnhancementLevel.displayName(): String = when (this) {
    AudioEnhancementLevel.OFF -> "Off"
    AudioEnhancementLevel.LOW -> "Low"
    AudioEnhancementLevel.MEDIUM -> "Medium"
    AudioEnhancementLevel.MAX -> "Max"
}

private fun AudioEnhancementLevel.shortName(): String = when (this) {
    AudioEnhancementLevel.OFF -> ""
    AudioEnhancementLevel.LOW -> "L"
    AudioEnhancementLevel.MEDIUM -> "M"
    AudioEnhancementLevel.MAX -> "MAX"
}
