package com.notifbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter fun matchTypeToString(t: MatchType): String = t.name
    @TypeConverter fun stringToMatchType(s: String): MatchType = MatchType.valueOf(s)
}

/**
 * v1 → v2: add [NotificationEntity.sbnKey] for dedupe and the postedAt/filtered
 * indices. Index names must match what Room generates from @Entity(indices=...).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notifications ADD COLUMN sbnKey TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_postedAt ON notifications (postedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_filtered ON notifications (filtered)")
    }
}

@Database(
    entities = [NotificationEntity::class, FilterRule::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "notifbox.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
