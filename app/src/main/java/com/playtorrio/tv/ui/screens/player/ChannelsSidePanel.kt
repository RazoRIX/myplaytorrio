package com.playtorrio.tv.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.playtorrio.tv.core.iptv.channels.HardcodedChannel
import com.playtorrio.tv.core.iptv.channels.HardcodedChannels
import com.playtorrio.tv.core.iptv.context.IptvChannelContextHolder
import com.playtorrio.tv.core.iptv.context.IptvLiveContext
import com.playtorrio.tv.core.iptv.model.ChannelHit
import com.playtorrio.tv.core.iptv.model.IptvStream
import com.playtorrio.tv.core.iptv.network.IptvAliveChecker
import com.playtorrio.tv.core.iptv.network.IptvClient
import com.playtorrio.tv.core.iptv.storage.IptvStorage
import com.playtorrio.tv.ui.screens.detail.requestFocusAfterFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChannelsSidePanel(
    onClose: () -> Unit,
    onSwitchChannel: (streamUrl: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveContext by IptvChannelContextHolder.currentContext.collectAsState()
    val context = LocalContext.current
    val storage = remember { IptvStorage(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedHardcodedCategory by remember { mutableStateOf("All") }
    var resolvingChannelId by remember { mutableStateOf<String?>(null) }
    var hardcodedTab by remember { mutableStateOf("Feeds") }
    var hardcodedHits by remember(liveContext) {
        mutableStateOf((liveContext as? IptvLiveContext.Hardcoded)?.hits.orEmpty())
    }

    LaunchedEffect(liveContext) {
        val hCtx = liveContext as? IptvLiveContext.Hardcoded ?: return@LaunchedEffect
        if (hCtx.hits.isNotEmpty()) {
            hardcodedHits = hCtx.hits
        } else if (hCtx.currentChannelId.isNotEmpty()) {
            val cached = storage.loadChannelHits(hCtx.currentChannelId)
            if (cached.isNotEmpty()) {
                hardcodedHits = cached
            }
        }
    }

    val listState = rememberLazyListState()
    val initialFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .width(480.dp)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF090D16))
                )
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        val title = when (val ctx = liveContext) {
                            is IptvLiveContext.Portal -> ctx.categoryName.ifEmpty { ctx.portal.name }
                            is IptvLiveContext.Hardcoded -> ctx.channel?.name ?: "Channel Feeds"
                            null -> "Channels"
                        }
                        val subtitle = when (val ctx = liveContext) {
                            is IptvLiveContext.Portal -> "${ctx.portal.name} · ${ctx.channels.size} channels"
                            is IptvLiveContext.Hardcoded -> if (hardcodedTab == "Feeds") "${hardcodedHits.size} alternate feeds found" else "${ctx.allChannels.size} channels available"
                            null -> ""
                        }
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                color = Color(0x88FFFFFF),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    val ph = if (liveContext is IptvLiveContext.Hardcoded && hardcodedTab == "Feeds") "Filter feeds…" else "Filter channels…"
                    Text(ph, color = Color(0x66FFFFFF), fontSize = 13.sp)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF111827),
                    focusedIndicatorColor = Color(0xFF38BDF8),
                    unfocusedIndicatorColor = Color(0x22FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Category Filter / Tabs for Hardcoded Channels
            if (liveContext is IptvLiveContext.Hardcoded) {
                val ctx = liveContext as IptvLiveContext.Hardcoded
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { hardcodedTab = "Feeds" },
                        color = if (hardcodedTab == "Feeds") Color(0xFF0284C7) else Color(0xFF1E293B)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Feeds (${hardcodedHits.size})",
                                color = if (hardcodedTab == "Feeds") Color.White else Color(0xAAFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { hardcodedTab = "All Channels" },
                        color = if (hardcodedTab == "All Channels") Color(0xFF0284C7) else Color(0xFF1E293B)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "All Channels",
                                color = if (hardcodedTab == "All Channels") Color.White else Color(0xAAFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (hardcodedTab == "All Channels") {
                    Spacer(modifier = Modifier.height(8.dp))
                    val categories = remember { HardcodedChannels.categories }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isAll = selectedHardcodedCategory == "All"
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedHardcodedCategory = "All" },
                                color = if (isAll) Color(0xFF0284C7) else Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "All",
                                    color = if (isAll) Color.White else Color(0xAAFFFFFF),
                                    fontSize = 11.sp,
                                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        items(categories) { cat ->
                            val isSelected = cat == selectedHardcodedCategory
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedHardcodedCategory = cat },
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.White else Color(0xAAFFFFFF),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Channels List
            when (val ctx = liveContext) {
                is IptvLiveContext.Portal -> {
                    val filtered = remember(ctx.channels, searchQuery) {
                        if (searchQuery.isBlank()) ctx.channels else {
                            ctx.channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                    }

                    LaunchedEffect(Unit) {
                        val activeIdx = filtered.indexOfFirst { it.streamId == ctx.currentStreamId }
                        if (activeIdx >= 0) {
                            listState.scrollToItem(activeIdx)
                            initialFocusRequester.requestFocusAfterFrames(2)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.streamId }) { stream ->
                            val isCurrent = stream.streamId == ctx.currentStreamId
                            PortalChannelRow(
                                stream = stream,
                                isCurrent = isCurrent,
                                focusRequester = if (isCurrent) initialFocusRequester else null,
                                onClick = {
                                    IptvChannelContextHolder.updateCurrentStreamId(stream.streamId)
                                    val streamUrl = IptvClient.streamUrl(ctx.portal.portal, stream)
                                    onSwitchChannel(streamUrl, stream.name)
                                }
                            )
                        }
                    }
                }

                is IptvLiveContext.Hardcoded -> {
                    if (hardcodedTab == "Feeds") {
                        val filteredFeeds = remember(hardcodedHits, searchQuery) {
                            if (searchQuery.isBlank()) hardcodedHits else {
                                hardcodedHits.filter {
                                    it.stream.name.contains(searchQuery, ignoreCase = true) ||
                                    it.portal.name.contains(searchQuery, ignoreCase = true)
                                }
                            }
                        }

                        LaunchedEffect(hardcodedHits) {
                            val activeIdx = filteredFeeds.indexOfFirst { it.streamUrl == ctx.currentStreamUrl }
                            if (activeIdx >= 0) {
                                listState.scrollToItem(activeIdx)
                                initialFocusRequester.requestFocusAfterFrames(2)
                            }
                        }

                        if (filteredFeeds.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No feeds match '$searchQuery'" else "No saved feeds for ${ctx.channel?.name ?: "this channel"}",
                                        color = Color(0xAAFFFFFF),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        onClick = { hardcodedTab = "All Channels" },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0284C7)
                                    ) {
                                        Text(
                                            text = "Browse All Channels",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredFeeds, key = { it.streamUrl }) { hit ->
                                    val isCurrent = hit.streamUrl == ctx.currentStreamUrl
                                    HardcodedHitRow(
                                        hit = hit,
                                        isCurrent = isCurrent,
                                        focusRequester = if (isCurrent) initialFocusRequester else null,
                                        onClick = {
                                            IptvChannelContextHolder.updateCurrentStreamUrl(hit.streamUrl)
                                            onSwitchChannel(hit.streamUrl, "${ctx.channel?.name ?: hit.stream.name} (${hit.portal.name})")
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // All Channels Directory
                        val filtered = remember(ctx.allChannels, searchQuery, selectedHardcodedCategory) {
                            var list = ctx.allChannels
                            if (selectedHardcodedCategory != "All") {
                                list = list.filter { it.category.equals(selectedHardcodedCategory, ignoreCase = true) }
                            }
                            if (searchQuery.isNotBlank()) {
                                list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            }
                            list
                        }

                        LaunchedEffect(Unit) {
                            val activeIdx = filtered.indexOfFirst { it.id == ctx.currentChannelId }
                            if (activeIdx >= 0) {
                                listState.scrollToItem(activeIdx)
                                initialFocusRequester.requestFocusAfterFrames(2)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered, key = { it.id }) { channel ->
                                val isCurrent = channel.id == ctx.currentChannelId
                                val isResolving = resolvingChannelId == channel.id
                                HardcodedChannelRow(
                                    channel = channel,
                                    isCurrent = isCurrent,
                                    isResolving = isResolving,
                                    focusRequester = if (isCurrent) initialFocusRequester else null,
                                    onClick = {
                                        if (isResolving) return@HardcodedChannelRow
                                        scope.launch {
                                            resolvingChannelId = channel.id
                                            val cachedHits = storage.loadChannelHits(channel.id)
                                            if (cachedHits.isNotEmpty()) {
                                                val hit = cachedHits.first()
                                                hardcodedHits = cachedHits
                                                IptvChannelContextHolder.setHardcodedContext(
                                                    channel = channel,
                                                    hits = cachedHits,
                                                    currentStreamUrl = hit.streamUrl,
                                                    allChannels = ctx.allChannels
                                                )
                                                onSwitchChannel(hit.streamUrl, "${channel.name} (${hit.portal.name})")
                                                resolvingChannelId = null
                                                hardcodedTab = "Feeds"
                                                return@launch
                                            }

                                            // Fallback: quickly check first 5 verified portals
                                            val portals = storage.loadVerifiedPortals().take(5)
                                            var foundUrl: String? = null
                                            var portalName: String = ""
                                            withContext(Dispatchers.IO) {
                                                for (p in portals) {
                                                    try {
                                                        val matches = IptvClient.searchChannelInPortal(p.portal, channel)
                                                        for (m in matches) {
                                                            val u = IptvClient.streamUrl(p.portal, m)
                                                            if (u.isNotEmpty() && IptvAliveChecker.isAlive(u)) {
                                                                foundUrl = u
                                                                portalName = p.name
                                                                break
                                                            }
                                                        }
                                                        if (foundUrl != null) break
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                            resolvingChannelId = null
                                            if (foundUrl != null) {
                                                IptvChannelContextHolder.setHardcodedContext(
                                                    channel = channel,
                                                    hits = emptyList(),
                                                    currentStreamUrl = foundUrl!!,
                                                    allChannels = ctx.allChannels
                                                )
                                                onSwitchChannel(foundUrl!!, "${channel.name} ($portalName)")
                                                hardcodedTab = "Feeds"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels loaded", color = Color(0x66FFFFFF), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalChannelRow(
    stream: IptvStream,
    isCurrent: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered
    val scale by animateFloatAsState(if (isHighlighted) 1.02f else 1.0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Color(0xFF1E293B) else if (isCurrent) Color(0xFF0F2236) else Color(0xFF111827)
        ),
        border = if (isHighlighted) BorderStroke(2.dp, Color(0xFF38BDF8)) else if (isCurrent) BorderStroke(1.dp, Color(0xFF0284C7)) else BorderStroke(1.dp, Color(0x11FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stream.icon.isNotBlank()) {
                AsyncImage(
                    model = stream.icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x2238BDF8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stream.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (stream.containerExt.isNotBlank()) {
                    Text(
                        text = stream.containerExt.uppercase(),
                        color = Color(0x66FFFFFF),
                        fontSize = 10.sp
                    )
                }
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0284C7))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PLAYING",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HardcodedHitRow(
    hit: ChannelHit,
    isCurrent: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered
    val scale by animateFloatAsState(if (isHighlighted) 1.02f else 1.0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Color(0xFF1E293B) else if (isCurrent) Color(0xFF0F2236) else Color(0xFF111827)
        ),
        border = if (isHighlighted) BorderStroke(2.dp, Color(0xFF38BDF8)) else if (isCurrent) BorderStroke(1.dp, Color(0xFF0284C7)) else BorderStroke(1.dp, Color(0x11FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x2238BDF8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.stream.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = hit.portal.name,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0284C7))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PLAYING",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HardcodedChannelRow(
    channel: HardcodedChannel,
    isCurrent: Boolean,
    isResolving: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered
    val scale by animateFloatAsState(if (isHighlighted) 1.02f else 1.0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Color(0xFF1E293B) else if (isCurrent) Color(0xFF0F2236) else Color(0xFF111827)
        ),
        border = if (isHighlighted) BorderStroke(2.dp, Color(0xFF38BDF8)) else if (isCurrent) BorderStroke(1.dp, Color(0xFF0284C7)) else BorderStroke(1.dp, Color(0x11FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x2238BDF8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.category,
                    color = Color(0x66FFFFFF),
                    fontSize = 10.sp
                )
            }

            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF38BDF8),
                    strokeWidth = 2.dp
                )
            } else if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0284C7))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PLAYING",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
