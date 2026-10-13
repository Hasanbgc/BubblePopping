package com.apps.bubblepopping.data

import android.content.Context
import androidx.room.Room
import com.apps.bubblepopping.data.db.AppDatabase
import com.apps.bubblepopping.data.db.DATABASE_NAME
import com.apps.bubblepopping.data.db.buildAppDatabase
import com.apps.bubblepopping.data.prefs.SETTINGS_FILE_NAME
import com.apps.bubblepopping.data.prefs.createSettingsDataStore

private val lock = Any()

@Volatile
private var instance: AppContainer? = null

/** Returns the process-wide [AppContainer], creating it on first use. */
fun appContainer(context: Context): AppContainer =
    instance ?: synchronized(lock) {
        instance ?: createAppContainer(context.applicationContext).also { instance = it }
    }

private fun createAppContainer(context: Context): AppContainer {
    val dbFile = context.getDatabasePath(DATABASE_NAME)
    val database = buildAppDatabase(
        Room.databaseBuilder<AppDatabase>(context = context, name = dbFile.absolutePath)
    )
    val settings = createSettingsDataStore {
        context.filesDir.resolve(SETTINGS_FILE_NAME).absolutePath
    }
    return AppContainer(database, settings)
}
