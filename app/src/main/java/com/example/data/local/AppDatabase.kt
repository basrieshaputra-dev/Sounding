package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SoundingLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soundingLogDao(): SoundingLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sounding_logs ADD COLUMN isManualKg INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sounding_logs ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE sounding_logs SET product = 'RBDPS' WHERE product = 'STEARIN (RPS)'")
                db.execSQL("UPDATE sounding_logs SET product = 'RHBDPKL' WHERE product = 'RHBDPKL MP 32-38'")
                db.execSQL("UPDATE sounding_logs SET product = 'RHBDPKO' WHERE product = 'RHBDPKO MP 34-36'")
            }
        }

        @Suppress("DEPRECATION")
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sounding_tangki.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
