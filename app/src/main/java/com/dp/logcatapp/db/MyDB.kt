package com.dp.logcatapp.db

import android.arch.persistence.room.*
import android.content.Context
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
                            .fallbackToDestructiveMigration()
                            .build()
                }
            }
            return instance!!
        }
    }
}