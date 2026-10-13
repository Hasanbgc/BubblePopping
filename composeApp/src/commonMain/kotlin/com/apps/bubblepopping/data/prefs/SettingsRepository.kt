package com.apps.bubblepopping.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/** DataStore requires this extension for preference files. */
const val SETTINGS_FILE_NAME = "settings.preferences_pb"

data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    /** On-screen wind buttons, alongside swiping. An accessibility option. */
    val windButtonsEnabled: Boolean = true,
)

interface SettingsRepository {
    val settings: Flow<GameSettings>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setMusicEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setWindButtonsEnabled(enabled: Boolean)
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val MUSIC = booleanPreferencesKey("music_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val WIND_BUTTONS = booleanPreferencesKey("wind_buttons_enabled")
    }

    override val settings: Flow<GameSettings> = dataStore.data.map { prefs ->
        val defaults = GameSettings()
        GameSettings(
            soundEnabled = prefs[Keys.SOUND] ?: defaults.soundEnabled,
            musicEnabled = prefs[Keys.MUSIC] ?: defaults.musicEnabled,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            windButtonsEnabled = prefs[Keys.WIND_BUTTONS] ?: defaults.windButtonsEnabled,
        )
    }

    override suspend fun setSoundEnabled(enabled: Boolean) = set(Keys.SOUND, enabled)
    override suspend fun setMusicEnabled(enabled: Boolean) = set(Keys.MUSIC, enabled)
    override suspend fun setHapticsEnabled(enabled: Boolean) = set(Keys.HAPTICS, enabled)
    override suspend fun setWindButtonsEnabled(enabled: Boolean) = set(Keys.WIND_BUTTONS, enabled)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }
}

/** Creates the settings DataStore. A corrupted file is replaced with defaults. */
fun createSettingsDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { producePath().toPath() },
    )
