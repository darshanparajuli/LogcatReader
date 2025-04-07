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

  @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
  fun insert(vararg savedLogInfo: SavedLogInfo)

  @Delete
  fun delete(vararg savedLogInfo: SavedLogInfo)
}