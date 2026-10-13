package com.apps.bubblepopping.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.apps.bubblepopping.data.db.AppDatabase
import com.apps.bubblepopping.data.prefs.DataStoreSettingsRepository
import com.apps.bubblepopping.data.prefs.SettingsRepository

/**
 * App-wide dependencies. Exactly one instance per process — DataStore fails if
 * two instances open the same file. Platform code creates it (it needs file paths).
 */
class AppContainer(
    database: AppDatabase,
    settingsDataStore: DataStore<Preferences>,
) {
    val progressRepository: ProgressRepository = RoomProgressRepository(database)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(settingsDataStore)
}
