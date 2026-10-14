package com.apps.bubblepopping.data.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

const val DATABASE_NAME = "bubble_popping.db"

@Database(
    entities = [
        StageProgressEntity::class,
        EndlessScoreEntity::class,
        StatsEntity::class,
        PlayerEntity::class,
        MissionEntity::class,
        OwnedCosmeticEntity::class,
        AchievementEntity::class,
    ],
    version = 2,
    // Version 2 (Phase 8) only adds tables, so Room migrates it automatically.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stageProgressDao(): StageProgressDao
    abstract fun endlessScoreDao(): EndlessScoreDao
    abstract fun statsDao(): StatsDao
    abstract fun metaDao(): MetaDao
}

// Room's KSP processor generates the `actual` implementations for each platform.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** Applies the settings shared by every platform and builds the database. */
fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
