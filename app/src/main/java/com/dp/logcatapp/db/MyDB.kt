package com.dp.logcatapp.db

import android.arch.persistence.room.*
import android.content.Context
import io.reactivex.Flowable

@Entity(tableName = "filters")
data class FilterInfo(@PrimaryKey(autoGenerate = true) val id: Long?,
                      @ColumnInfo(name = "keyword") val keyword: String,
                      @ColumnInfo(name = "tag") val tag: String,
                      @ColumnInfo(name = "log_priorities") val logPriorities: String,
                      @ColumnInfo(name = "exclude") val exclude: Boolean) {

    @Ignore
    constructor(keyword: String, tag: String, logPriorities: String, exclude: Boolean) :
            this(null, keyword, tag, logPriorities, exclude)
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
                            .build()
                }
            }
            return instance!!
        }
    }
}