package com.dp.logcatapp.fragments.settings

import android.annotation.TargetApi
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v14.preference.MultiSelectListPreference
import android.support.v7.app.AlertDialog
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import androidx.core.content.edit
import com.dp.logcat.Logcat
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.settings.dialogs.FolderChooserDialogFragment
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.isDarkThemeOn
import com.dp.logcatapp.util.restartApp
import com.dp.logcatapp.util.showToast
import java.io.File
import java.text.NumberFormat

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        val TAG = SettingsFragment::class.qualifiedName
    }

    private lateinit var prefSaveLocation: Preference

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
                    activity!!.restartApp()
                    true
                }

        useBlackThemePref.onPreferenceChangeListener = Preference
                .OnPreferenceChangeListener { _, _ ->
                    if (activity!!.isDarkThemeOn()) {
                        activity!!.restartApp()
                    }
                    true
                }
    }

    private fun setupLogcatCategory() {
        val prefPollInterval = findPreference(PreferenceKeys.Logcat.KEY_POLL_INTERVAL)
        val prefBuffers = findPreference(PreferenceKeys.Logcat.KEY_BUFFERS)
                as MultiSelectListPreference
        val prefMaxLogs = findPreference(PreferenceKeys.Logcat.KEY_MAX_LOGS)

        prefPollInterval.summary = preferenceScreen.sharedPreferences
                .getString(PreferenceKeys.Logcat.KEY_POLL_INTERVAL,
                        PreferenceKeys.Logcat.Default.POLL_INTERVAL) + " ms"

        prefPollInterval.onPreferenceChangeListener = Preference
                .OnPreferenceChangeListener { preference, newValue ->
                    try {
                        val v = newValue.toString().trim()
                        val num = v.toLong()
                        if (num <= 0) {
                            activity!!.showToast(getString(R.string.value_must_be_greater_than_0))
                            false
                        } else {
                            preference.summary = "$v ms"
                            true
                        }
                    } catch (e: NumberFormatException) {
                        activity!!.showToast(getString(R.string.value_must_be_a_positive_integer))
                        false
                    }
                }

        val availableBuffers = Logcat.AVAILABLE_BUFFERS
        val defaultBuffers = PreferenceKeys.Logcat.Default.BUFFERS
        if (availableBuffers.isNotEmpty() && defaultBuffers.isNotEmpty()) {
            val bufferValues = preferenceScreen.sharedPreferences
                    .getStringSet(PreferenceKeys.Logcat.KEY_BUFFERS, defaultBuffers)

            val toSummary = { values: Set<String> ->
                values.map { e -> availableBuffers[e.toInt()] }
                        .sorted()
                        .joinToString(", ")
            }

            prefBuffers.entries = availableBuffers.copyOf()
            val entryValues = mutableListOf<String>()
            for (i in 0 until availableBuffers.size) {
                entryValues += i.toString()
            }
            prefBuffers.entryValues = entryValues.toTypedArray()
            prefBuffers.summary = toSummary(bufferValues)

            @Suppress("unchecked_cast")
            prefBuffers.onPreferenceChangeListener = Preference
                    .OnPreferenceChangeListener { preference, newValue ->
                        val mp = preference as MultiSelectListPreference
                        val values = newValue as Set<String>

                        if (values.isEmpty()) {
                            false
                        } else {
                            mp.summary = toSummary(values)
                            true
                        }
                    }
        } else {
            prefBuffers.isVisible = false
        }

        val maxLogs = preferenceScreen.sharedPreferences.getString(
                PreferenceKeys.Logcat.KEY_MAX_LOGS,
                PreferenceKeys.Logcat.Default.MAX_LOGS
        ).trim().toInt()

        prefMaxLogs.summary = NumberFormat.getInstance().format(maxLogs)
        prefMaxLogs.onPreferenceChangeListener = Preference
                .OnPreferenceChangeListener callback@{ preference, newValue ->
                    try {
                        val oldValue = preferenceScreen.sharedPreferences.getString(
                                PreferenceKeys.Logcat.KEY_MAX_LOGS,
                                PreferenceKeys.Logcat.Default.MAX_LOGS
                        ).trim().toInt()

                        val newMaxLogs = (newValue as String).trim().toInt()
                        if (newMaxLogs == oldValue) {
                            return@callback false
                        }

                        if (newMaxLogs < 1000) {
                            activity!!.showToast(getString(R.string.cannot_be_less_than_1000))
                            return@callback false
                        }

                        preference.summary = NumberFormat.getInstance().format(newMaxLogs)
                        true
                    } catch (e: NumberFormatException) {
                        activity!!.showToast(getString(R.string.not_a_valid_number))
                        false
                    }
                }

        setupSaveLocationOption()
    }

    private fun setupSaveLocationOption() {
        prefSaveLocation = findPreference(PreferenceKeys.Logcat.KEY_SAVE_LOCATION)
        val saveLocation = preferenceScreen.sharedPreferences.getString(
                PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
                PreferenceKeys.Logcat.Default.SAVE_LOCATION
        ).trim()
        if (saveLocation.isEmpty()) {
            prefSaveLocation.summary = getString(R.string.save_location_internal)
        } else {
            prefSaveLocation.summary = saveLocation
        }

        prefSaveLocation.setOnPreferenceClickListener {
            val frag = SaveLocationDialogFragment()
            frag.setTargetFragment(this@SettingsFragment, 0)
            frag.show(fragmentManager, SaveLocationDialogFragment.TAG)
            true
        }

        val frag = fragmentManager?.findFragmentByTag(SaveLocationDialogFragment.TAG)
        frag?.setTargetFragment(this, 0)

        val folderChooserFragment = fragmentManager
                ?.findFragmentByTag(FolderChooserDialogFragment.TAG)
        folderChooserFragment?.setTargetFragment(this, 0)
    }
    private fun isExternalStorageWritable() =
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    private fun setupCustomSaveLocation() {
        if (Build.VERSION.SDK_INT >= 19) {
            setupCustomSaveLocationKitkat()
        } else {
            if (isExternalStorageWritable()) {
                val frag = FolderChooserDialogFragment()
                frag.setTargetFragment(this, 0)
                frag.show(fragmentManager, FolderChooserDialogFragment.TAG)
            } else {
                activity!!.showToast(getString(R.string.err_msg_external_storage_not_writable))
            }
        }
    }

    fun setupCustomSaveLocationPreKitkat(file: File?) {
        if (file == null) {
            activity!!.showToast("Folder not selected")
        } else {
            if (!file.canWrite()) {
                activity!!.showToast("Folder not writable")
                return
            }

            preferenceScreen.sharedPreferences.edit {
                putString(PreferenceKeys.Logcat.KEY_SAVE_LOCATION, file.absolutePath)
            }
            prefSaveLocation.summary = file.absolutePath
        }
    }

    @TargetApi(19)
    private fun setupCustomSaveLocationKitkat() {
        // TODO(darshan): Use document API for setting a path uri
    }

    private fun setupDefaultSaveLocation() {
        preferenceScreen.sharedPreferences.edit {
            putString(PreferenceKeys.Logcat.KEY_SAVE_LOCATION, "")
        }
        prefSaveLocation.summary = getString(R.string.save_location_internal)
    }

    private fun setupAboutCategory() {
        val prefAbout = findPreference(PreferenceKeys.About.KEY_VERSION_NAME)
        prefAbout.summary = "Version ${BuildConfig.VERSION_NAME}"

        val prefGitHubPage = findPreference(PreferenceKeys.About.KEY_GITHUB_PAGE)
        prefGitHubPage.setOnPreferenceClickListener { _ ->
            try {
                val url = "https://github.com/darshanparajuli/LogcatReader"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    class SaveLocationDialogFragment : BaseDialogFragment() {
        companion object {
            val TAG = SaveLocationDialogFragment::class.qualifiedName
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(activity!!)
                    .setTitle(R.string.save_location)
                    .setItems(R.array.save_location_options, { _, which ->
                        if (which == 0) {
                            (targetFragment as SettingsFragment).setupDefaultSaveLocation()
                        } else {
                            (targetFragment as SettingsFragment).setupCustomSaveLocation()
                        }
                    })
                    .create()
        }
    }
}
