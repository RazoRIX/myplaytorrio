package com.playtorrio.tv.ui.screens.iptv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playtorrio.tv.core.iptv.channels.HardcodedChannel
import com.playtorrio.tv.core.iptv.channels.HardcodedChannels
import com.playtorrio.tv.core.iptv.context.IptvChannelContextHolder
import com.playtorrio.tv.core.iptv.model.CatalogSource
import com.playtorrio.tv.core.iptv.model.M3uPlaylist

@Composable
fun IptvActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color,
    focusedContainerColor: Color = containerColor.copy(alpha = 0.85f),
    contentColor: Color = Color.White,
    focusedBorderColor: Color = Color(0xFF38BDF8),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = (isFocused || isHovered) && enabled

    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.06f else 1.0f,
        label = "btn_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isHighlighted) focusedContainerColor else containerColor,
        label = "btn_bg"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) focusedBorderColor else Color(0x22FFFFFF)
        ),
        modifier = modifier
            .scale(scale)
            .height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
fun IptvHomeScreen(
    viewModel: IptvViewModel,
    onOpenPortals: () -> Unit,
    onPlayStream: (streamUrl: String, title: String) -> Unit
) {
    val isScraping by viewModel.isScraping.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val verifiedPortals by viewModel.verifiedPortals.collectAsState()
    val canGetMore by viewModel.canGetMore.collectAsState()
    val scrapeSource by viewModel.scrapeSource.collectAsState()
    val quickChannels by viewModel.quickChannels.collectAsState()
    val m3uPlaylists by viewModel.m3uPlaylists.collectAsState()

    var selectedChannelForSources by remember { mutableStateOf<HardcodedChannel?>(null) }
    var showQuickChannelDialog by remember { mutableStateOf(false) }
    var showM3uDialog by remember { mutableStateOf(false) }
    var selectedM3uForBrowsing by remember { mutableStateOf<M3uPlaylist?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPortalsFromStorage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val featuredChannels = remember {
        listOfNotNull(
            HardcodedChannels.byId("ufc"),
            HardcodedChannels.byId("champions_league"),
            HardcodedChannels.byId("espn_plus"),
            HardcodedChannels.byId("f1"),
            HardcodedChannels.byId("nba"),
            HardcodedChannels.byId("bein_1")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Top Action Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = "PlayTorrio Live IPTV",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isScraping) statusText else "Community Live Portals (${verifiedPortals.size} active)",
                            color = if (isScraping) Color(0xFF38BDF8) else Color(0x88FFFFFF),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Portals Manager Button
                        IptvActionButton(
                            onClick = onOpenPortals,
                            containerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF38BDF8)
                        ) {
                            Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Portals (${verifiedPortals.size})", fontSize = 12.sp, maxLines = 1)
                        }

                        // M3U Playlists Button
                        IptvActionButton(
                            onClick = { showM3uDialog = true },
                            containerColor = Color(0xFF0F231D),
                            focusedContainerColor = Color(0xFF14382C),
                            contentColor = Color(0xFF10B981),
                            focusedBorderColor = Color(0xFF10B981)
                        ) {
                            Icon(imageVector = Icons.Default.FeaturedPlayList, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("M3U (${m3uPlaylists.size})", color = Color(0xFF10B981), fontSize = 12.sp, maxLines = 1)
                        }

                        // Add Quick Channel Button
                        IptvActionButton(
                            onClick = { showQuickChannelDialog = true },
                            containerColor = Color(0xFF312E81),
                            focusedContainerColor = Color(0xFF3730A3),
                            contentColor = Color(0xFFA5B4FC),
                            focusedBorderColor = Color(0xFFA5B4FC)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Quick", color = Color(0xFFA5B4FC), fontSize = 12.sp, maxLines = 1)
                        }

                        // Source Switcher
                        IptvActionButton(
                            onClick = { viewModel.toggleScrapeSource() },
                            enabled = !isScraping,
                            containerColor = if (scrapeSource == CatalogSource.CLOUD_VAULT) Color(0xFF4338CA) else Color(0xFFC2410C),
                            focusedBorderColor = if (scrapeSource == CatalogSource.CLOUD_VAULT) Color(0xFF818CF8) else Color(0xFFFB923C)
                        ) {
                            Text(scrapeSource.label, fontSize = 11.sp, maxLines = 1)
                        }

                        // Get More Button
                        if (canGetMore && !isScraping) {
                            IptvActionButton(
                                onClick = { viewModel.getMorePortals() },
                                containerColor = Color(0xFF059669),
                                focusedBorderColor = Color(0xFF34D399)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Get More", fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        // Scrape Button
                        IptvActionButton(
                            onClick = { viewModel.scrapePortals(reset = true) },
                            enabled = !isScraping,
                            containerColor = Color(0xFF0284C7),
                            focusedBorderColor = Color(0xFF38BDF8)
                        ) {
                            if (isScraping) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isScraping) "Scraping..." else "Scrape Portals", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            // 1. Spotlight Hero Carousel
            item {
                SpotlightHeroCarousel(
                    featuredChannels = featuredChannels,
                    onWatchNow = { channel ->
                        viewModel.openChannel(channel)
                        selectedChannelForSources = channel
                    },
                    onSourcesTap = { channel ->
                        viewModel.openChannel(channel)
                        selectedChannelForSources = channel
                    }
                )
            }

            // 2. Quick Channels Rail (Custom shortcuts)
            if (quickChannels.isNotEmpty()) {
                item(key = "quick_channels_row") {
                    QuickChannelsRow(
                        channels = quickChannels,
                        onChannelClick = { quick ->
                            viewModel.openQuickChannel(quick)
                            selectedChannelForSources = HardcodedChannel(
                                id = quick.id,
                                name = quick.name,
                                short = quick.short,
                                category = quick.category,
                                keywords = quick.keywords,
                                gradient = quick.gradient,
                                iconUrl = quick.iconUrl
                            )
                        },
                        onAddClick = { showQuickChannelDialog = true },
                        onDeleteClick = { quick -> viewModel.removeQuickChannel(quick.id) }
                    )
                }
            }

            // 3. M3U Playlists Rail (Custom playlists)
            if (m3uPlaylists.isNotEmpty()) {
                item(key = "m3u_playlists_row") {
                    M3uPlaylistsRow(
                        playlists = m3uPlaylists,
                        onPlaylistClick = { playlist -> selectedM3uForBrowsing = playlist },
                        onManageClick = { showM3uDialog = true }
                    )
                }
            }

            // 4. Curated Slider Sections (All 14 categories from HardcodedChannels)
            items(
                count = HardcodedChannels.categories.size,
                key = { idx -> "cat_${HardcodedChannels.categories[idx]}" }
            ) { idx ->
                val cat = HardcodedChannels.categories[idx]
                val channels = remember(cat) { HardcodedChannels.byCategory(cat) }
                if (channels.isNotEmpty()) {
                    IptvCategoryRow(
                        title = HardcodedChannels.getCategoryDisplayTitle(cat),
                        subtitle = HardcodedChannels.getCategorySubtitle(cat),
                        channels = channels,
                        onChannelClick = { channel ->
                            viewModel.openChannel(channel)
                            selectedChannelForSources = channel
                        }
                    )
                }
            }
        }

        // Live Channel Sources Dialog
        selectedChannelForSources?.let { channel ->
            IptvChannelSourcesDialog(
                viewModel = viewModel,
                channel = channel,
                onDismiss = { selectedChannelForSources = null },
                onPlayHit = { hit ->
                    selectedChannelForSources = null
                    IptvChannelContextHolder.setHardcodedContext(
                        channel = channel,
                        hits = viewModel.channelHits.value,
                        currentStreamUrl = hit.streamUrl,
                        allChannels = HardcodedChannels.all
                    )
                    onPlayStream(hit.streamUrl, "${channel.name} (${hit.portal.name})")
                }
            )
        }

        // Quick Channel Creator Dialog
        if (showQuickChannelDialog) {
            IptvQuickChannelDialog(
                onDismiss = { showQuickChannelDialog = false },
                onAddChannel = { name, query, icon, color ->
                    viewModel.addQuickChannel(name, query, icon, color)
                }
            )
        }

        // M3U Manager Dialog
        if (showM3uDialog) {
            IptvM3uDialog(
                viewModel = viewModel,
                onDismiss = { showM3uDialog = false },
                onBrowsePlaylist = { playlist ->
                    showM3uDialog = false
                    selectedM3uForBrowsing = playlist
                }
            )
        }

        // M3U Channel Browser Dialog
        selectedM3uForBrowsing?.let { playlist ->
            IptvM3uBrowserDialog(
                viewModel = viewModel,
                playlist = playlist,
                onDismiss = { selectedM3uForBrowsing = null },
                onPlayChannel = { url, title ->
                    selectedM3uForBrowsing = null
                    onPlayStream(url, title)
                }
            )
        }
    }
}
