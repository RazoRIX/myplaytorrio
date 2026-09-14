package com.playtorrio.tv.ui.screens.iptv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.view.KeyEvent as AndroidKeyEvent
import com.playtorrio.tv.ui.util.isSelectKey
import com.playtorrio.tv.ui.util.rememberLongPressKeyTracker
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import com.playtorrio.tv.core.iptv.model.M3uPlaylist
import com.playtorrio.tv.core.iptv.model.QuickChannel
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.playtorrio.tv.core.iptv.channels.HardcodedChannel

@Composable
fun HardcodedChannelCard(
    channel: HardcodedChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(12.dp) }
    val bgGradient = remember(channel.gradient) {
        if (channel.gradient.isNotEmpty()) {
            Brush.verticalGradient(channel.gradient)
        } else {
            Brush.verticalGradient(listOf(Color(0xFF1E2235), Color(0xFF131522)))
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered

    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.05f else 1.0f,
        label = "ch_card_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = cardShape,
        color = if (isHighlighted) Color(0xFF1E2235) else Color(0xFF131522),
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) Color(0xFF38BDF8) else Color(0x33FFFFFF)
        ),
        modifier = modifier
            .width(180.dp)
            .height(112.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(10.dp)
        ) {
            // Channel Logo or Icon
            if (!channel.iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.iconUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.short,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Top Badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Name Pill
            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun SpotlightHeroCarousel(
    featuredChannels: List<HardcodedChannel>,
    onWatchNow: (HardcodedChannel) -> Unit,
    onSourcesTap: (HardcodedChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredChannels.isEmpty()) return
    val channel = featuredChannels.first()

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0284C7)
                    )
                )
            )
    ) {
        // Backdrop Image if available
        if (!channel.backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = channel.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xEE0B0F19),
                                Color(0x990B0F19),
                                Color(0x44000000)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE50914))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SPOTLIGHT LIVE BROADCAST",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Live coverage, championship fights, and premier sports broadcasts powered by community scrapers.",
                color = Color(0xCCFFFFFF),
                fontSize = 13.sp,
                maxLines = 2,
                modifier = Modifier.width(420.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpotlightButton(
                    onClick = { onWatchNow(channel) },
                    containerColor = Color(0xFF38BDF8),
                    focusedContainerColor = Color(0xFF7DD3FC),
                    contentColor = Color.Black,
                    focusedBorderColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Watch Live", fontWeight = FontWeight.Bold)
                }

                SpotlightButton(
                    onClick = { onSourcesTap(channel) },
                    containerColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0x55FFFFFF),
                    contentColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8)
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Sources & Feeds", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SpotlightButton(
    onClick: () -> Unit,
    containerColor: Color,
    focusedContainerColor: Color,
    contentColor: Color,
    focusedBorderColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered

    val scale by animateFloatAsState(if (isHighlighted) 1.05f else 1.0f, label = "spotlight_btn_scale")

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlighted) focusedContainerColor else containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (isHighlighted) 2.dp else 1.dp, if (isHighlighted) focusedBorderColor else Color.Transparent),
        modifier = Modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IptvCategoryRow(
    title: String,
    subtitle: String,
    channels: List<HardcodedChannel>,
    onChannelClick: (HardcodedChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) return

    val density = LocalDensity.current
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val horizontalBringIntoViewSpec = remember(density, defaultBringIntoViewSpec, isRtl) {
        val startPx = with(density) { 24.dp.roundToPx() }
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        object : BringIntoViewSpec {
            override val scrollAnimationSpec: AnimationSpec<Float> =
                defaultBringIntoViewSpec.scrollAnimationSpec
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val childSize = kotlin.math.abs(size)
                if (isRtl) {
                    val childSmallerThanParent = childSize <= containerSize
                    val initialTarget = containerSize - startPx.toFloat()
                    val targetForTrailingEdge =
                        if (childSmallerThanParent && initialTarget < childSize) {
                            childSize
                        } else {
                            initialTarget
                        }
                    return (offset + size) - targetForTrailingEdge
                } else {
                    val target = startPx.toFloat()
                    val space = containerSize - target
                    val leading = if (childSize <= containerSize && space < childSize) containerSize - childSize else target
                    return offset - leading
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color(0x88FFFFFF),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        CompositionLocalProvider(LocalBringIntoViewSpec provides horizontalBringIntoViewSpec) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                contentPadding = PaddingValues(start = 24.dp, end = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    HardcodedChannelCard(
                        channel = channel,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickChannelsRow(
    channels: List<QuickChannel>,
    onChannelClick: (QuickChannel) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: ((QuickChannel) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(12.dp) }
    var quickForOptions by remember { mutableStateOf<QuickChannel?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Quick Channels",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Custom channel shortcuts that search portals using your keywords (Hold OK to manage)",
                color = Color(0x88FFFFFF),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            contentPadding = PaddingValues(start = 24.dp, end = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Add New Button Card
            item(key = "add_quick_channel") {
                val addInteraction = remember { MutableInteractionSource() }
                val isFocused by addInteraction.collectIsFocusedAsState()
                val isHovered by addInteraction.collectIsHoveredAsState()
                val isHighlighted = isFocused || isHovered
                val scale by animateFloatAsState(if (isHighlighted) 1.05f else 1.0f, label = "add_quick_scale")

                Surface(
                    onClick = onAddClick,
                    interactionSource = addInteraction,
                    shape = cardShape,
                    color = if (isHighlighted) Color(0xFF1E2D4A) else Color(0xFF131D31),
                    border = BorderStroke(
                        width = if (isHighlighted) 2.dp else 1.dp,
                        color = if (isHighlighted) Color(0xFF38BDF8) else Color(0x3338BDF8)
                    ),
                    modifier = Modifier
                        .width(160.dp)
                        .height(112.dp)
                        .scale(scale)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3338BDF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Channel",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            items(channels, key = { it.id }) { quick ->
                QuickChannelCardItem(
                    quick = quick,
                    onClick = { onChannelClick(quick) },
                    onLongClick = { quickForOptions = quick },
                    onDeleteClick = if (onDeleteClick != null) { { onDeleteClick(quick) } } else null,
                    cardShape = cardShape
                )
            }
        }
    }

    quickForOptions?.let { quick ->
        QuickChannelActionDialog(
            quick = quick,
            onDismiss = { quickForOptions = null },
            onOpen = { onChannelClick(quick) },
            onDelete = { onDeleteClick?.invoke(quick) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickChannelCardItem(
    quick: QuickChannel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    cardShape: RoundedCornerShape
) {
    val bgGradient = remember(quick.gradient) {
        if (quick.gradient.isNotEmpty()) Brush.verticalGradient(quick.gradient)
        else Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFF1E1B4B)))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered

    val scale by animateFloatAsState(if (isHighlighted) 1.05f else 1.0f, label = "quick_card_scale")
    val longPressKeyTracker = rememberLongPressKeyTracker()
    var longPressTriggered by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .width(180.dp)
            .height(112.dp)
            .scale(scale)
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (native.action == AndroidKeyEvent.ACTION_DOWN) {
                    if (native.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                        longPressTriggered = true
                        onLongClick()
                        return@onPreviewKeyEvent true
                    }
                }
                if (longPressKeyTracker.handle(native, ::isSelectKey) {
                    longPressTriggered = true
                    onLongClick()
                }) {
                    if (native.action == AndroidKeyEvent.ACTION_UP) {
                        longPressTriggered = false
                    }
                    return@onPreviewKeyEvent true
                }
                if (native.action == AndroidKeyEvent.ACTION_UP &&
                    longPressTriggered &&
                    (isSelectKey(native.keyCode) || native.keyCode == AndroidKeyEvent.KEYCODE_MENU)
                ) {
                    longPressTriggered = false
                    return@onPreviewKeyEvent true
                }
                false
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = cardShape,
        color = if (isHighlighted) Color(0xFF1E2235) else Color(0xFF131522),
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) Color(0xFF38BDF8) else Color(0x33FFFFFF)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(10.dp)
        ) {
            if (!quick.iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = quick.iconUrl,
                    contentDescription = quick.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(60.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quick.short,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            // Top Pill
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUICK",
                    color = Color(0xFFBAE6FD),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Delete button for mouse/touch
            if (onDeleteClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xCCEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Bottom Name
            Text(
                text = quick.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun M3uPlaylistsRow(
    playlists: List<M3uPlaylist>,
    onPlaylistClick: (M3uPlaylist) -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = remember { RoundedCornerShape(12.dp) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "M3U Playlists",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Custom M3U/M3U8 playlists imported from URL or pasted content",
                color = Color(0x88FFFFFF),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            contentPadding = PaddingValues(start = 24.dp, end = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Manage / Add M3U Card
            item(key = "manage_m3u") {
                val m3uManageInteraction = remember { MutableInteractionSource() }
                val isFocused by m3uManageInteraction.collectIsFocusedAsState()
                val isHovered by m3uManageInteraction.collectIsHoveredAsState()
                val isHighlighted = isFocused || isHovered
                val scale by animateFloatAsState(if (isHighlighted) 1.05f else 1.0f, label = "m3u_manage_scale")

                Surface(
                    onClick = onManageClick,
                    interactionSource = m3uManageInteraction,
                    shape = cardShape,
                    color = if (isHighlighted) Color(0xFF16382E) else Color(0xFF0F231D),
                    border = BorderStroke(
                        width = if (isHighlighted) 2.dp else 1.dp,
                        color = if (isHighlighted) Color(0xFF10B981) else Color(0x3310B981)
                    ),
                    modifier = Modifier
                        .width(180.dp)
                        .height(112.dp)
                        .scale(scale)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3310B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FeaturedPlayList,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "M3U Manager",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            items(playlists, key = { it.id }) { playlist ->
                val plInteraction = remember { MutableInteractionSource() }
                val isFocused by plInteraction.collectIsFocusedAsState()
                val isHovered by plInteraction.collectIsHoveredAsState()
                val isHighlighted = isFocused || isHovered
                val scale by animateFloatAsState(if (isHighlighted) 1.05f else 1.0f, label = "m3u_pl_scale")

                Surface(
                    onClick = { onPlaylistClick(playlist) },
                    interactionSource = plInteraction,
                    shape = cardShape,
                    color = if (isHighlighted) Color(0xFF334155) else Color(0xFF1E293B),
                    border = BorderStroke(
                        width = if (isHighlighted) 2.dp else 1.dp,
                        color = if (isHighlighted) Color(0xFF38BDF8) else Color(0x33FFFFFF)
                    ),
                    modifier = Modifier
                        .width(200.dp)
                        .height(112.dp)
                        .scale(scale)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = playlist.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x3310B981))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${playlist.count} ch",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (playlist.url.isNotBlank()) "Remote URL" else "Local / Pasted",
                                    color = Color(0x88FFFFFF),
                                    fontSize = 11.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickChannelActionDialog(
    quick: QuickChannel,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var canInteract by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(350)
        canInteract = true
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0x3338BDF8)),
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = quick.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quick Channel Options",
                    color = Color(0x88FFFFFF),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                IptvDialogActionButton(
                    text = "Open Feeds & Sources",
                    icon = Icons.Default.PlayArrow,
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    enabled = canInteract,
                    onClick = {
                        if (canInteract) {
                            onDismiss()
                            onOpen()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                IptvDialogActionButton(
                    text = "Remove Quick Channel",
                    icon = Icons.Default.Delete,
                    containerColor = Color(0xFF451A1A),
                    contentColor = Color(0xFFEF4444),
                    focusedBorderColor = Color(0xFFEF4444),
                    enabled = canInteract,
                    onClick = {
                        if (canInteract) {
                            onDismiss()
                            onDelete()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                IptvDialogActionButton(
                    text = "Cancel",
                    icon = null,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xAAFFFFFF),
                    focusedBorderColor = Color(0x44FFFFFF),
                    enabled = canInteract,
                    onClick = {
                        if (canInteract) onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun IptvDialogActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    containerColor: Color,
    contentColor: Color,
    focusedBorderColor: Color = Color(0xFF38BDF8),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = (isFocused || isHovered) && enabled

    val scale by animateFloatAsState(if (isHighlighted) 1.03f else 1.0f, label = "dlg_btn_scale")

    Surface(
        onClick = {
            if (enabled) onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = if (isHighlighted) containerColor.copy(alpha = 0.9f) else containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) focusedBorderColor else Color(0x1AFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
