package com.rosan.ruto.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rosan.ruto.data.AppDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

// Migration 7->8：为 conversations.ai_id 新增索引，不破坏任何数据
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_conversations_ai_id` ON `conversations` (`ai_id`)"
        )
    }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "ruto-database"
        )
            .addMigrations(MIGRATION_7_8)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    single { get<AppDatabase>().conversations() }
    single { get<AppDatabase>().messages() }
    single { get<AppDatabase>().ais() }
}
