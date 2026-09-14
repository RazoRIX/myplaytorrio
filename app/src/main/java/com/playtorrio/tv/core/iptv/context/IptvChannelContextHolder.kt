package com.playtorrio.tv.core.iptv.context

import com.playtorrio.tv.core.iptv.channels.HardcodedChannel
import com.playtorrio.tv.core.iptv.channels.HardcodedChannels
import com.playtorrio.tv.core.iptv.model.IptvStream
import com.playtorrio.tv.core.iptv.model.VerifiedPortal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.playtorrio.tv.core.iptv.model.ChannelHit

sealed class IptvLiveContext {
    data class Portal(
        val portal: VerifiedPortal,
        val categoryName: String,
        val currentStreamId: String,
        val channels: List<IptvStream>
    ) : IptvLiveContext()

    data class Hardcoded(
        val channel: HardcodedChannel? = null,
        val currentChannelId: String = channel?.id.orEmpty(),
        val hits: List<ChannelHit> = emptyList(),
        val currentStreamUrl: String = "",
        val allChannels: List<HardcodedChannel> = HardcodedChannels.all
    ) : IptvLiveContext()
}

object IptvChannelContextHolder {
    private val _currentContext = MutableStateFlow<IptvLiveContext?>(null)
    val currentContext: StateFlow<IptvLiveContext?> = _currentContext.asStateFlow()

    fun setPortalContext(
        portal: VerifiedPortal,
        categoryName: String,
        currentStreamId: String,
        channels: List<IptvStream>
    ) {
        _currentContext.value = IptvLiveContext.Portal(
            portal = portal,
            categoryName = categoryName,
            currentStreamId = currentStreamId,
            channels = channels
        )
    }

    fun setHardcodedContext(
        channel: HardcodedChannel,
        hits: List<ChannelHit> = emptyList(),
        currentStreamUrl: String = "",
        allChannels: List<HardcodedChannel> = HardcodedChannels.all
    ) {
        _currentContext.value = IptvLiveContext.Hardcoded(
            channel = channel,
            currentChannelId = channel.id,
            hits = hits,
            currentStreamUrl = currentStreamUrl,
            allChannels = allChannels
        )
    }

    fun setHardcodedContext(
        currentChannelId: String,
        allChannels: List<HardcodedChannel> = HardcodedChannels.all
    ) {
        val channel = HardcodedChannels.byId(currentChannelId)
        _currentContext.value = IptvLiveContext.Hardcoded(
            channel = channel,
            currentChannelId = currentChannelId,
            hits = emptyList(),
            currentStreamUrl = "",
            allChannels = allChannels
        )
    }

    fun updateCurrentStreamId(streamId: String) {
        val ctx = _currentContext.value
        if (ctx is IptvLiveContext.Portal) {
            _currentContext.value = ctx.copy(currentStreamId = streamId)
        }
    }

    fun updateCurrentStreamUrl(streamUrl: String) {
        val ctx = _currentContext.value
        if (ctx is IptvLiveContext.Hardcoded) {
            _currentContext.value = ctx.copy(currentStreamUrl = streamUrl)
        }
    }

    fun updateCurrentHardcodedId(channelId: String) {
        val ctx = _currentContext.value
        if (ctx is IptvLiveContext.Hardcoded) {
            _currentContext.value = ctx.copy(
                currentChannelId = channelId,
                channel = HardcodedChannels.byId(channelId) ?: ctx.channel
            )
        }
    }

    fun clearContext() {
        _currentContext.value = null
    }

    fun hasContext(): Boolean = _currentContext.value != null
}
