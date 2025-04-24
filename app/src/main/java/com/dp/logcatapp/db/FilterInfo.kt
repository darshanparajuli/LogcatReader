package com.dp.logcatapp.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(tableName = "filters")
data class FilterInfo(
  @PrimaryKey @ColumnInfo(name = "id") val id: Long? = null,
  @ColumnInfo(name = "tag") val tag: String? = null,
  @ColumnInfo(name = "message") val message: String? = null,
  @ColumnInfo(name = "pid") val pid: Int? = null,
  @ColumnInfo(name = "tid") val tid: Int? = null,
  @ColumnInfo(name = "package_name") val packageName: String? = null,
  @ColumnInfo(name = "log_levels") val logLevels: Set<LogLevel>? = null,
  @ColumnInfo(name = "date_range") val dateRange: DateRange? = null,
  @ColumnInfo(name = "exclude") val exclude: Boolean = false,
  @ColumnInfo(name = "enabled") val enabled: Boolean = true,
  @ColumnInfo(name = "regex_enabled_filter_types") val regexEnabledFilterTypes: Set<RegexEnabledFilterType>? = null,
)

@Parcelize
data class DateRange(
  val start: Date?,
  val end: Date?,
) : Parcelable {
  init {
    check(start != null || end != null) {
      "both start and end cannot be null"
    }
  }
}

@Parcelize
enum class RegexEnabledFilterType : Parcelable {
  Tag,
  Message,
  PackageName,
}

@Parcelize
enum class LogLevel(
  val label: String
) : Parcelable {
  Assert("Assert"),
  Debug("Debug"),
  Error("Error"),
  Fatal("Fatal"),
  Info("Info"),
  Verbose("Verbose"),
  Warning("Warning"),
}

@Dao
interface FilterDao {

  @Query("SELECT * FROM filters")
  fun filters(): Flow<List<FilterInfo>>

  @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
  fun insert(vararg info: FilterInfo)

  @Delete
  fun delete(vararg info: FilterInfo)

  @Update(onConflict = OnConflictStrategy.REPLACE)
  fun update(vararg info: FilterInfo)

  @Query("DELETE FROM filters")
  fun deleteAll()
}

// Format: "<start millis>:<end millis>"
class DateRangeTypeConverter {
  @TypeConverter
  fun fromDateRage(dateRange: DateRange?): String? {
    if (dateRange == null) {
      return null
    }
    return buildString {
      if (dateRange.start != null) {
        append(dateRange.start.time)
      }
      append(":")
      if (dateRange.end != null) {
        append(dateRange.end.time)
      }
    }
  }

  @TypeConverter
  fun toDateRange(s: String?): DateRange? {
    if (s == null) {
      return null
    }
    val (start, end) = s.split(":")
    return DateRange(
      start = start.takeIf { it.isNotEmpty() }?.let { Date(it.toLong()) },
      end = end.takeIf { it.isNotEmpty() }?.let { Date(it.toLong()) },
    )
  }
}

class LogLevelsTypeConverter {
  companion object {
    private val logLevelsMap = LogLevel.entries.associate {
      Pair(it.label.first().toString(), it)
    }
  }

  @TypeConverter
  fun fromLogLevels(logLevels: Set<LogLevel>?): String? {
    if (logLevels == null) {
      return null
    }
    return logLevels.map { it.label.first().toString() }
      .sorted()
      .joinToString(separator = ",")
  }

  @TypeConverter
  fun toLogLevels(s: String?): Set<LogLevel>? {
    if (s == null) {
      return null
    }
    return s.split(",").mapNotNull { logLevelsMap[it] }.toSet()
  }
}

// Format: comma separated values of `RegexFilterType`
class RegexEnabledFilterTypeConverter {
  @TypeConverter
  fun fromFilterTypes(types: Set<RegexEnabledFilterType>?): String? {
    if (types == null) {
      return null
    }
    return types.joinToString(separator = ",") { it.ordinal.toString() }
  }

  @TypeConverter
  fun toFilterTypes(s: String?): Set<RegexEnabledFilterType>? {
    if (s == null) {
      return null
    }
    return s.split(",").mapNotNull { it.toIntOrNull() }
      .map { RegexEnabledFilterType.entries[it] }
      .toSet()
  }
}
