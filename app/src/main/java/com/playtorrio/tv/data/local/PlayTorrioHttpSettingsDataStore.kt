package com.playtorrio.tv.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.playtorrio.tv.core.profile.ProfileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayTorrioHttpSettingsDataStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        private const val FEATURE = "playtorrio_http_settings"
        val DEFAULT_TOP_PROVIDERS = listOf("Cinejoy", "IStreamCDN", "111477")
    }

    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    private val providerOrderKey = stringPreferencesKey("provider_order")
    private val disabledProvidersKey = stringSetPreferencesKey("disabled_providers")
    private val httpScrapingEnabledKey = booleanPreferencesKey("http_scraping_enabled")

    val providerOrder: Flow<List<String>> = profileManager.activeProfileId.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { prefs ->
            val json = prefs[providerOrderKey]
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    gson.fromJson<List<String>>(json, listType) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }

    val disabledProviders: Flow<Set<String>> = profileManager.activeProfileId.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { prefs ->
            prefs[disabledProvidersKey] ?: emptySet()
        }
    }

    val isHttpScrapingEnabled: Flow<Boolean> = profileManager.activeProfileId.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { prefs ->
            prefs[httpScrapingEnabledKey] ?: true
        }
    }

    fun generateDefaultOrder(allProviderIds: List<String>): List<String> {
        val distinct = allProviderIds.distinct()
        val top = DEFAULT_TOP_PROVIDERS.filter { it in distinct }
        val remaining = (distinct - top.toSet()).shuffled(Random())
        return top + remaining
    }

    suspend fun setProviderOrder(order: List<String>) {
        val pid = profileManager.activeProfileId.value
        factory.get(pid, FEATURE).edit { prefs ->
            prefs[providerOrderKey] = gson.toJson(order)
        }
    }

    suspend fun setProviderEnabled(providerId: String, enabled: Boolean) {
        val pid = profileManager.activeProfileId.value
        factory.get(pid, FEATURE).edit { prefs ->
            val current = (prefs[disabledProvidersKey] ?: emptySet()).toMutableSet()
            val targetIds = if (providerId.equals("IStreamCDN", ignoreCase = true) || providerId.equals("IStreamFlare", ignoreCase = true)) {
                listOf("IStreamCDN", "IStreamFlare")
            } else {
                listOf(providerId)
            }
            if (enabled) {
                current.removeAll(targetIds)
            } else {
                current.addAll(targetIds)
            }
            prefs[disabledProvidersKey] = current
        }
    }

    suspend fun setHttpScrapingEnabled(enabled: Boolean) {
        val pid = profileManager.activeProfileId.value
        factory.get(pid, FEATURE).edit { prefs ->
            prefs[httpScrapingEnabledKey] = enabled
        }
    }

    suspend fun resetToDefaultOrder(allProviderIds: List<String>): List<String> {
        val newOrder = generateDefaultOrder(allProviderIds)
        setProviderOrder(newOrder)
        return newOrder
    }

    suspend fun moveProviderUp(providerId: String, currentOrder: List<String>): List<String> {
        val index = currentOrder.indexOf(providerId)
        if (index <= 0) return currentOrder
        val mutable = currentOrder.toMutableList()
        val temp = mutable[index - 1]
        mutable[index - 1] = mutable[index]
        mutable[index] = temp
        setProviderOrder(mutable)
        return mutable
    }

    suspend fun moveProviderDown(providerId: String, currentOrder: List<String>): List<String> {
        val index = currentOrder.indexOf(providerId)
        if (index < 0 || index >= currentOrder.lastIndex) return currentOrder
        val mutable = currentOrder.toMutableList()
        val temp = mutable[index + 1]
        mutable[index + 1] = mutable[index]
        mutable[index] = temp
        setProviderOrder(mutable)
        return mutable
    }
}
