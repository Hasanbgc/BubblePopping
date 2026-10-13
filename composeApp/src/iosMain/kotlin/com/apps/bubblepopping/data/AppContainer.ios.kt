package com.apps.bubblepopping.data

import androidx.room.Room
import com.apps.bubblepopping.data.db.AppDatabase
import com.apps.bubblepopping.data.db.DATABASE_NAME
import com.apps.bubblepopping.data.db.buildAppDatabase
import com.apps.bubblepopping.data.prefs.SETTINGS_FILE_NAME
import com.apps.bubblepopping.data.prefs.createSettingsDataStore
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The process-wide [AppContainer], created on first use. */
val iosAppContainer: AppContainer by lazy {
    val documents = documentDirectory()
    val database = buildAppDatabase(
        Room.databaseBuilder<AppDatabase>(name = "$documents/$DATABASE_NAME")
    )
    val settings = createSettingsDataStore { "$documents/$SETTINGS_FILE_NAME" }
    AppContainer(database, settings)
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path) { "Documents directory not found" }
}
