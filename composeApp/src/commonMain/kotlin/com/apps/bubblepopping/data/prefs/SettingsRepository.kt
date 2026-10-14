package com.apps.bubblepopping.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.apps.bubblepopping.meta.Cosmetic
import com.apps.bubblepopping.meta.CosmeticKind
import com.apps.bubblepopping.meta.Cosmetics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/** DataStore requires this extension for preference files. */
const val SETTINGS_FILE_NAME = "settings.preferences_pb"

/** Longest local player name. */
const val MAX_PLAYER_NAME_LENGTH = 16

data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    /** On-screen wind buttons, alongside swiping. An accessibility option. */
    val windButtonsEnabled: Boolean = true,
    /** Local name until Google / Facebook sign-in provides one. */
    val playerName: String = DEFAULT_PLAYER_NAME,
    /** Equipped cosmetic ids, one per [CosmeticKind]. */
    val equippedSkin: String = Cosmetics.CLASSIC.id,
    val equippedPopEffect: String = Cosmetics.RAINBOW.id,
    val equippedBackground: String = Cosmetics.DEFAULT_BACKGROUND.id,
    /** The World 1 Stage 1 tutorial has been finished or skipped. */
    val tutorialSeen: Boolean = false,
    /** Accessibility: bigger touch area around every bubble. */
    val largeHitArea: Boolean = false,
    /** Accessibility: a striped hazard ring around skulls, readable without colour. */
    val highContrastSkulls: Boolean = false,
) {
    fun equipped(kind: CosmeticKind): String = when (kind) {
        CosmeticKind.SKIN -> equippedSkin
        CosmeticKind.POP_EFFECT -> equippedPopEffect
        CosmeticKind.BACKGROUND -> equippedBackground
    }

    companion object {
        const val DEFAULT_PLAYER_NAME = "Player"
    }
}

interface SettingsRepository {
    val settings: Flow<GameSettings>
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setMusicEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setWindButtonsEnabled(enabled: Boolean)
    /** Trimmed and shortened to [MAX_PLAYER_NAME_LENGTH]; blank resets to the default. */
    suspend fun setPlayerName(name: String)
    /** Equips [cosmetic] in its slot. Callers check the player owns it. */
    suspend fun equip(cosmetic: Cosmetic)
    suspend fun setTutorialSeen(seen: Boolean)
    suspend fun setLargeHitArea(enabled: Boolean)
    suspend fun setHighContrastSkulls(enabled: Boolean)
    /** Back to the default cosmetics and the tutorial unseen (used by "reset progress"). */
    suspend fun resetProgressSettings()
}

/** The name as it will be stored. */
fun cleanPlayerName(name: String): String =
    name.trim().take(MAX_PLAYER_NAME_LENGTH).ifBlank { GameSettings.DEFAULT_PLAYER_NAME }

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val MUSIC = booleanPreferencesKey("music_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val WIND_BUTTONS = booleanPreferencesKey("wind_buttons_enabled")
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val SKIN = stringPreferencesKey("equipped_skin")
        val POP_EFFECT = stringPreferencesKey("equipped_pop_effect")
        val BACKGROUND = stringPreferencesKey("equipped_background")
        val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        val LARGE_HIT_AREA = booleanPreferencesKey("large_hit_area")
        val HIGH_CONTRAST_SKULLS = booleanPreferencesKey("high_contrast_skulls")

        fun equipped(kind: CosmeticKind) = when (kind) {
            CosmeticKind.SKIN -> SKIN
            CosmeticKind.POP_EFFECT -> POP_EFFECT
            CosmeticKind.BACKGROUND -> BACKGROUND
        }
    }

    override val settings: Flow<GameSettings> = dataStore.data.map { prefs ->
        val defaults = GameSettings()
        GameSettings(
            soundEnabled = prefs[Keys.SOUND] ?: defaults.soundEnabled,
            musicEnabled = prefs[Keys.MUSIC] ?: defaults.musicEnabled,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            windButtonsEnabled = prefs[Keys.WIND_BUTTONS] ?: defaults.windButtonsEnabled,
            playerName = prefs[Keys.PLAYER_NAME] ?: defaults.playerName,
            equippedSkin = prefs[Keys.SKIN] ?: defaults.equippedSkin,
            equippedPopEffect = prefs[Keys.POP_EFFECT] ?: defaults.equippedPopEffect,
            equippedBackground = prefs[Keys.BACKGROUND] ?: defaults.equippedBackground,
            tutorialSeen = prefs[Keys.TUTORIAL_SEEN] ?: defaults.tutorialSeen,
            largeHitArea = prefs[Keys.LARGE_HIT_AREA] ?: defaults.largeHitArea,
            highContrastSkulls = prefs[Keys.HIGH_CONTRAST_SKULLS] ?: defaults.highContrastSkulls,
        )
    }

    override suspend fun setSoundEnabled(enabled: Boolean) = set(Keys.SOUND, enabled)
    override suspend fun setMusicEnabled(enabled: Boolean) = set(Keys.MUSIC, enabled)
    override suspend fun setHapticsEnabled(enabled: Boolean) = set(Keys.HAPTICS, enabled)
    override suspend fun setWindButtonsEnabled(enabled: Boolean) = set(Keys.WIND_BUTTONS, enabled)
    override suspend fun setPlayerName(name: String) = set(Keys.PLAYER_NAME, cleanPlayerName(name))
    override suspend fun equip(cosmetic: Cosmetic) = set(Keys.equipped(cosmetic.kind), cosmetic.id)
    override suspend fun setTutorialSeen(seen: Boolean) = set(Keys.TUTORIAL_SEEN, seen)
    override suspend fun setLargeHitArea(enabled: Boolean) = set(Keys.LARGE_HIT_AREA, enabled)
    override suspend fun setHighContrastSkulls(enabled: Boolean) = set(Keys.HIGH_CONTRAST_SKULLS, enabled)

    override suspend fun resetProgressSettings() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SKIN)
            prefs.remove(Keys.POP_EFFECT)
            prefs.remove(Keys.BACKGROUND)
            prefs.remove(Keys.TUTORIAL_SEEN)
        }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}

/** Creates the settings DataStore. A corrupted file is replaced with defaults. */
fun createSettingsDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { producePath().toPath() },
    )
