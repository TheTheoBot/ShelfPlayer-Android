package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsRepositoryTest {
    @Test
    fun `shared preferences app settings repository uses safe defaults when nothing is persisted`() {
        val repository = SharedPreferencesAppSettingsRepository(FakeSharedPreferences())

        assertEquals(AppSettings(), repository.settings.value)
    }

    @Test
    fun `shared preferences app settings repository persists values across a fresh instance`() {
        val sharedPreferences = FakeSharedPreferences()
        val firstRepository = SharedPreferencesAppSettingsRepository(sharedPreferences)

        firstRepository.setPlaybackSkipIntervalSeconds(30)
        firstRepository.setDefaultPlaybackRate(1.25f)
        firstRepository.setThemeMode(ThemeMode.DARK)

        val secondRepository = SharedPreferencesAppSettingsRepository(sharedPreferences)

        assertEquals(
            AppSettings(
                playbackSkipIntervalSeconds = 30,
                defaultPlaybackRate = 1.25f,
                themeMode = ThemeMode.DARK,
            ),
            secondRepository.settings.value,
        )
    }

    @Test
    fun `shared preferences app settings repository updates the current snapshot immediately`() {
        val repository = SharedPreferencesAppSettingsRepository(FakeSharedPreferences())

        repository.setPlaybackSkipIntervalSeconds(45)
        repository.setDefaultPlaybackRate(0.75f)
        repository.setThemeMode(ThemeMode.LIGHT)

        assertEquals(
            AppSettings(
                playbackSkipIntervalSeconds = 45,
                defaultPlaybackRate = 0.75f,
                themeMode = ThemeMode.LIGHT,
            ),
            repository.settings.value,
        )
    }

    @Test
    fun `shared preferences app settings repository normalizes invalid persisted data back to safe defaults`() {
        val sharedPreferences = FakeSharedPreferences().apply {
            edit()
                .putInt("app_settings_playback_skip_interval_seconds", 0)
                .putFloat("app_settings_default_playback_rate", Float.NaN)
                .putString("app_settings_theme_mode", "NEON")
                .apply()
        }

        val repository = SharedPreferencesAppSettingsRepository(sharedPreferences)

        assertEquals(AppSettings(), repository.settings.value)
    }

    @Test
    fun `shared preferences app settings repository coerces negative skip intervals to one second`() {
        val sharedPreferences = FakeSharedPreferences().apply {
            edit().putInt("app_settings_playback_skip_interval_seconds", -30).apply()
        }

        val repository = SharedPreferencesAppSettingsRepository(sharedPreferences)

        assertEquals(1, repository.settings.value.playbackSkipIntervalSeconds)
    }

    @Test
    fun `shared preferences app settings repository falls back to default playback rate for non finite values`() {
        val sharedPreferences = FakeSharedPreferences().apply {
            edit()
                .putFloat("app_settings_default_playback_rate", Float.POSITIVE_INFINITY)
                .apply()
        }

        val repository = SharedPreferencesAppSettingsRepository(sharedPreferences)

        assertEquals(AppSettings.DEFAULT_PLAYBACK_RATE, repository.settings.value.defaultPlaybackRate, 0f)
    }

    @Test
    fun `shared preferences app settings repository normalizes values passed through save`() {
        val repository = SharedPreferencesAppSettingsRepository(FakeSharedPreferences())

        repository.save(
            AppSettings(
                playbackSkipIntervalSeconds = 0,
                defaultPlaybackRate = Float.NEGATIVE_INFINITY,
                themeMode = ThemeMode.DARK,
            ),
        )

        assertEquals(
            AppSettings(
                playbackSkipIntervalSeconds = 1,
                defaultPlaybackRate = AppSettings.DEFAULT_PLAYBACK_RATE,
                themeMode = ThemeMode.DARK,
            ),
            repository.settings.value,
        )
    }
}
