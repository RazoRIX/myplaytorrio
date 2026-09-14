@file:OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.playtorrio.tv.ui.screens.addon

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.playtorrio.tv.ui.theme.PlayTorrioTheme

@Composable
fun PlayTorrioHttpProvidersScreen(
    onBackPress: () -> Unit,
    viewModel: PlayTorrioHttpProvidersViewModel = hiltViewModel()
) {
    BackHandler(onBack = onBackPress)
    val uiState by viewModel.uiState.collectAsState()

    val backButtonFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayTorrioTheme.colors.Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onBackPress,
                            modifier = Modifier
                                .size(40.dp)
                                .focusRequester(backButtonFocusRequester),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = PlayTorrioTheme.colors.BackgroundCard,
                                focusedContainerColor = PlayTorrioTheme.colors.FocusBackground
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = PlayTorrioTheme.focusRing.border(PlayTorrioTheme.spacing.xxs),
                                    shape = RoundedCornerShape(PlayTorrioTheme.radii.md)
                                )
                            ),
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(PlayTorrioTheme.radii.md)),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = PlayTorrioTheme.colors.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(PlayTorrioTheme.spacing.md))

                        Column {
                            Text(
                                text = "PlayTorrioHTTP Providers",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = PlayTorrioTheme.colors.TextPrimary
                            )
                            Text(
                                text = "Reorder and toggle providers. Pinned providers appear at top of results.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PlayTorrioTheme.colors.TextSecondary
                            )
                        }
                    }

                    // Reset to default button
                    Button(
                        onClick = { viewModel.resetToDefaultOrder() },
                        colors = ButtonDefaults.colors(
                            containerColor = PlayTorrioTheme.colors.BackgroundCard,
                            contentColor = PlayTorrioTheme.colors.TextSecondary,
                            focusedContainerColor = PlayTorrioTheme.colors.FocusBackground,
                            focusedContentColor = PlayTorrioTheme.colors.Primary
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(PlayTorrioTheme.radii.md))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Order", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Master Toggle Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = PlayTorrioTheme.colors.BackgroundCard),
                    shape = RoundedCornerShape(PlayTorrioTheme.radii.md)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable PlayTorrioHTTP",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PlayTorrioTheme.colors.TextPrimary
                            )
                            Text(
                                text = "Scrape HTTP streaming links from built-in and bundled sources",
                                style = MaterialTheme.typography.bodySmall,
                                color = PlayTorrioTheme.colors.TextSecondary
                            )
                        }

                        Surface(
                            onClick = { viewModel.toggleMaster(!uiState.isHttpEnabled) },
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = PlayTorrioTheme.colors.FocusBackground
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = PlayTorrioTheme.focusRing.border(PlayTorrioTheme.spacing.xxs),
                                    shape = RoundedCornerShape(PlayTorrioTheme.radii.md)
                                )
                            ),
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(PlayTorrioTheme.radii.md)),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Switch(
                                    checked = uiState.isHttpEnabled,
                                    onCheckedChange = null,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PlayTorrioTheme.colors.Secondary,
                                        checkedTrackColor = PlayTorrioTheme.colors.Secondary.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Providers Section Header
            item {
                Text(
                    text = "Providers Priority Order ()",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PlayTorrioTheme.colors.TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Providers List
            itemsIndexed(
                items = uiState.providers,
                key = { _, provider -> provider.id }
            ) { index, provider ->
                val toggleFocusRequester = remember { FocusRequester() }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .focusProperties { enter = { toggleFocusRequester } },
                    colors = CardDefaults.cardColors(containerColor = PlayTorrioTheme.colors.BackgroundCard),
                    shape = RoundedCornerShape(PlayTorrioTheme.radii.md)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Rank Badge
                            val isTopRank = index < 2
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isTopRank) PlayTorrioTheme.colors.Secondary.copy(alpha = 0.2f)
                                        else PlayTorrioTheme.colors.Background,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#",
                                    color = if (isTopRank) PlayTorrioTheme.colors.Secondary else PlayTorrioTheme.colors.TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PlayTorrioTheme.colors.TextPrimary
                                    )
                                    if (isTopRank) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(PlayTorrioTheme.colors.Secondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "PINNED",
                                                color = PlayTorrioTheme.colors.Secondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!provider.description.isNullOrBlank()) {
                                        Text(
                                            text = provider.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PlayTorrioTheme.colors.TextSecondary
                                        )
                                    }
                                    if (!provider.enabled) {
                                        Text(
                                            text = "• Disabled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }

                        // Actions: Toggle, Move Up, Move Down
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PlayTorrioTheme.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Switch
                            Surface(
                                onClick = { viewModel.toggleProvider(provider.id, !provider.enabled) },
                                modifier = Modifier.focusRequester(toggleFocusRequester),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.Transparent,
                                    focusedContainerColor = PlayTorrioTheme.colors.FocusBackground
                                ),
                                border = ClickableSurfaceDefaults.border(
                                    focusedBorder = Border(
                                        border = PlayTorrioTheme.focusRing.border(PlayTorrioTheme.spacing.xxs),
                                        shape = RoundedCornerShape(PlayTorrioTheme.radii.md)
                                    )
                                ),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(PlayTorrioTheme.radii.md)),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Switch(
                                        checked = provider.enabled,
                                        onCheckedChange = null,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PlayTorrioTheme.colors.Secondary,
                                            checkedTrackColor = PlayTorrioTheme.colors.Secondary.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }

                            // Move Up
                            Button(
                                onClick = { viewModel.moveUp(provider.id) },
                                enabled = index > 0,
                                colors = ButtonDefaults.colors(
                                    containerColor = PlayTorrioTheme.colors.BackgroundCard,
                                    contentColor = PlayTorrioTheme.colors.TextSecondary,
                                    focusedContainerColor = PlayTorrioTheme.colors.FocusBackground,
                                    focusedContentColor = PlayTorrioTheme.colors.Primary
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(PlayTorrioTheme.radii.md))
                            ) {
                                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move Up")
                            }

                            // Move Down
                            Button(
                                onClick = { viewModel.moveDown(provider.id) },
                                enabled = index < uiState.providers.lastIndex,
                                colors = ButtonDefaults.colors(
                                    containerColor = PlayTorrioTheme.colors.BackgroundCard,
                                    contentColor = PlayTorrioTheme.colors.TextSecondary,
                                    focusedContainerColor = PlayTorrioTheme.colors.FocusBackground,
                                    focusedContentColor = PlayTorrioTheme.colors.Primary
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(PlayTorrioTheme.radii.md))
                            ) {
                                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move Down")
                            }
                        }
                    }
                }
            }
        }
    }
}
