package com.playtorrio.tv.ui.screens.iptv

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playtorrio.tv.core.iptv.channels.HardcodedChannel
import com.playtorrio.tv.core.iptv.channels.HardcodedChannels
import com.playtorrio.tv.core.iptv.model.AliveProgress
import com.playtorrio.tv.core.iptv.model.CatalogSource
import com.playtorrio.tv.core.iptv.model.ChannelHit
import com.playtorrio.tv.core.iptv.model.EpgEntry
import com.playtorrio.tv.core.iptv.model.IptvCategory
import com.playtorrio.tv.core.iptv.model.IptvEpisode
import com.playtorrio.tv.core.iptv.model.IptvPortal
import androidx.compose.ui.graphics.Color
import com.playtorrio.tv.core.iptv.model.IptvSection
import com.playtorrio.tv.core.iptv.model.IptvStream
import com.playtorrio.tv.core.iptv.model.M3uChannel
import com.playtorrio.tv.core.iptv.model.M3uPlaylist
import com.playtorrio.tv.core.iptv.model.QuickChannel
import com.playtorrio.tv.core.iptv.model.ScrapePage
import com.playtorrio.tv.core.iptv.model.VerifiedPortal
import com.playtorrio.tv.core.iptv.network.IptvAliveChecker
import com.playtorrio.tv.core.iptv.network.IptvClient
import com.playtorrio.tv.core.iptv.network.IptvScraper
import com.playtorrio.tv.core.iptv.network.IptvVerifier
import com.playtorrio.tv.core.iptv.network.M3uParser
import com.playtorrio.tv.core.iptv.storage.IptvStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class IptvViewModel @Inject constructor(
    private val storage: IptvStorage
) : ViewModel() {
    companion object {
        private const val TAG = "IptvViewModel"
    }

    private val _scrapeSource = MutableStateFlow(storage.loadScrapeSource())
    val scrapeSource: StateFlow<CatalogSource> = _scrapeSource.asStateFlow()

    fun setScrapeSource(source: CatalogSource) {
        _scrapeSource.value = source
        storage.saveScrapeSource(source)
    }

    fun toggleScrapeSource() {
        val next = if (_scrapeSource.value == CatalogSource.CLOUD_VAULT) CatalogSource.REDDIT else CatalogSource.CLOUD_VAULT
        setScrapeSource(next)
    }

    private val _isScraping = MutableStateFlow(false)
    val isScraping: StateFlow<Boolean> = _isScraping.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _verifiedPortals = MutableStateFlow<List<VerifiedPortal>>(emptyList())
    val verifiedPortals: StateFlow<List<VerifiedPortal>> = _verifiedPortals.asStateFlow()

    private val _favoritePortalKeys = MutableStateFlow<Set<String>>(emptySet())
    val favoritePortalKeys: StateFlow<Set<String>> = _favoritePortalKeys.asStateFlow()

    private val _m3uPlaylists = MutableStateFlow<List<M3uPlaylist>>(emptyList())
    val m3uPlaylists: StateFlow<List<M3uPlaylist>> = _m3uPlaylists.asStateFlow()

    private val _quickChannels = MutableStateFlow<List<QuickChannel>>(emptyList())
    val quickChannels: StateFlow<List<QuickChannel>> = _quickChannels.asStateFlow()

    private val _currentChannelFavorites = MutableStateFlow<Set<String>>(emptySet())
    val currentChannelFavorites: StateFlow<Set<String>> = _currentChannelFavorites.asStateFlow()

    // ── Channel Scan State ──
    private val _activeHardcoded = MutableStateFlow<HardcodedChannel?>(null)
    val activeHardcoded: StateFlow<HardcodedChannel?> = _activeHardcoded.asStateFlow()

    private val _channelHits = MutableStateFlow<List<ChannelHit>>(emptyList())
    val channelHits: StateFlow<List<ChannelHit>> = _channelHits.asStateFlow()

    private val _isScanningChannel = MutableStateFlow(false)
    val isScanningChannel: StateFlow<Boolean> = _isScanningChannel.asStateFlow()

    private val _channelScanStatus = MutableStateFlow("")
    val channelScanStatus: StateFlow<String> = _channelScanStatus.asStateFlow()

    private var channelScanJob: Job? = null
    private var scrapeJob: Job? = null

    // ── Portal Browser State ──
    private val _browserCategories = MutableStateFlow<List<IptvCategory>>(emptyList())
    val browserCategories: StateFlow<List<IptvCategory>> = _browserCategories.asStateFlow()

    private val _browserStreams = MutableStateFlow<List<IptvStream>>(emptyList())
    val browserStreams: StateFlow<List<IptvStream>> = _browserStreams.asStateFlow()

    private val _isBrowserLoading = MutableStateFlow(false)
    val isBrowserLoading: StateFlow<Boolean> = _isBrowserLoading.asStateFlow()

    private val _browserSelectedCategoryId = MutableStateFlow("")
    val browserSelectedCategoryId: StateFlow<String> = _browserSelectedCategoryId.asStateFlow()

    private val _lastPlayedStreamId = MutableStateFlow<String?>(null)
    val lastPlayedStreamId: StateFlow<String?> = _lastPlayedStreamId.asStateFlow()

    fun setLastPlayedStreamId(id: String?) {
        _lastPlayedStreamId.value = id
    }

    private val _browserAliveIds = MutableStateFlow<Set<String>>(emptySet())
    val browserAliveIds: StateFlow<Set<String>> = _browserAliveIds.asStateFlow()

    private val _isAliveChecking = MutableStateFlow(false)
    val isAliveChecking: StateFlow<Boolean> = _isAliveChecking.asStateFlow()

    private val _aliveProgress = MutableStateFlow(AliveProgress(0, 0, 0))
    val aliveProgress: StateFlow<AliveProgress> = _aliveProgress.asStateFlow()

    private val _epgMap = ConcurrentHashMap<String, List<EpgEntry>>()
    private val _streamUrlCache = ConcurrentHashMap<String, String>()

    private var scrapeAfter: String? = null
    private val verifiedKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val _attemptedKeys = mutableSetOf<String>()
    private val _pendingPortals = mutableListOf<IptvPortal>()
    private val _pendingKeys = mutableSetOf<String>()
    private val _channelAttemptedPortals = ConcurrentHashMap<String, MutableSet<String>>()
    private var _canGetMore = MutableStateFlow(false)
    val canGetMore: StateFlow<Boolean> = _canGetMore.asStateFlow()
    private val scrapeMutex = Mutex()

    init {
        viewModelScope.launch {
            val stored = storage.loadVerifiedPortals()
            val favs = storage.loadFavoritePortalKeys()
            _favoritePortalKeys.value = favs
            _verifiedPortals.value = sortFavoritesFirst(stored, favs)
            verifiedKeys.addAll(stored.map { it.credKey })
            _m3uPlaylists.value = storage.loadM3uPlaylists()
            _quickChannels.value = storage.loadQuickChannels()

            val cachedCandidates = storage.loadCandidatePortals()
            _pendingPortals.addAll(cachedCandidates.filter { !verifiedKeys.contains(it.credKey) })
            _pendingKeys.addAll(_pendingPortals.map { it.credKey })
            _canGetMore.value = _pendingPortals.isNotEmpty()

            // Auto-scrape if no portals available
            if (stored.isEmpty()) {
                scrapePortals(reset = true)
            }
        }
    }

    private fun sortFavoritesFirst(list: List<VerifiedPortal>, favs: Set<String>): List<VerifiedPortal> {
        val f = list.filter { favs.contains(it.key) }
        val r = list.filter { !favs.contains(it.key) }
        return f + r
    }

    fun toggleFavoritePortal(key: String) {
        viewModelScope.launch {
            val favs = _favoritePortalKeys.value.toMutableSet()
            if (favs.contains(key)) favs.remove(key) else favs.add(key)
            _favoritePortalKeys.value = favs
            storage.saveFavoritePortalKeys(favs)
            _verifiedPortals.value = sortFavoritesFirst(_verifiedPortals.value, favs)
        }
    }

    fun deletePortal(portal: VerifiedPortal) {
        deletePortals(setOf(portal.key, portal.credKey))
    }

    fun deletePortals(keys: Set<String>) {
        if (keys.isEmpty()) return
        viewModelScope.launch {
            val toDelete = _verifiedPortals.value.filter { keys.contains(it.key) || keys.contains(it.credKey) }
            val toDeleteKeys = toDelete.map { it.key }.toSet()
            val toDeleteCredKeys = toDelete.map { it.credKey }.toSet()
            val allDeleteKeys = toDeleteKeys + toDeleteCredKeys + keys

            val list = _verifiedPortals.value.filter {
                !allDeleteKeys.contains(it.key) && !allDeleteKeys.contains(it.credKey)
            }
            _verifiedPortals.value = list
            toDeleteCredKeys.forEach { verifiedKeys.remove(it) }
            storage.saveVerifiedPortals(list)

            // Remove alive cache and persisted channel hits for deleted portals
            allDeleteKeys.forEach { storage.removeAliveCache(it) }
            storage.removeHitsForPortals(allDeleteKeys)

            // Immediately purge active channel hits belonging to deleted portals
            _channelHits.value = _channelHits.value.filter { hit ->
                !allDeleteKeys.contains(hit.portal.key) && !allDeleteKeys.contains(hit.portal.credKey)
            }

            _canGetMore.value = _pendingPortals.isNotEmpty() || _verifiedPortals.value.isNotEmpty()
        }
    }

    fun deleteAllPortals() {
        viewModelScope.launch {
            val allKeys = _verifiedPortals.value.flatMap { listOf(it.key, it.credKey) }.toSet()
            _verifiedPortals.value = emptyList()
            verifiedKeys.clear()
            storage.saveVerifiedPortals(emptyList())
            storage.removeHitsForPortals(allKeys)
            _channelHits.value = emptyList()
            _canGetMore.value = _pendingPortals.isNotEmpty()
        }
    }

    fun refreshPortalsFromStorage() {
        viewModelScope.launch {
            val stored = storage.loadVerifiedPortals()
            val favs = storage.loadFavoritePortalKeys()
            _favoritePortalKeys.value = favs
            _verifiedPortals.value = sortFavoritesFirst(stored, favs)
            verifiedKeys.clear()
            verifiedKeys.addAll(stored.map { it.credKey })
            _canGetMore.value = _pendingPortals.isNotEmpty() || _verifiedPortals.value.isNotEmpty()
        }
    }

    fun deleteChannelHits(channelId: String, streamUrls: Set<String>) {
        if (streamUrls.isEmpty()) return
        viewModelScope.launch {
            val remaining = _channelHits.value.filter { !streamUrls.contains(it.streamUrl) }
            _channelHits.value = remaining
            storage.saveChannelHits(channelId, remaining)
        }
    }

    fun clearAllChannelHits(channelId: String) {
        viewModelScope.launch {
            _channelHits.value = emptyList()
            storage.clearChannelHits(channelId)
        }
    }

    fun addManualPortal(url: String, user: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val p = IptvPortal(url = url.trim(), username = user.trim(), password = pass.trim(), source = "Manual")
            val v = IptvClient.verifyOrNull(p, 8)
            if (v != null) {
                val list = _verifiedPortals.value.toMutableList()
                list.removeAll { it.credKey == v.credKey }
                list.add(0, v)
                _verifiedPortals.value = sortFavoritesFirst(list, _favoritePortalKeys.value)
                verifiedKeys.add(v.credKey)
                storage.saveVerifiedPortals(list)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun scrapePortals(reset: Boolean = false) {
        if (_isScraping.value) return
        scrapeJob?.cancel()
        scrapeJob = viewModelScope.launch {
            _isScraping.value = true
            _canGetMore.value = false
            _statusText.value = "Finding live portals…"

            if (reset) {
                scrapeAfter = null
                _pendingPortals.clear()
                _pendingKeys.clear()
                _attemptedKeys.clear()
                storage.saveCandidatePortals(emptyList())
            }

            scrapeAndVerify()
        }
    }

    fun getMorePortals() {
        if (_isScraping.value) return
        scrapeJob?.cancel()
        scrapeJob = viewModelScope.launch {
            _isScraping.value = true
            _statusText.value = "Searching for more…"
            scrapeAndVerify()
        }
    }

    suspend fun ensureLivePortals(
        targetCount: Int = 5,
        onStatus: (String) -> Unit = {}
    ): List<VerifiedPortal> = scrapeMutex.withLock {
        val maxPagesPerPress = 40
        val newAlive = mutableListOf<VerifiedPortal>()
        var lastPage: ScrapePage? = null
        var pagesTried = 0

        try {
            while (newAlive.size < targetCount && pagesTried < maxPagesPerPress) {
                // If pending pool is empty, fetch ONE page at a time
                while (_pendingPortals.isEmpty() && pagesTried < maxPagesPerPress) {
                    pagesTried++
                    onStatus("Discovering candidate portals (batch $pagesTried)…")

                    val page = try {
                        IptvScraper.scrapeCatalogPage(
                            maxResults = 50,
                            after = scrapeAfter,
                            source = _scrapeSource.value
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Scrape catalog page failed: ${e.message}")
                        null
                    } ?: break

                    lastPage = page
                    scrapeAfter = page.nextAfter

                    for (p in page.portals) {
                        if (verifiedKeys.contains(p.credKey)) continue
                        if (_attemptedKeys.contains(p.credKey)) continue
                        if (_pendingKeys.contains(p.credKey)) continue
                        _pendingKeys.add(p.credKey)
                        _pendingPortals.add(p)
                    }

                    storage.saveCandidatePortals(_pendingPortals)

                    if (_pendingPortals.isEmpty() && !page.hasMore) {
                        break
                    }
                }

                if (_pendingPortals.isEmpty()) break

                val remaining = targetCount - newAlive.size
                onStatus("Checking ${_pendingPortals.size} candidate portals · need $remaining more…")

                val snapshot = ArrayList(_pendingPortals)
                val attemptedInBatch = java.util.Collections.synchronizedSet(mutableSetOf<String>())
                IptvVerifier.verifyUntil(
                    portals = snapshot,
                    target = remaining,
                    onAttempted = { p ->
                        attemptedInBatch.add(p.credKey)
                    },
                    onProgress = { c, t, a ->
                        val total = newAlive.size + a
                        onStatus("Checking $c / $t · Active: $total / $targetCount")
                    },
                    onAlive = { v ->
                        if (verifiedKeys.add(v.credKey)) {
                            newAlive.add(v)
                            val current = _verifiedPortals.value.toMutableList()
                            current.add(v)
                            _verifiedPortals.value = sortFavoritesFirst(current, _favoritePortalKeys.value)
                        }
                    }
                )

                // Remove attempted portals from pending (safe on main coroutine)
                _attemptedKeys.addAll(attemptedInBatch)
                _pendingPortals.removeAll { attemptedInBatch.contains(it.credKey) }
                _pendingKeys.removeAll(attemptedInBatch)
                storage.saveCandidatePortals(_pendingPortals)

                if (newAlive.size < targetCount &&
                    _pendingPortals.isEmpty() &&
                    (lastPage != null && !lastPage.hasMore)) {
                    break
                }
            }

            if (newAlive.isNotEmpty()) {
                storage.saveVerifiedPortals(_verifiedPortals.value)
            }

            _canGetMore.value = _pendingPortals.isNotEmpty() ||
                    (lastPage?.hasMore ?: _canGetMore.value)
        } catch (e: Exception) {
            Log.e(TAG, "ensureLivePortals failed: ${e.message}")
            onStatus("Discovery error: ${e.message}")
        }

        newAlive
    }

    private suspend fun scrapeAndVerify() {
        try {
            val targetAlive = 5
            val newAlive = ensureLivePortals(targetAlive) { status ->
                _statusText.value = status
            }

            if (newAlive.isEmpty()) {
                _statusText.value = if (_canGetMore.value) {
                    "No new live portals found. Tap Get More."
                } else {
                    "No new live portals found in this source."
                }
            } else {
                val msg = if (newAlive.size >= targetAlive) {
                    "Found ${newAlive.size} live portals."
                } else {
                    "Found ${newAlive.size} live portals."
                }
                _statusText.value = if (_pendingPortals.isNotEmpty()) {
                    "$msg (${_pendingPortals.size} more cached)"
                } else msg
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scrape failed: ${e.message}")
            _statusText.value = "Scrape failed: ${e.message}"
        } finally {
            _isScraping.value = false
        }
    }

    fun stopChannelScan() {
        channelScanJob?.cancel()
        channelScanJob = null
        _isScanningChannel.value = false
        _channelScanStatus.value = "Stopped."
    }

    private fun sortHitsWithFavorites(hits: List<ChannelHit>, favs: Set<String>): List<ChannelHit> {
        val f = hits.filter { favs.contains(it.streamUrl) }
        val r = hits.filter { !favs.contains(it.streamUrl) }
        return f + r
    }

    fun openChannel(channel: HardcodedChannel) {
        _activeHardcoded.value = channel
        channelScanJob?.cancel()

        viewModelScope.launch {
            val favs = storage.loadChannelFavorites(channel.id)
            _currentChannelFavorites.value = favs

            val cachedHits = storage.loadChannelHits(channel.id)
            val sorted = sortHitsWithFavorites(cachedHits, favs)
            _channelHits.value = sorted

            // If no cached hits, scan first 8 portals
            if (cachedHits.isEmpty()) {
                scanChannel(channel, resetAttempted = false)
            } else {
                _channelScanStatus.value = "${cachedHits.size} cached feeds available."
            }
        }
    }

    fun toggleFavoriteHit(hit: ChannelHit) {
        val ch = _activeHardcoded.value ?: return
        viewModelScope.launch {
            val favs = _currentChannelFavorites.value.toMutableSet()
            if (favs.contains(hit.streamUrl)) {
                favs.remove(hit.streamUrl)
            } else {
                favs.add(hit.streamUrl)
            }
            _currentChannelFavorites.value = favs
            storage.saveChannelFavorites(ch.id, favs)
            _channelHits.value = sortHitsWithFavorites(_channelHits.value, favs)
        }
    }

    fun removeHit(hit: ChannelHit) {
        val ch = _activeHardcoded.value ?: return
        viewModelScope.launch {
            val updated = _channelHits.value.filter { it.streamUrl != hit.streamUrl }
            _channelHits.value = updated
            storage.saveChannelHits(ch.id, updated)
        }
    }

    fun isFavoriteHit(hit: ChannelHit): Boolean {
        return _currentChannelFavorites.value.contains(hit.streamUrl)
    }

    suspend fun getEpgForHit(hit: ChannelHit): List<EpgEntry> = withContext(Dispatchers.IO) {
        getEpg(hit.portal, hit.stream.streamId)
    }

    fun scanChannel(channel: HardcodedChannel, resetAttempted: Boolean = false) {
        channelScanJob?.cancel()
        channelScanJob = viewModelScope.launch {
            _isScanningChannel.value = true
            _activeHardcoded.value = channel

            val attempted = _channelAttemptedPortals.getOrPut(channel.id) { ConcurrentHashMap.newKeySet() }
            if (resetAttempted) {
                attempted.clear()
            }

            val hitsMap = ConcurrentHashMap<String, ChannelHit>()
            _channelHits.value.forEach { hitsMap[it.streamUrl] = it }
            val startHitsCount = hitsMap.size

            data class Candidate(val portal: VerifiedPortal, val stream: IptvStream, val url: String)

            suspend fun scanBatchOfPortals(toScan: List<VerifiedPortal>) {
                if (toScan.isEmpty()) return
                toScan.forEach { attempted.add(it.key) }
                _channelScanStatus.value = "Scanning ${toScan.size} portals for '${channel.name}'…"

                val scanJobs = toScan.map { portal ->
                    launch(Dispatchers.IO) {
                        try {
                            val matchingStreams = IptvClient.searchChannelInPortal(portal.portal, channel)
                            if (matchingStreams.isEmpty()) return@launch

                            val candidates = matchingStreams.mapNotNull { s ->
                                val url = IptvClient.streamUrl(portal.portal, s)
                                if (url.isNotEmpty() && !hitsMap.containsKey(url)) {
                                    Candidate(portal, s, url)
                                } else null
                            }

                            if (candidates.isEmpty()) return@launch

                            candidates.forEach { c ->
                                if (!isActive) return@launch
                                val alive = IptvAliveChecker.isAlive(c.url)
                                if (alive && isActive) {
                                    if (!hitsMap.containsKey(c.url)) {
                                        val hit = ChannelHit(portal = c.portal, stream = c.stream, streamUrl = c.url)
                                        hitsMap[c.url] = hit
                                        val currentList = sortHitsWithFavorites(hitsMap.values.toList(), _currentChannelFavorites.value)
                                        _channelHits.value = currentList
                                        storage.saveChannelHits(channel.id, currentList)
                                        _channelScanStatus.value = "Found ${currentList.size} active feeds · scanning…"
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                scanJobs.joinAll()
            }

            var allPortals = _verifiedPortals.value
            var unattempted = allPortals.filter { !attempted.contains(it.key) }

            // If we have no portals at all, or all existing portals were already scanned:
            // Auto scrape 5 live portals!
            if (allPortals.isEmpty() || (!resetAttempted && unattempted.isEmpty())) {
                val reason = if (allPortals.isEmpty()) {
                    "No portals available. Discovering 5 live portals…"
                } else {
                    "All portals scanned. Discovering 5 more live portals…"
                }
                _channelScanStatus.value = reason
                ensureLivePortals(targetCount = 5) { status ->
                    _channelScanStatus.value = status
                }
                allPortals = _verifiedPortals.value
                unattempted = allPortals.filter { !attempted.contains(it.key) }
            }

            // Loop to scan unattempted portals in batches of 8:
            // If no NEW feeds have been found in this scan, keep scanning subsequent batches until at least one NEW feed is found or unattempted is exhausted!
            while (isActive) {
                val toScan = if (unattempted.isNotEmpty()) {
                    unattempted.take(8)
                } else if (resetAttempted && attempted.isEmpty()) {
                    allPortals.take(8)
                } else {
                    emptyList()
                }

                if (toScan.isEmpty()) break

                scanBatchOfPortals(toScan)
                unattempted = allPortals.filter { !attempted.contains(it.key) }

                // If we found NEW feeds, or there are no unattempted portals left, pause this scan step
                if (hitsMap.size > startHitsCount || unattempted.isEmpty()) {
                    break
                }
            }

            // If after exhausting all existing portals we STILL haven't found any NEW hits, auto-scrape 5 more and scan them!
            if (isActive && hitsMap.size == startHitsCount && unattempted.isEmpty()) {
                _channelScanStatus.value = "Scanning more live portals…"
                val newAlive = ensureLivePortals(targetCount = 5) { status ->
                    _channelScanStatus.value = status
                }
                allPortals = _verifiedPortals.value
                val freshPortals = newAlive.filter { !attempted.contains(it.key) }
                if (freshPortals.isNotEmpty()) {
                    scanBatchOfPortals(freshPortals)
                }
            }

            allPortals = _verifiedPortals.value
            val remainingPortals = allPortals.count { !attempted.contains(it.key) }
            _channelScanStatus.value = if (_channelHits.value.isEmpty()) {
                "All ${attempted.size} portals scanned. No streams found. Tap Scan More."
            } else {
                "Found ${_channelHits.value.size} active feeds ($remainingPortals portals remaining)"
            }
            _isScanningChannel.value = false
        }
    }

    fun loadPortalCategories(portal: VerifiedPortal, section: IptvSection) {
        viewModelScope.launch {
            _isBrowserLoading.value = true
            try {
                val cats = IptvClient.categories(portal.portal, section)
                _browserCategories.value = cats
                val currentSelected = _browserSelectedCategoryId.value
                val targetCat = cats.firstOrNull { it.id == currentSelected } ?: cats.firstOrNull()
                val targetCatId = targetCat?.id ?: ""

                // Preserve existing loaded category streams if already populated
                if (_browserSelectedCategoryId.value != targetCatId || _browserStreams.value.isEmpty()) {
                    loadPortalStreams(portal, section, targetCatId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading categories: ${e.message}")
            } finally {
                _isBrowserLoading.value = false
            }
        }
    }

    fun loadPortalStreams(portal: VerifiedPortal, section: IptvSection, categoryId: String) {
        _browserSelectedCategoryId.value = categoryId
        viewModelScope.launch {
            _isBrowserLoading.value = true
            try {
                val streams = IptvClient.streams(portal.portal, section, categoryId)
                _browserStreams.value = streams

                // Load cached alive IDs
                val cachedAlive = storage.loadAliveStreamIds(portal.key)
                _browserAliveIds.value = cachedAlive
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading streams: ${e.message}")
            } finally {
                _isBrowserLoading.value = false
            }
        }
    }

    fun checkAliveForCategory(portal: VerifiedPortal) {
        if (_isAliveChecking.value) return
        val currentStreams = _browserStreams.value
        if (currentStreams.isEmpty()) return

        viewModelScope.launch {
            _isAliveChecking.value = true
            _aliveProgress.value = AliveProgress(0, currentStreams.size, 0)
            val aliveSet = ConcurrentHashMap.newKeySet<String>()
            aliveSet.addAll(_browserAliveIds.value)

            val pairs = currentStreams.map { s ->
                Pair(s.streamId, IptvClient.streamUrl(portal.portal, s))
            }

            IptvAliveChecker.launchCheck(
                streams = pairs,
                onResult = { id, alive ->
                    if (alive) aliveSet.add(id) else aliveSet.remove(id)
                    _browserAliveIds.value = aliveSet.toSet()
                },
                onProgress = { p ->
                    _aliveProgress.value = p
                },
                onDone = {
                    storage.saveAliveStreamIds(portal.key, aliveSet.toSet())
                    _isAliveChecking.value = false
                }
            )
        }
    }

    fun getStreamUrl(portal: VerifiedPortal, stream: IptvStream): String {
        return IptvClient.streamUrl(portal.portal, stream)
    }

    suspend fun getEpg(portal: VerifiedPortal, streamId: String): List<EpgEntry> = withContext(Dispatchers.IO) {
        if (streamId.isEmpty()) return@withContext emptyList()
        val cached = _epgMap[streamId]
        if (cached != null) return@withContext cached
        val res = IptvClient.shortEpg(portal.portal, streamId, limit = 2)
        _epgMap[streamId] = res
        res
    }

    // ── M3U Playlists ──

    fun addM3uPlaylist(name: String, url: String, onComplete: (Boolean) -> Unit) {
        addM3uFromUrl(name, url) { success, _ -> onComplete(success) }
    }

    fun addM3uFromUrl(name: String, url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val req = Request.Builder()
                    .url(url.trim())
                    .addHeader("User-Agent", "VLC/3.0.20 LibVLC/3.0.20")
                    .build()
                val client = OkHttpClient.Builder().build()
                val text = withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use { res ->
                        if (res.isSuccessful) res.body?.string().orEmpty() else ""
                    }
                }
                if (text.isBlank()) {
                    onResult(false, "Failed to download playlist")
                    return@launch
                }
                val channels = M3uParser.parse(text)
                if (channels.isEmpty()) {
                    onResult(false, "No channels found in playlist")
                    return@launch
                }
                val id = UUID.randomUUID().toString()
                val playlist = M3uPlaylist(
                    id = id,
                    name = name.trim().ifEmpty { "M3U Playlist" },
                    sourceUrl = url.trim(),
                    channels = channels
                )
                val list = _m3uPlaylists.value + playlist
                _m3uPlaylists.value = list
                storage.saveM3uPlaylists(list)
                storage.saveM3uPlaylistChannels(id, channels)
                onResult(true, "Added ${channels.size} channels")
            } catch (e: Exception) {
                Log.e(TAG, "Failed adding M3U: ${e.message}")
                onResult(false, e.message ?: "Failed to add M3U")
            }
        }
    }

    fun addM3uFromContent(name: String, content: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val channels = M3uParser.parse(content)
                if (channels.isEmpty()) {
                    onResult(false, "No channels found in text")
                    return@launch
                }
                val id = UUID.randomUUID().toString()
                val playlist = M3uPlaylist(
                    id = id,
                    name = name.trim().ifEmpty { "M3U Playlist" },
                    sourceUrl = null,
                    channels = channels
                )
                val list = _m3uPlaylists.value + playlist
                _m3uPlaylists.value = list
                storage.saveM3uPlaylists(list)
                storage.saveM3uPlaylistChannels(id, channels)
                onResult(true, "Added ${channels.size} channels")
            } catch (e: Exception) {
                Log.e(TAG, "Failed adding M3U: ${e.message}")
                onResult(false, e.message ?: "Failed to add M3U")
            }
        }
    }

    suspend fun loadPlaylistChannels(playlistId: String): List<M3uChannel> {
        return storage.loadM3uPlaylistChannels(playlistId)
    }

    fun deleteM3uPlaylist(id: String) {
        viewModelScope.launch {
            val list = _m3uPlaylists.value.filter { it.id != id }
            _m3uPlaylists.value = list
            storage.saveM3uPlaylists(list)
            storage.deleteM3uPlaylistFile(id)
        }
    }

    // ── Quick Channels ──

    fun addQuickChannel(name: String, query: String, icon: String = "", accentColor: Color = Color(0xFF6366F1)) {
        viewModelScope.launch {
            val keywords = query.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val newCh = QuickChannel(
                id = "quick_${UUID.randomUUID().toString().take(8)}",
                name = name.trim(),
                short = name.trim().take(3).uppercase(),
                category = "Quick Channels",
                keywords = if (keywords.isEmpty()) listOf(name.trim().lowercase()) else keywords,
                gradient = listOf(accentColor, accentColor.copy(alpha = 0.6f)),
                iconUrl = icon.trim().ifEmpty { null }
            )
            val list = _quickChannels.value + newCh
            _quickChannels.value = list
            storage.saveQuickChannels(list)
        }
    }

    fun removeQuickChannel(id: String) {
        viewModelScope.launch {
            val list = _quickChannels.value.filter { it.id != id }
            _quickChannels.value = list
            storage.saveQuickChannels(list)
        }
    }

    fun openQuickChannel(quick: QuickChannel) {
        val hc = HardcodedChannel(
            id = quick.id,
            name = quick.name,
            short = quick.short,
            category = quick.category,
            keywords = quick.keywords,
            gradient = quick.gradient,
            iconUrl = quick.iconUrl
        )
        openChannel(hc)
    }

    // ── Portal Management ──

    fun importPortalsJson(jsonStr: String, onResult: (Int, String) -> Unit) {
        viewModelScope.launch {
            try {
                val parsed = IptvScraper.parsePortalsFromJson(jsonStr)
                if (parsed.isEmpty()) {
                    onResult(0, "No valid portals found")
                    return@launch
                }
                var addedCount = 0
                for (p in parsed) {
                    if (!verifiedKeys.contains(p.credKey) && !_pendingKeys.contains(p.credKey)) {
                        _pendingKeys.add(p.credKey)
                        _pendingPortals.add(p)
                        addedCount++
                    }
                }
                storage.saveCandidatePortals(_pendingPortals)
                _canGetMore.value = _pendingPortals.isNotEmpty()
                onResult(addedCount, "Imported $addedCount portals into pending pool")
            } catch (e: Exception) {
                onResult(0, "Import failed: ${e.message}")
            }
        }
    }

    fun deletePortal(key: String) {
        deletePortals(setOf(key))
    }

    fun clearDeadPortals(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val current = _verifiedPortals.value
            val toKeep = mutableListOf<VerifiedPortal>()
            var removed = 0
            for (vp in current) {
                val ok = IptvClient.verifyOrNull(vp.portal, 5) != null
                if (ok) {
                    toKeep.add(vp)
                } else {
                    removed++
                    verifiedKeys.remove(vp.credKey)
                }
            }
            _verifiedPortals.value = toKeep
            storage.saveVerifiedPortals(toKeep)
            onComplete(removed)
        }
    }
}
