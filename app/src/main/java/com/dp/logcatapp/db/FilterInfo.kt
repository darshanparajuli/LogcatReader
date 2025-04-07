package com.dp.logcatapp.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
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

  @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
  fun insert(vararg info: FilterInfo)

  @Delete
  fun delete(vararg info: FilterInfo)

  @Query("DELETE FROM filters")
  fun deleteAll()
}