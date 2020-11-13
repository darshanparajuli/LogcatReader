package com.dp.logcatapp.db

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dp.logcatapp.fragments.filters.FilterType

@Entity(
  primaryKeys = ["type", "value", "exclude"], tableName = "filters",
  indices = [Index(name = "index_filters", value = ["exclude"])]
)
data class FilterInfo(
  @ColumnInfo(name = "type") val type: Int,
  @ColumnInfo(name = "value") val content: String,
  @ColumnInfo(name = "exclude") val exclude: Boolean
)

@Dao
interface FilterDao {

  @Query("SELECT * FROM filters WHERE `exclude` = 0")
  fun getFilters(): List<FilterInfo>

  @Query("SELECT * FROM filters WHERE `exclude` = 1")
  fun getExclusions(): List<FilterInfo>

  @Query("SELECT * FROM filters")
  fun getAll(): List<FilterInfo>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(vararg info: FilterInfo)

  @Delete
  fun delete(vararg info: FilterInfo)

  @Query("DELETE FROM filters WHERE `exclude` = :exclusions")
  fun deleteAll(exclusions: Boolean)
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
  fun getAllSync(): List<SavedLogInfo>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(vararg savedLogInfo: SavedLogInfo)

  @Delete
  fun delete(vararg savedLogInfo: SavedLogInfo)
}

@Database(entities = [FilterInfo::class, SavedLogInfo::class], exportSchema = false, version = 2)
abstract class MyDB : RoomDatabase() {
  abstract fun filterDao(): FilterDao

  abstract fun savedLogsDao(): SavedLogsDao

  companion object {
    private const val DB_NAME = "logcat_reader_db"

    private val instanceLock = Any()

    @GuardedBy("instanceLock")
    @Volatile
    private var instance: MyDB? = null

    fun getInstance(context: Context): MyDB {
      val tmp = instance
      if (tmp != null) {
        return tmp
      }

      synchronized(instanceLock) {
        if (instance == null) {
          instance = Room.databaseBuilder(
            context.applicationContext,
            MyDB::class.java, DB_NAME
          )
            .addMigrations(MIGRATION_1_2)
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
        db.execSQL(
          "INSERT OR IGNORE INTO `filters_new` (`type`, `value`, `exclude`) SELECT ${FilterType.KEYWORD}, `keyword`, `exclude` FROM `filters` WHERE `keyword` != ''"
        )
        db.execSQL(
          "INSERT OR IGNORE INTO `filters_new` (`type`, `value`, `exclude`) SELECT ${FilterType.TAG}, `tag`, `exclude` FROM `filters` WHERE `tag` != ''"
        )
        db.execSQL(
          "INSERT OR IGNORE INTO `filters_new` (`type`, `value`, `exclude`) SELECT ${FilterType.LOG_LEVELS}, `log_priorities`, `exclude` FROM `filters` WHERE `log_priorities` != ''"
        )

        db.execSQL("DROP TABLE `filters`")
        db.execSQL("ALTER TABLE `filters_new` RENAME TO `filters`")
        db.execSQL("CREATE INDEX `index_filters` ON `filters` (`exclude`)")
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS `saved_logs_info` (`name` TEXT NOT NULL, `path` TEXT NOT NULL, `is_custom` INTEGER NOT NULL, PRIMARY KEY (`path`))"
        )
      }
    }
  }
}