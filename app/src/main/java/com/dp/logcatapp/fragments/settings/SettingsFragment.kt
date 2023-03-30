package com.dp.logcatapp.fragments.settings

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.setFragmentResult
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.dp.logcat.Logcat
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.isDarkThemeOn
import com.dp.logcatapp.util.restartApp
import com.dp.logcatapp.util.showToast
import java.text.NumberFormat

class SettingsFragment : PreferenceFragmentCompat() {

  companion object {
    val TAG = SettingsFragment::class.qualifiedName
    private const val SAVE_LOCATION_REQ = 123
    val SAVE_DIALOG = TAG + "_save_dialog"
    val IS_DEFAULT_SAVE_LOCATION = TAG + "_is_default_save_location"
  }

  private lateinit var saveResultLauncher: ActivityResultLauncher<Intent>
  private lateinit var prefSaveLocation: Preference

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    addPreferencesFromResource(R.xml.settings)
    setupAppearanceCategory()
    setupLogcatCategory()
    setupAboutCategory()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    saveResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val uri = data?.data
        if (uri != null) {
          requireActivity().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION
          )
          preferenceScreen.sharedPreferences?.edit {
            putString(
              PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
              uri.toString()
            )
          }
          prefSaveLocation.summary = getString(R.string.save_location_custom)
        }
      }
    }
  }

  private fun setupAppearanceCategory() {
    val sharedPrefs = preferenceScreen.sharedPreferences
    val themePref = findPreference<ListPreference>(PreferenceKeys.Appearance.KEY_THEME)!!
    val useBlackThemePref =
      findPreference<Preference>(PreferenceKeys.Appearance.KEY_USE_BLACK_THEME)!!

    val currTheme = sharedPrefs?.getString(
      PreferenceKeys.Appearance.KEY_THEME,
      PreferenceKeys.Appearance.Default.THEME
    )!!

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
        requireActivity().restartApp()
        true
      }

    useBlackThemePref.onPreferenceChangeListener = Preference
      .OnPreferenceChangeListener { _, _ ->
        val activity = requireActivity()
        if (activity.isDarkThemeOn()) {
          activity.restartApp()
        }
        true
      }
  }

  private fun setupLogcatCategory() {
    val prefPollInterval = findPreference<Preference>(PreferenceKeys.Logcat.KEY_POLL_INTERVAL)!!
    val prefBuffers = findPreference<MultiSelectListPreference>(PreferenceKeys.Logcat.KEY_BUFFERS)!!
    val prefMaxLogs = findPreference<Preference>(PreferenceKeys.Logcat.KEY_MAX_LOGS)!!

    prefPollInterval.summary = preferenceScreen.sharedPreferences
      ?.getString(
        PreferenceKeys.Logcat.KEY_POLL_INTERVAL,
        PreferenceKeys.Logcat.Default.POLL_INTERVAL
      )!! + " ms"

    prefPollInterval.onPreferenceChangeListener = Preference
      .OnPreferenceChangeListener { preference, newValue ->
        val activity = requireActivity()
        try {
          val v = newValue.toString().trim()
          val num = v.toLong()
          if (num <= 0) {
            activity.showToast(getString(R.string.value_must_be_greater_than_0))
            false
          } else {
            preference.summary = "$v ms"
            true
          }
        } catch (e: NumberFormatException) {
          activity.showToast(getString(R.string.value_must_be_a_positive_integer))
          false
        }
      }

    val availableBuffers = Logcat.AVAILABLE_BUFFERS
    val defaultBuffers = PreferenceKeys.Logcat.Default.BUFFERS
    if (availableBuffers.isNotEmpty() && defaultBuffers.isNotEmpty()) {
      val bufferValues = preferenceScreen.sharedPreferences
        ?.getStringSet(PreferenceKeys.Logcat.KEY_BUFFERS, defaultBuffers)!!

      val toSummary = { values: Set<String> ->
        values.map { e -> availableBuffers[e.toInt()] }
          .sorted()
          .joinToString(", ")
      }

      prefBuffers.entries = availableBuffers.copyOf()
      val entryValues = mutableListOf<String>()
      for (i in availableBuffers.indices) {
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

    val maxLogs = preferenceScreen.sharedPreferences?.getString(
      PreferenceKeys.Logcat.KEY_MAX_LOGS,
      PreferenceKeys.Logcat.Default.MAX_LOGS
    )!!.trim().toInt()

    prefMaxLogs.summary = NumberFormat.getInstance().format(maxLogs)
    prefMaxLogs.onPreferenceChangeListener = Preference
      .OnPreferenceChangeListener callback@{ preference, newValue ->
        val activity = requireActivity()
        try {
          val oldValue = preferenceScreen.sharedPreferences?.getString(
            PreferenceKeys.Logcat.KEY_MAX_LOGS,
            PreferenceKeys.Logcat.Default.MAX_LOGS
          )!!.trim().toInt()

          val newMaxLogs = (newValue as String).trim().toInt()
          if (newMaxLogs == oldValue) {
            return@callback false
          }

          if (newMaxLogs < 1000) {
            activity.showToast(getString(R.string.cannot_be_less_than_1000))
            return@callback false
          }

          preference.summary = NumberFormat.getInstance().format(newMaxLogs)
          true
        } catch (e: NumberFormatException) {
          activity.showToast(getString(R.string.not_a_valid_number))
          false
        }
      }

    setupSaveLocationOption()
  }

  private fun setupSaveLocationOption() {
    prefSaveLocation = findPreference<Preference>(PreferenceKeys.Logcat.KEY_SAVE_LOCATION)!!
    val saveLocation = preferenceScreen.sharedPreferences?.getString(
      PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
      PreferenceKeys.Logcat.Default.SAVE_LOCATION
    )!!.trim()
    if (saveLocation.isEmpty()) {
      prefSaveLocation.summary = getString(R.string.save_location_internal)
    } else {
      prefSaveLocation.summary = getString(R.string.save_location_custom)
    }

    prefSaveLocation.setOnPreferenceClickListener {
      val frag = SaveLocationDialogFragment()
      frag.show(parentFragmentManager, SaveLocationDialogFragment.TAG)
      parentFragmentManager.setFragmentResultListener(SAVE_DIALOG, this) { key, bundle ->
        if (key == SAVE_DIALOG) {
          val isDefaultLocation = bundle.getBoolean(IS_DEFAULT_SAVE_LOCATION, false)
          if (isDefaultLocation)
            setupDefaultSaveLocation()
          else
            setupCustomSaveLocation()
        }
      }
      true
    }
  }

  private fun setupCustomSaveLocation() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
    saveResultLauncher.launch(intent)
  }

  private fun setupDefaultSaveLocation() {
    preferenceScreen.sharedPreferences?.edit {
      putString(PreferenceKeys.Logcat.KEY_SAVE_LOCATION, "")
    }
    prefSaveLocation.summary = getString(R.string.save_location_internal)
  }

  private fun setupAboutCategory() {
    val prefAbout = findPreference<Preference>(PreferenceKeys.About.KEY_VERSION_NAME)!!
    prefAbout.summary = "Version ${BuildConfig.VERSION_NAME}"

    val prefGitHubPage = findPreference<Preference>(PreferenceKeys.About.KEY_GITHUB_PAGE)!!
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
      return AlertDialog.Builder(requireActivity())
        .setTitle(R.string.save_location)
        .setItems(R.array.save_location_options) { _, which ->
          val bundle = Bundle()
          if (which == 0)
            bundle.putBoolean(IS_DEFAULT_SAVE_LOCATION, true)
          else
            bundle.putBoolean(IS_DEFAULT_SAVE_LOCATION, false)

          setFragmentResult(SAVE_DIALOG, bundle)
        }
        .create()
    }
  }
}
