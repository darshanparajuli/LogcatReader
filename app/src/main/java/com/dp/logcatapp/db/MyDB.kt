package com.dp.logcatapp.db

import android.arch.persistence.room.*
import android.content.Context
import io.reactivex.Flowable

@Entity(tableName = "filters")
data class FilterInfo(@PrimaryKey(autoGenerate = true) var id: Long?,
                      @ColumnInfo(name = "keyword") var keyword: String,
                      @ColumnInfo(name = "tag") var tag: String,
                      @ColumnInfo(name = "log_priorities") var logPriorities: String,
                      @ColumnInfo(name = "exclude") var exclude: Boolean) {

    @Ignore
    constructor(keyword: String, tag: String, logPriorities: String, exclude: Boolean) :
            this(null, keyword, tag, logPriorities, exclude)
}

@Dao
interface FilterDAO {

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

@Database(entities = [FilterInfo::class], exportSchema = false, version = 1)
abstract class MyDB : RoomDatabase() {
    abstract fun filterDAO(): FilterDAO

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