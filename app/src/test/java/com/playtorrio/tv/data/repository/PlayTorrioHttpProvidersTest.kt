package com.playtorrio.tv.data.repository

import android.content.Context
import com.playtorrio.tv.core.scraper.A111477Scraper
import com.playtorrio.tv.data.local.PlayTorrioHttpSettingsDataStore
import com.playtorrio.tv.domain.model.Stream
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlayTorrioHttpProvidersTest {

    private fun createTestStream(
        name: String? = null,
        title: String? = null,
        url: String? = null,
        provider: String? = null
    ): Stream = Stream(
        name = name,
        title = title,
        description = null,
        url = url,
        ytId = null,
        infoHash = null,
        fileIdx = null,
        externalUrl = null,
        behaviorHints = null,
        addonName = "PlayTorrioHTTP",
        addonLogo = null,
        provider = provider
    )

    @Test
    fun a111477ScraperHasProperId() {
        val scraper = A111477Scraper()
        assertEquals("111477", scraper.id)
        assertEquals("111477", scraper.displayName)
        assertNotEquals("IStreamCDN", scraper.id)
    }

    @Test
    fun sortPlayTorrioHttpStreamsSortsCorrectly() = runBlocking {
        val httpSettingsDataStore = mockk<PlayTorrioHttpSettingsDataStore>()
        every { httpSettingsDataStore.providerOrder } returns flowOf(listOf("Cinejoy", "IStreamCDN", "111477"))

        val repository = StreamRepositoryImpl(
            context = mockk<Context>(relaxed = true),
            api = mockk(relaxed = true),
            addonRepository = mockk(relaxed = true),
            pluginManager = mockk(relaxed = true),
            profileManager = mockk(relaxed = true),
            debridSettingsDataStore = mockk(relaxed = true),
            tmdbService = mockk(relaxed = true),
            debridStreamPresentation = mockk(relaxed = true),
            localDebridAvailabilityService = mockk(relaxed = true),
            playTorrioHttpScraperManager = mockk(relaxed = true),
            playTorrioP2PScraperManager = mockk(relaxed = true),
            playTorrioHttpSettingsDataStore = httpSettingsDataStore,
            torrentSettings = mockk(relaxed = true)
        )

        val stream111477 = createTestStream(
            name = "[111477] Stream",
            title = "Movie 1080p",
            url = "https://a.111477.xyz/video.mp4",
            provider = "111477"
        )
        val streamCinejoy = createTestStream(
            name = "[Cinejoy] Stream",
            title = "Movie 1080p",
            url = "https://cinejoy.xyz/video.mp4",
            provider = "Cinejoy"
        )
        val streamIStreamFlare = createTestStream(
            name = "[IStreamFlare] Stream",
            title = "Movie 1080p",
            url = "https://istreamcdn.com/video.mp4",
            provider = "IStreamFlare"
        )

        val unsorted = listOf(stream111477, streamIStreamFlare, streamCinejoy)
        val sorted = repository.sortPlayTorrioHttpStreams(unsorted)

        assertEquals(3, sorted.size)
        assertEquals("Cinejoy", sorted[0].provider)
        assertEquals("IStreamFlare", sorted[1].provider)
        assertEquals("111477", sorted[2].provider)
    }

    @Test
    fun extractProviderFallbackWorks() {
        val repository = StreamRepositoryImpl(
            context = mockk<Context>(relaxed = true),
            api = mockk(relaxed = true),
            addonRepository = mockk(relaxed = true),
            pluginManager = mockk(relaxed = true),
            profileManager = mockk(relaxed = true),
            debridSettingsDataStore = mockk(relaxed = true),
            tmdbService = mockk(relaxed = true),
            debridStreamPresentation = mockk(relaxed = true),
            localDebridAvailabilityService = mockk(relaxed = true),
            playTorrioHttpScraperManager = mockk(relaxed = true),
            playTorrioP2PScraperManager = mockk(relaxed = true),
            playTorrioHttpSettingsDataStore = mockk(relaxed = true),
            torrentSettings = mockk(relaxed = true)
        )

        val s1 = createTestStream(name = "Direct", title = "111477 • 1080p", url = "http://test")
        assertEquals("111477", repository.extractProviderFallback(s1))

        val s2 = createTestStream(name = "Direct", title = "[IStreamCDN] 1080p", url = "http://test")
        assertEquals("IStreamCDN", repository.extractProviderFallback(s2))

        val s3 = createTestStream(name = "Direct", title = "[IStreamFlare] 1080p", url = "http://test")
        assertEquals("IStreamCDN", repository.extractProviderFallback(s3))

        val s4 = createTestStream(name = "Direct", title = "Cinejoy 1080p", url = "http://test")
        assertEquals("Cinejoy", repository.extractProviderFallback(s4))
    }
}
