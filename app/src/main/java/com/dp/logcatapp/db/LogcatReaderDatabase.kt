package com.dp.logcatapp.db

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [FilterInfo::class, SavedLogInfo::class],
  exportSchema = false,
  version = LogcatReaderDatabase.LATEST_VERSION,
)
abstract class LogcatReaderDatabase : RoomDatabase() {
  abstract fun filterDao(): FilterDao

  abstract fun savedLogsDao(): SavedLogsDao

  companion object {
    private const val DB_NAME = "logcat_reader_db"
    const val LATEST_VERSION = 4

    private val instanceLock = Any()

    @GuardedBy("instanceLock")
    @Volatile
    private var instance: LogcatReaderDatabase? = null

    fun getInstance(context: Context): LogcatReaderDatabase {
      val tmp = instance
      if (tmp != null) {
        return tmp
      }

      synchronized(instanceLock) {
        if (instance == null) {
          instance = Room
            .databaseBuilder(
              context.applicationContext,
              LogcatReaderDatabase::class.java, DB_NAME
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        }
        return instance!!
      }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE TABLE `filters_new` (`type` INTEGER NOT NULL, `value` TEXT NOT NULL, `exclude` INTEGER NOT NULL, PRIMARY KEY (`type`, `value`, `exclude`))"
        )
        // FilterType 0: keyword
        db.execSQL(
          "INSERT OR IGNORE INTO `filters_new` (`type`, `value`, `exclude`) SELECT 0, `keyword`, `exclude` FROM `filters` WHERE `keyword` != ''"
        )
        // FilterType 1: tag
        db.execSQL(
          "INSERT OR IGNORE INTO `filters_new` (`type`, `value`, `exclude`) SELECT 1, `tag`, `exclude` FROM `filters` WHERE `tag` != ''"
        )
        // FilterType 4: LogLevels
        db.execSQL(
          "INSERT OR IGNORE INTO `filters_new` (`type`, `value`, `exclude`) SELECT 4, `log_priorities`, `exclude` FROM `filters` WHERE `log_priorities` != ''"
        )

        db.execSQL("DROP TABLE `filters`")
        db.execSQL("ALTER TABLE `filters_new` RENAME TO `filters`")
        db.execSQL("CREATE INDEX `index_filters` ON `filters` (`exclude`)")
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS `saved_logs_info` (`name` TEXT NOT NULL, `path` TEXT NOT NULL, `is_custom` INTEGER NOT NULL, PRIMARY KEY (`path`))"
        )
      }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE `filters`")
        db.execSQL(
          """
            CREATE TABLE `filters` (
              `id` INTEGER, 
              `tag` TEXT,
              `message` TEXT,
              `pid` INTEGER,
              `tid` INTEGER,
              `log_levels` TEXT,
              `exclude` INTEGER NOT NULL,
              PRIMARY KEY (`id`)
            )
          """.trimIndent()
        )
      }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
            CREATE TABLE IF NOT EXISTS `saved_logs_info_new` (
              `name` TEXT NOT NULL, 
              `path` TEXT NOT NULL, 
              `is_custom` INTEGER NOT NULL, 
              `timestamp` INTEGER,
              PRIMARY KEY (`path`)
            )
          """.trimIndent()
        )
        db.execSQL(
          """
            INSERT OR IGNORE INTO `saved_logs_info_new` (`name`, `path`, `is_custom`, `timestamp`) 
            SELECT `name`, `path`, `is_custom`, null FROM `saved_logs_info`
          """.trimIndent()
        )
        db.execSQL("DROP TABLE `saved_logs_info`")
        db.execSQL("ALTER TABLE `saved_logs_info_new` RENAME TO `saved_logs_info`")

        db.execSQL(
          """
            CREATE TABLE `filters_new` (
              `id` INTEGER, 
              `tag` TEXT,
              `message` TEXT,
              `package_name` TEXT,
              `pid` INTEGER,
              `tid` INTEGER,
              `log_levels` TEXT,
              `exclude` INTEGER NOT NULL,
              `enabled` INTEGER NOT NULL,
              `useRegex` TEXT,
              PRIMARY KEY (`id`)
            )
          """.trimIndent()
        )
        db.execSQL(
          """
            INSERT OR IGNORE INTO `filters_new` (
              `id`, `tag`, `message`, `package_name`, `pid`, `tid`, `log_levels`, `exclude`, 
              `enabled`, `regex_enabled_filter_types`
            ) 
            SELECT `id`, `tag`, `message`, null, `pid`, `tid`, `log_levels`, `exclude`, true, null
            FROM `filters`
          """.trimIndent()
        )
        db.execSQL("DROP TABLE `filters`")
        db.execSQL("ALTER TABLE `filters_new` RENAME TO `filters`")
      }
    }
  }
}