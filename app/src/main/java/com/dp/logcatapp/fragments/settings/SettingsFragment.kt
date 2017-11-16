package com.dp.logcatapp.fragments.settings

import android.os.Bundle
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.isDarkThemeOn
import com.dp.logcatapp.util.restartApp
import com.dp.logcatapp.util.showToast

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        val TAG = SettingsFragment::class.qualifiedName
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        setupAppearanceCategory()
        setupLogcatCategory()
        setupAboutCategory()
    }

    private fun setupAppearanceCategory() {
        val sharedPrefs = preferenceScreen.sharedPreferences
        val themePref = findPreference(PreferenceKeys.Appearance.KEY_THEME) as ListPreference
        val useBlackThemePref = findPreference(PreferenceKeys.Appearance.KEY_USE_BLACK_THEME)

        val currTheme = sharedPrefs.getString(PreferenceKeys.Appearance.KEY_THEME,
                PreferenceKeys.Appearance.Default.THEME)

        when (currTheme) {
            PreferenceKeys.Appearance.Theme.AUTO,
            PreferenceKeys.Appearance.Theme.DARK -> useBlackThemePref.isEnabled = true
            else -> useBlackThemePref.isEnabled = false
        }

        val themePrefEntries = resources.getStringArray(R.array.pref_appearance_theme_entries)
        themePref.summary = themePrefEntries[currTheme.toInt()]

        themePref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    preference.summary = themePrefEntries[(newValue as String).toInt()]
                    activity.restartApp()
                    true
                }

        useBlackThemePref.onPreferenceChangeListener = Preference
                .OnPreferenceChangeListener { _, _ ->
                    if (activity.isDarkThemeOn()) {
                        activity.restartApp()
                    }
                    true
                }
    }

    private fun setupLogcatCategory() {
        val pref = findPreference(PreferenceKeys.Logcat.KEY_POLL_INTERVAL)
        pref.summary = preferenceScreen.sharedPreferences
                .getString(PreferenceKeys.Logcat.KEY_POLL_INTERVAL,
                        PreferenceKeys.Logcat.Default.POLL_INTERVAL) + " ms"

        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            try {
                val v = newValue.toString().trim()
                val num = v.toLong()
                if (num <= 0) {
                    activity.showToast("Value must be greater than 0")
                    false
                } else {
                    pref.summary = "$v ms"
                    true
                }
            } catch (e: NumberFormatException) {
                activity.showToast("Value must be a postive integer")
                false
            }
        }
    }

    private fun setupAboutCategory() {
        val pref = findPreference(PreferenceKeys.About.KEY_VERSION_NAME)
        pref.summary = "Version ${BuildConfig.VERSION_NAME}"
    }
}
