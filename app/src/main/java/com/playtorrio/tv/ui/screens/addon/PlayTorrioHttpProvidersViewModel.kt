package com.playtorrio.tv.ui.screens.addon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playtorrio.tv.core.plugin.PluginManager
import com.playtorrio.tv.core.scraper.PlayTorrioHttpScraperManager
import com.playtorrio.tv.data.local.PlayTorrioHttpSettingsDataStore
import com.playtorrio.tv.domain.model.isBundledPhisher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayTorrioHttpProviderItem(
    val id: String,
    val displayName: String,
    val description: String?,
    val isBuiltIn: Boolean,
    val enabled: Boolean
)

data class PlayTorrioHttpProvidersUiState(
    val isLoading: Boolean = true,
    val isHttpEnabled: Boolean = true,
    val providers: List<PlayTorrioHttpProviderItem> = emptyList()
)

@HiltViewModel
class PlayTorrioHttpProvidersViewModel @Inject constructor(
    private val scraperManager: PlayTorrioHttpScraperManager,
    private val pluginManager: PluginManager,
    private val settingsDataStore: PlayTorrioHttpSettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayTorrioHttpProvidersUiState())
    val uiState: StateFlow<PlayTorrioHttpProvidersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                pluginManager.ensureBundledPhisherRepository()
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            combine(
                settingsDataStore.providerOrder,
                settingsDataStore.disabledProviders,
                settingsDataStore.isHttpScrapingEnabled,
                pluginManager.scrapers,
                pluginManager.repositories
            ) { savedOrder, disabledSet, isHttpEnabled, allPlugins, allRepos ->
                val reposById = allRepos.associateBy { it.id }
                val bundledPlugins = allPlugins.filter {
                    it.isBundledPhisher(reposById) || it.isBundled || it.type == com.playtorrio.tv.domain.model.RepositoryType.EXTERNAL_DEX
                }

                // 1. Built-in scrapers
                val builtInItems = scraperManager.allScrapers.map { s ->
                    PlayTorrioHttpProviderItem(
                        id = s.id,
                        displayName = s.displayName,
                        description = if (s.id == "111477") "111477 • High-Speed Direct CDN Stream" else "Built-in HTTP Scraper",
                        isBuiltIn = true,
                        enabled = s.id !in disabledSet
                    )
                }

                // 2. Bundled CloudStream extensions
                val pluginItems = bundledPlugins.map { p ->
                    val rawId = p.name.removeSuffix("Provider")
                    val isIStream = rawId.equals("IStreamFlare", ignoreCase = true) || rawId.equals("IStreamCDN", ignoreCase = true)
                    val providerId = if (isIStream) "IStreamCDN" else rawId
                    val displayName = if (isIStream) "IStreamCDN (IStreamFlare)" else rawId
                    val desc = if (isIStream) {
                        "Direct CDN • CloudStream Extension"
                    } else {
                        p.description.takeIf { it.isNotBlank() } ?: "CloudStream Extension"
                    }
                    PlayTorrioHttpProviderItem(
                        id = providerId,
                        displayName = displayName,
                        description = desc,
                        isBuiltIn = false,
                        enabled = providerId !in disabledSet && rawId !in disabledSet && p.id !in disabledSet
                    )
                }

                val allItems = (builtInItems + pluginItems).distinctBy { it.id }
                val allIds = allItems.map { it.id }

                // Normalize savedOrder (e.g. coalesce IStreamFlare into IStreamCDN)
                val normalizedSavedOrder = savedOrder.map {
                    if (it.equals("IStreamFlare", ignoreCase = true)) "IStreamCDN" else it
                }.distinct()

                // Determine effective ordering
                val effectiveOrder = if (normalizedSavedOrder.isEmpty()) {
                    val defaultOrder = settingsDataStore.generateDefaultOrder(allIds)
                    settingsDataStore.setProviderOrder(defaultOrder)
                    defaultOrder
                } else {
                    // Append any new providers that aren't yet in savedOrder
                    val missing = allIds.filter { it !in normalizedSavedOrder }
                    val merged = if (missing.isNotEmpty()) {
                        normalizedSavedOrder + missing
                    } else {
                        normalizedSavedOrder
                    }
                    if (merged != savedOrder) {
                        settingsDataStore.setProviderOrder(merged)
                    }
                    merged
                }

                val itemsById = allItems.associateBy { it.id }
                val orderedItems = effectiveOrder.mapNotNull { itemsById[it] } +
                    allItems.filter { it.id !in effectiveOrder }

                PlayTorrioHttpProvidersUiState(
                    isLoading = false,
                    isHttpEnabled = isHttpEnabled,
                    providers = orderedItems
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleMaster(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setHttpScrapingEnabled(enabled)
        }
    }

    fun toggleProvider(providerId: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setProviderEnabled(providerId, enabled)
            if (providerId.equals("IStreamCDN", ignoreCase = true)) {
                settingsDataStore.setProviderEnabled("IStreamFlare", enabled)
            } else if (providerId.equals("IStreamFlare", ignoreCase = true)) {
                settingsDataStore.setProviderEnabled("IStreamCDN", enabled)
            }
        }
    }

    fun moveUp(providerId: String) {
        viewModelScope.launch {
            val currentOrder = _uiState.value.providers.map { it.id }
            settingsDataStore.moveProviderUp(providerId, currentOrder)
        }
    }

    fun moveDown(providerId: String) {
        viewModelScope.launch {
            val currentOrder = _uiState.value.providers.map { it.id }
            settingsDataStore.moveProviderDown(providerId, currentOrder)
        }
    }

    fun resetToDefaultOrder() {
        viewModelScope.launch {
            val allIds = _uiState.value.providers.map { it.id }
            settingsDataStore.resetToDefaultOrder(allIds)
        }
    }
}
