package com.dp.logcatapp.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.arch.persistence.room.migration.Migration
import android.content.Context
import com.dp.logcatapp.fragments.filters.FilterType
import io.reactivex.Flowable

@Entity(tableName = "filters", indices = [Index(name = "index_filters", value = ["exclude"])])
data class FilterInfo(@PrimaryKey(autoGenerate = true) val id: Long?,
                      @ColumnInfo(name = "type") val type: Int,
                      @ColumnInfo(name = "value") val content: String,
                      @ColumnInfo(name = "exclude") val exclude: Boolean) {

    @Ignore
    constructor(type: Int, content: String, exclude: Boolean) :
            this(null, type, content, exclude)
}

@Dao
interface FilterDao {

    @Query("SELECT * FROM filters WHERE `exclude` = 0")
    fun getFilters(): Flowable<List<FilterInfo>>

    @Query("SELECT * FROM filters WHERE `exclude` = 1")
    fun getExclusions(): Flowable<List<FilterInfo>>

    @Query("SELECT * FROM filters")
    fun getAll(): Flowable<List<FilterInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg info: FilterInfo)

    @Delete
    fun delete(vararg info: FilterInfo)

    @Query("DELETE FROM filters WHERE `exclude` = :exclusions")
    fun deleteAll(exclusions: Boolean)
}

@Entity(tableName = "saved_logs_info")
data class SavedLogInfo(@ColumnInfo(name = "name") val fileName: String,
                        @PrimaryKey @ColumnInfo(name = "path") val path: String,
                        @ColumnInfo(name = "is_custom") val isCustom: Boolean)

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
        private var instance: MyDB? = null

        fun getInstance(context: Context): MyDB {
            if (instance == null) {
                synchronized(MyDB::class) {
                    instance = Room.databaseBuilder(context.applicationContext,
                            MyDB::class.java, DB_NAME)
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build()
                }
            }
            return instance!!
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `filters_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type` INTEGER NOT NULL, `value` TEXT NOT NULL, `exclude` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `filters_new` (`type`, `value`, `exclude`) VALUES (`${FilterType.KEYWORD}`, SELECT (`keyword`, `exclude`) FROM `filters`)")
                db.execSQL("INSERT INTO `filters_new` (`type`, `value`, `exclude`) VALUES (`${FilterType.TAG}`, SELECT (`tag`, `exclude`) FROM `filters`)")
                db.execSQL("INSERT INTO `filters_new` (`type`, `value`, `exclude`) VALUES (`${FilterType.LOG_LEVELS}`, SELECT (`log_priorities`, `exclude`) FROM `filters`)")

                db.execSQL("DROP TABLE `filters`")
                db.execSQL("ALTER TABLE `filters_new` RENAME TO `filters`")
            }
        }
    }
}