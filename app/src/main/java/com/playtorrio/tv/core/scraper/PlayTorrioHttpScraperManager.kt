package com.playtorrio.tv.core.scraper

import android.util.Log
import com.playtorrio.tv.core.network.CdnHeaderResolver
import com.playtorrio.tv.domain.model.AddonStreams
import com.playtorrio.tv.domain.model.ProxyHeaders
import com.playtorrio.tv.domain.model.Stream
import com.playtorrio.tv.domain.model.StreamBehaviorHints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayTorrioHttpScraperManager @Inject constructor(
    private val livenessValidator: HttpStreamLivenessValidator,
    private val settingsDataStore: com.playtorrio.tv.data.local.PlayTorrioHttpSettingsDataStore
) {
    companion object {
        private const val TAG = "PlayTorrioHttpManager"
        const val ADDON_NAME = "PlayTorrioHTTP"
    }

    private val scrapers: List<StreamScraper> = listOf(
        A111477Scraper(),
        BcineScraper(),
        CinejoyScraper(),
        DownloadEverythingScraper(),
        DuloScraper(),
        FSOnlineScraper(),
        FSonicScraper(),
        FlaxMoviesScraper(),
        FlyStreamScraper(),
        FourKHDHubScraper(),
        FrameScraper(),
        FshareTvScraper(),
        HexaScraper(),
        HindMoviezScraper(),
        KissKhScraper(),
        LMScriptScraper(),
        LookMovieScraper(),
        MappleScraper(),
        MegaSourceScraper(),
        MeowTvScraper(),
        MovieNightScraper(),
        MovyScraper(),
        MultiEmbedScraper(),
        NovaScraper(),
        PeeStreamScraper(),
        PurstreamScraper(),
        RiveStreamScraper(),
        VadapavScraper(),
        VidApiScraper(),
        VidCoreScraper(),
        VidFastScraper(),
        VidGodScraper(),
        VidLinkScraper(),
        VidRockScraper(),
        VidSrcScraper(),
        VidUpScraper(),
        VidVaultScraper(),
        VidZeeScraper(),
        VideasyScraper(),
        VixSrcScraper(),
        VuflixScraper(),
        XDownloaderScraper(),
        XPassScraper(),
        ZxcStreamScraper()
    )

    val allScrapers: List<StreamScraper> get() = scrapers

    suspend fun scrapeStreams(
        type: String,
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        imdbId: String? = null,
        tmdbId: Int? = null
    ): List<Stream> = withContext(Dispatchers.IO) {
        val request = ScraperMediaRequest(
            type = type,
            title = title,
            year = year,
            season = season,
            episode = episode,
            imdbId = imdbId,
            tmdbId = tmdbId
        )

        val isEnabled = settingsDataStore.isHttpScrapingEnabled.first()
        if (!isEnabled) {
            Log.d(TAG, "PlayTorrioHTTP scraping is disabled in settings")
            return@withContext emptyList<Stream>()
        }

        val disabledProviders = settingsDataStore.disabledProviders.first()
        val activeScrapers = scrapers.filter { it.id !in disabledProviders }

        Log.d(TAG, "Starting PlayTorrioHTTP scrape for '${request.title}' (${activeScrapers.size} active scrapers)")

        val tasks = activeScrapers.map { scraper ->
            async {
                try {
                    scraper.scrape(request).map { res ->
                        if (res.provider == null) res.copy(provider = scraper.id) else res
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Scraper ${scraper.javaClass.simpleName} error: ${e.message}")
                    emptyList<ScraperStreamResult>()
                }
            }
        }

        val allResults = tasks.awaitAll().flatten()
        val seenUrls = mutableSetOf<String>()
        val finalStreams = mutableListOf<Stream>()

        for (res in allResults) {
            val url = res.url.trim()
            if (url.isBlank() || !url.startsWith("http")) continue
            if (!seenUrls.add(url)) continue

            val resolvedHeaders = CdnHeaderResolver.resolveStreamHeaders(url, res.headers)
            finalStreams.add(
                Stream(
                    name = res.name,
                    title = res.title,
                    description = res.description,
                    url = url,
                    ytId = null,
                    infoHash = null,
                    fileIdx = null,
                    externalUrl = null,
                    behaviorHints = StreamBehaviorHints(
                        notWebReady = false,
                        bingeGroup = null,
                        countryWhitelist = null,
                        proxyHeaders = ProxyHeaders(
                            request = resolvedHeaders,
                            response = null
                        )
                    ),
                    addonName = ADDON_NAME,
                    addonLogo = null,
                    quality = res.quality,
                    provider = res.provider
                )
            )
        }

        val aliveStreams = finalStreams.map { s ->
            async {
                val isAlive = livenessValidator.isStreamAlive(
                    url = s.url ?: return@async null,
                    headers = s.behaviorHints?.proxyHeaders?.request
                )
                if (isAlive) s else null
            }
        }.awaitAll().filterNotNull()

        Log.d(TAG, "PlayTorrioHTTP finished with ${aliveStreams.size} verified alive streams (out of ${finalStreams.size})")
        aliveStreams
    }

    suspend fun scrapeStreamsDynamic(
        type: String,
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        imdbId: String? = null,
        tmdbId: Int? = null,
        onStreamsFound: suspend (List<Stream>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = ScraperMediaRequest(
            type = type,
            title = title,
            year = year,
            season = season,
            episode = episode,
            imdbId = imdbId,
            tmdbId = tmdbId
        )

        val isEnabled = settingsDataStore.isHttpScrapingEnabled.first()
        if (!isEnabled) {
            Log.d(TAG, "PlayTorrioHTTP scraping is disabled in settings")
            return@withContext
        }

        val disabledProviders = settingsDataStore.disabledProviders.first()
        val activeScrapers = scrapers.filter { it.id !in disabledProviders }

        Log.d(TAG, "Starting dynamic PlayTorrioHTTP scrape for '${request.title}' (${activeScrapers.size} active scrapers)")

        val seenUrls = java.util.Collections.synchronizedSet(mutableSetOf<String>())

        val jobs = activeScrapers.map { scraper ->
            launch {
                try {
                    val rawResults = withTimeoutOrNull(10000L) {
                        scraper.scrape(request)
                    } ?: emptyList()
                    val results = rawResults.map { res ->
                        if (res.provider == null) res.copy(provider = scraper.id) else res
                    }
                    if (results.isNotEmpty()) {
                        val streams = mutableListOf<Stream>()
                        for (res in results) {
                            val url = res.url.trim()
                            if (url.isBlank() || !url.startsWith("http")) continue
                            if (!seenUrls.add(url)) continue

                            val resolvedHeaders = CdnHeaderResolver.resolveStreamHeaders(url, res.headers)
                            streams.add(
                                Stream(
                                    name = res.name,
                                    title = res.title,
                                    description = res.description,
                                    url = url,
                                    ytId = null,
                                    infoHash = null,
                                    fileIdx = null,
                                    externalUrl = null,
                                    behaviorHints = StreamBehaviorHints(
                                        notWebReady = false,
                                        bingeGroup = null,
                                        countryWhitelist = null,
                                        proxyHeaders = ProxyHeaders(
                                            request = resolvedHeaders,
                                            response = null
                                        )
                                    ),
                                    addonName = ADDON_NAME,
                                    addonLogo = null,
                                    quality = res.quality,
                                    provider = res.provider
                                )
                            )
                        }
                        if (streams.isNotEmpty()) {
                            val aliveStreams = streams.map { s ->
                                async {
                                    val isAlive = livenessValidator.isStreamAlive(
                                        url = s.url ?: return@async null,
                                        headers = s.behaviorHints?.proxyHeaders?.request
                                    )
                                    if (isAlive) s else null
                                }
                            }.awaitAll().filterNotNull()

                            if (aliveStreams.isNotEmpty()) {
                                onStreamsFound(aliveStreams)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.d(TAG, "Scraper ${scraper.javaClass.simpleName} error: ${e.message}")
                }
            }
        }
        jobs.joinAll()
        Log.d(TAG, "Dynamic PlayTorrioHTTP scrape finished across all providers")
    }
}
