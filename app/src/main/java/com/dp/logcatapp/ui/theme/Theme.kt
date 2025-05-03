package com.dp.logcatapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dp.logcatapp.util.SettingsPrefKeys
import com.dp.logcatapp.util.SettingsPrefKeys.Appearance.Theme
import com.dp.logcatapp.util.rememberBooleanSharedPreference
import com.dp.logcatapp.util.rememberStringSharedPreference

private val lightScheme = lightColorScheme(
  primary = primaryLight,
  onPrimary = onPrimaryLight,
  primaryContainer = primaryContainerLight,
  onPrimaryContainer = onPrimaryContainerLight,
  secondary = secondaryLight,
  onSecondary = onSecondaryLight,
  secondaryContainer = secondaryContainerLight,
  onSecondaryContainer = onSecondaryContainerLight,
  tertiary = tertiaryLight,
  onTertiary = onTertiaryLight,
  tertiaryContainer = tertiaryContainerLight,
  onTertiaryContainer = onTertiaryContainerLight,
  error = errorLight,
  onError = onErrorLight,
  errorContainer = errorContainerLight,
  onErrorContainer = onErrorContainerLight,
  background = backgroundLight,
  onBackground = onBackgroundLight,
  surface = surfaceLight,
  onSurface = onSurfaceLight,
  surfaceVariant = surfaceVariantLight,
  onSurfaceVariant = onSurfaceVariantLight,
  outline = outlineLight,
  outlineVariant = outlineVariantLight,
  scrim = scrimLight,
  inverseSurface = inverseSurfaceLight,
  inverseOnSurface = inverseOnSurfaceLight,
  inversePrimary = inversePrimaryLight,
  surfaceDim = surfaceDimLight,
  surfaceBright = surfaceBrightLight,
  surfaceContainerLowest = surfaceContainerLowestLight,
  surfaceContainerLow = surfaceContainerLowLight,
  surfaceContainer = surfaceContainerLight,
  surfaceContainerHigh = surfaceContainerHighLight,
  surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
  primary = primaryDark,
  onPrimary = onPrimaryDark,
  primaryContainer = primaryContainerDark,
  onPrimaryContainer = onPrimaryContainerDark,
  secondary = secondaryDark,
  onSecondary = onSecondaryDark,
  secondaryContainer = secondaryContainerDark,
  onSecondaryContainer = onSecondaryContainerDark,
  tertiary = tertiaryDark,
  onTertiary = onTertiaryDark,
  tertiaryContainer = tertiaryContainerDark,
  onTertiaryContainer = onTertiaryContainerDark,
  error = errorDark,
  onError = onErrorDark,
  errorContainer = errorContainerDark,
  onErrorContainer = onErrorContainerDark,
  background = backgroundDark,
  onBackground = onBackgroundDark,
  surface = surfaceDark,
  onSurface = onSurfaceDark,
  surfaceVariant = surfaceVariantDark,
  onSurfaceVariant = onSurfaceVariantDark,
  outline = outlineDark,
  outlineVariant = outlineVariantDark,
  scrim = scrimDark,
  inverseSurface = inverseSurfaceDark,
  inverseOnSurface = inverseOnSurfaceDark,
  inversePrimary = inversePrimaryDark,
  surfaceDim = surfaceDimDark,
  surfaceBright = surfaceBrightDark,
  surfaceContainerLowest = surfaceContainerLowestDark,
  surfaceContainerLow = surfaceContainerLowDark,
  surfaceContainer = surfaceContainerDark,
  surfaceContainerHigh = surfaceContainerHighDark,
  surfaceContainerHighest = surfaceContainerHighestDark,
)

/**
 * Dynamic color is available on Android 12+
 */
@Composable
fun LogcatReaderTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable() () -> Unit
) {

  val context = LocalContext.current
  val appThemeSetting = rememberStringSharedPreference(
    key = SettingsPrefKeys.Appearance.KEY_THEME,
    default = SettingsPrefKeys.Appearance.Default.THEME,
  ).value

  val dynamicColor = rememberBooleanSharedPreference(
    key = SettingsPrefKeys.Appearance.KEY_DYNAMIC_COLOR,
    default = SettingsPrefKeys.Appearance.Default.DYNAMIC_COLOR
  ).value

  val colorScheme = when (appThemeSetting) {
    Theme.AUTO -> {
      when {
        dynamicColor && isDynamicThemeAvailable() -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
      }
    }
    Theme.DARK -> if (dynamicColor && isDynamicThemeAvailable()) {
      dynamicDarkColorScheme(context)
    } else {
      darkScheme
    }
    else -> if (dynamicColor && isDynamicThemeAvailable()) {
      dynamicLightColorScheme(context)
    } else {
      lightScheme
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    shapes = Shapes,
    typography = AppTypography,
    content = content,
  )
}

fun isDynamicThemeAvailable() = Build.VERSION.SDK_INT >= 31

@Composable
fun currentSearchHitColor(): Color =
  if (isDarkThemeOn()) currentSearchHitColorDark else currentSearchHitColorLight

@Composable
fun logListItemSecondaryColor(): Color =
  if (isDarkThemeOn()) logListItemSecondaryColorDark else logListItemSecondaryColorLight

@Composable
private fun isDarkThemeOn(): Boolean {
  val appThemeSetting = rememberStringSharedPreference(
    key = SettingsPrefKeys.Appearance.KEY_THEME,
    default = SettingsPrefKeys.Appearance.Default.THEME,
  ).value
  return if (appThemeSetting == Theme.AUTO) {
    isSystemInDarkTheme()
  } else {
    appThemeSetting == Theme.DARK
  }
}
