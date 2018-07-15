package com.dp.logcatapp.db

import android.arch.persistence.room.*
import android.content.Context
import io.reactivex.Flowable

@Entity(tableName = "filters")
data class LogcatFilterRow(@PrimaryKey(autoGenerate = true) var id: Long?,
                           @ColumnInfo(name = "keyword") var keyword: String,
                           @ColumnInfo(name = "log_priorities") var logPriorities: String) {

    @Ignore
    constructor(keyword: String, logPriorities: String) :
            this(null, keyword, logPriorities)
}

@Dao
interface FilterDAO {

    @Query("SELECT * FROM filters")
    fun getAll(): Flowable<List<LogcatFilterRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg row: LogcatFilterRow)

    @Delete
    fun delete(vararg row: LogcatFilterRow)
}

@Database(entities = [LogcatFilterRow::class], exportSchema = false, version = 1)
abstract class FiltersDB : RoomDatabase() {
    abstract fun filterDAO(): FilterDAO

    companion object {
        private const val DB_NAME = "logcat_filters.db"
        private var instance: FiltersDB? = null

        fun getInstance(context: Context): FiltersDB {
            if (instance == null) {
                synchronized(FiltersDB::class) {
                    instance = Room.databaseBuilder(context.applicationContext,
                            FiltersDB::class.java, DB_NAME)
                            .build()
                }
            }
            return instance!!
        }
    }
}