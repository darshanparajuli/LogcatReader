package com.dp.logcatapp.db

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "filters")
data class FilterInfo(
  @PrimaryKey @ColumnInfo(name = "id") val id: Long? = null,
  @ColumnInfo(name = "tag") val tag: String? = null,
  @ColumnInfo(name = "message") val message: String? = null,
  @ColumnInfo(name = "pid") val pid: Int? = null,
  @ColumnInfo(name = "tid") val tid: Int? = null,
  @ColumnInfo(name = "log_levels") val logLevels: String? = null,
  @ColumnInfo(name = "exclude") val exclude: Boolean = false,
)

@Dao
interface FilterDao {

  @Query("SELECT * FROM filters")
  fun filters(): Flow<List<FilterInfo>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(vararg info: FilterInfo)

  @Delete
  fun delete(vararg info: FilterInfo)

  @Query("DELETE FROM filters")
  fun deleteAll()
}

@Entity(tableName = "saved_logs_info")
data class SavedLogInfo(
  @ColumnInfo(name = "name") val fileName: String,
  @PrimaryKey @ColumnInfo(name = "path") val path: String,
  @ColumnInfo(name = "is_custom") val isCustom: Boolean
)

@Dao
interface SavedLogsDao {

  @Query("SELECT * FROM saved_logs_info")
  fun savedLogs(): Flow<List<SavedLogInfo>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(vararg savedLogInfo: SavedLogInfo)

  @Delete
  fun delete(vararg savedLogInfo: SavedLogInfo)
}

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
    const val LATEST_VERSION = 3

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
          instance = Room.databaseBuilder(
            context.applicationContext,
            LogcatReaderDatabase::class.java, DB_NAME
          )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
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
  }
}