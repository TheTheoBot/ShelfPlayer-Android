package com.thetheobot.shelfplayer

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

internal fun shouldUseDarkTheme(themeMode: ThemeMode, systemDarkTheme: Boolean): Boolean {
    return when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

internal fun themeModeButtonLabel(themeMode: ThemeMode): String {
    return when (themeMode) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Hell"
        ThemeMode.DARK -> "Dunkel"
    }
}

data class AppSettings(
    val playbackSkipIntervalSeconds: Int = DEFAULT_PLAYBACK_SKIP_INTERVAL_SECONDS,
    val defaultPlaybackRate: Float = DEFAULT_PLAYBACK_RATE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        const val DEFAULT_PLAYBACK_SKIP_INTERVAL_SECONDS = 15
        const val DEFAULT_PLAYBACK_RATE = 1.0f
    }
}

interface AppSettingsRepository {
    val settings: StateFlow<AppSettings>

    fun setPlaybackSkipIntervalSeconds(seconds: Int)
    fun setDefaultPlaybackRate(rate: Float)
    fun setThemeMode(themeMode: ThemeMode)
    fun save(settings: AppSettings)
}

class SharedPreferencesAppSettingsRepository(
    private val sharedPreferences: SharedPreferences,
) : AppSettingsRepository {
    private val _settings = MutableStateFlow(readSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun setPlaybackSkipIntervalSeconds(seconds: Int) {
        save(_settings.value.copy(playbackSkipIntervalSeconds = seconds))
    }

    override fun setDefaultPlaybackRate(rate: Float) {
        save(_settings.value.copy(defaultPlaybackRate = rate))
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        save(_settings.value.copy(themeMode = themeMode))
    }

    override fun save(settings: AppSettings) {
        val normalized = settings.normalized()
        sharedPreferences.edit()
            .putInt(KEY_PLAYBACK_SKIP_INTERVAL_SECONDS, normalized.playbackSkipIntervalSeconds)
            .putFloat(KEY_DEFAULT_PLAYBACK_RATE, normalized.defaultPlaybackRate)
            .putString(KEY_THEME_MODE, normalized.themeMode.name)
            .apply()
        _settings.value = normalized
    }

    private fun readSettings(): AppSettings {
        val persistedSkipInterval = sharedPreferences
            .getInt(KEY_PLAYBACK_SKIP_INTERVAL_SECONDS, AppSettings.DEFAULT_PLAYBACK_SKIP_INTERVAL_SECONDS)

        val normalizedPersistedSkipInterval = if (persistedSkipInterval == 0) {
            AppSettings.DEFAULT_PLAYBACK_SKIP_INTERVAL_SECONDS
        } else {
            persistedSkipInterval
        }

        return AppSettings(
            playbackSkipIntervalSeconds = normalizedPersistedSkipInterval,
            defaultPlaybackRate = sharedPreferences
                .getFloat(KEY_DEFAULT_PLAYBACK_RATE, AppSettings.DEFAULT_PLAYBACK_RATE),
            themeMode = sharedPreferences
                .getString(KEY_THEME_MODE, null)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
        ).normalized()
    }

    private fun AppSettings.normalized(): AppSettings {
        return copy(
            playbackSkipIntervalSeconds = playbackSkipIntervalSeconds.coerceAtLeast(1),
            defaultPlaybackRate = defaultPlaybackRate.takeIf { it.isFinite() && it > 0f }
                ?: AppSettings.DEFAULT_PLAYBACK_RATE,
        )
    }

    private companion object {
        const val KEY_PLAYBACK_SKIP_INTERVAL_SECONDS = "app_settings_playback_skip_interval_seconds"
        const val KEY_DEFAULT_PLAYBACK_RATE = "app_settings_default_playback_rate"
        const val KEY_THEME_MODE = "app_settings_theme_mode"
    }
}
