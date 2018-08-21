package com.dp.logcatapp.fragments.logcatlive

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.provider.DocumentFile
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.dp.logcat.Filter
import com.dp.logcat.Log
import com.dp.logcat.Logcat
import com.dp.logcat.LogcatEventListener
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.BaseActivityWithToolbar
import com.dp.logcatapp.activities.FiltersActivity
import com.dp.logcatapp.activities.SavedLogsActivity
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.MyDB
import com.dp.logcatapp.db.SavedLogInfo
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.filters.FilterType
import com.dp.logcatapp.fragments.logcatlive.dialogs.InstructionToGrantPermissionDialogFragment
import com.dp.logcatapp.fragments.shared.dialogs.CopyToClipboardDialogFragment
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.util.*
import com.dp.logcatapp.views.IndeterminateProgressSnackBar
import com.dp.logger.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class LogcatLiveFragment : BaseFragment(), ServiceConnection, LogcatEventListener {
    companion object {
        val TAG = LogcatLiveFragment::class.qualifiedName
        const val LOGCAT_DIR = "logcat"

        private const val SEARCH_FILTER_TAG = "search_filter_tag"

        private val STOP_RECORDING = TAG + "_stop_recording"

        fun newInstance(stopRecording: Boolean): LogcatLiveFragment {
            val bundle = Bundle()
            bundle.putBoolean(STOP_RECORDING, stopRecording)
            val frag = LogcatLiveFragment()
            frag.arguments = bundle
            return frag
        }
    }

    private lateinit var serviceBinder: ServiceBinder
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var viewModel: LogcatLiveViewModel
    private lateinit var adapter: MyRecyclerViewAdapter
    private lateinit var fabUp: FloatingActionButton
    private lateinit var fabDown: FloatingActionButton
    private var logcatService: LogcatService? = null
    private var ignoreScrollEvent = false
    private var searchViewActive = false
    private var lastLogId = -1
    private var lastSearchRunnable: Runnable? = null
    private var searchTask: SearchTask? = null
    private var filterSubscription: Disposable? = null

    private val hideFabUpRunnable: Runnable = Runnable {
        fabUp.hide()
    }

    private val hideFabDownRunnable: Runnable = Runnable {
        fabDown.hide()
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        var lastDy = 0

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy > 0 && lastDy <= 0) {
                hideFabUp()
                showFabDown()
            } else if (dy < 0 && lastDy >= 0) {
                showFabUp()
                hideFabDown()
            }
            lastDy = dy
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            when (newState) {
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    viewModel.autoScroll = false
                    if (lastDy > 0) {
                        hideFabUp()
                        showFabDown()
                    } else if (lastDy < 0) {
                        showFabUp()
                        hideFabDown()
                    }
                }
                else -> {
                    var firstPos = -1
                    if (searchViewActive && !viewModel.autoScroll &&
                            newState == RecyclerView.SCROLL_STATE_IDLE) {
                        firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                        if (firstPos != RecyclerView.NO_POSITION) {
                            val log = adapter[firstPos]
                            lastLogId = log.id
                        }
                    }

                    val pos = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                    if (pos == RecyclerView.NO_POSITION) {
                        viewModel.autoScroll = false
                        return
                    }

                    if (ignoreScrollEvent) {
                        if (pos == adapter.itemCount) {
                            ignoreScrollEvent = false
                        }
                        return
                    }

                    if (pos == 0) {
                        hideFabUp()
                    }

                    if (firstPos == RecyclerView.NO_POSITION) {
                        firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                    }

                    viewModel.scrollPosition = firstPos
                    viewModel.autoScroll = pos == adapter.itemCount - 1

                    if (viewModel.autoScroll) {
                        hideFabUp()
                        hideFabDown()
                    }
                }
            }
        }
    }

    private fun showFabUp() {
        handler.removeCallbacks(hideFabUpRunnable)
        fabUp.show()
        handler.postDelayed(hideFabUpRunnable, 2000)
    }

    private fun hideFabUp() {
        handler.removeCallbacks(hideFabUpRunnable)
        fabUp.hide()
    }

    private fun showFabDown() {
        handler.removeCallbacks(hideFabDownRunnable)
        fabDown.show()
        handler.postDelayed(hideFabDownRunnable, 2000)
    }

    private fun hideFabDown() {
        handler.removeCallbacks(hideFabDownRunnable)
        fabDown.hide()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        serviceBinder = ServiceBinder(LogcatService::class.java, this)

        val maxLogs = activity!!.getDefaultSharedPreferences()
                .getString(PreferenceKeys.Logcat.KEY_MAX_LOGS,
                        PreferenceKeys.Logcat.Default.MAX_LOGS)!!.trim().toInt()
        adapter = MyRecyclerViewAdapter(activity!!, maxLogs)
        activity!!.getDefaultSharedPreferences().registerOnSharedPreferenceChangeListener(adapter)

        viewModel = ViewModelProviders.of(activity!!)
                .get(LogcatLiveViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflateLayout(R.layout.fragment_logcat_live)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        linearLayoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.itemAnimator = null
        recyclerView.addItemDecoration(DividerItemDecoration(activity,
                linearLayoutManager.orientation))
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(onScrollListener)

        fabDown = view.findViewById(R.id.fabDown)
        fabDown.setOnClickListener {
            logcatService?.logcat?.pause()
            hideFabDown()
            ignoreScrollEvent = true
            viewModel.autoScroll = true
            linearLayoutManager.scrollToPosition(adapter.itemCount - 1)
            resumeLogcat()
        }

        fabUp = view.findViewById(R.id.fabUp)
        fabUp.setOnClickListener {
            logcatService?.logcat?.pause()
            hideFabUp()
            viewModel.autoScroll = false
            linearLayoutManager.scrollToPositionWithOffset(0, 0)
            resumeLogcat()
        }

        hideFabUp()
        hideFabDown()

        adapter.setOnClickListener { v ->
            val pos = linearLayoutManager.getPosition(v)
            if (pos >= 0) {
                viewModel.autoScroll = false
                val log = adapter[pos]
                CopyToClipboardDialogFragment.newInstance(log)
                        .show(fragmentManager, CopyToClipboardDialogFragment.TAG)
            }
        }

        if (!checkReadLogsPermission() && !viewModel.showedGrantPermissionInstruction) {
            viewModel.showedGrantPermissionInstruction = true
            InstructionToGrantPermissionDialogFragment().show(fragmentManager,
                    InstructionToGrantPermissionDialogFragment.TAG)
        }
    }

    private fun checkReadLogsPermission() =
            ContextCompat.checkSelfPermission(activity!!,
                    Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.logcat_live, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        var reachedBlank = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                searchViewActive = true
                removeLastSearchRunnableCallback()

                if (newText.isBlank()) {
                    reachedBlank = true
                    onSearchViewClose()
                } else {
                    reachedBlank = false
                    val logcat = logcatService?.logcat ?: return true

                    lastSearchRunnable = Runnable {
                        onSearchAction(logcat, newText)
                    }

                    handler.postDelayed(lastSearchRunnable, 300)
                }
                return true
            }

            override fun onQueryTextSubmit(query: String) = false
        })

        val playPauseItem = menu.findItem(R.id.action_play_pause)
        val recordToggleItem = menu.findItem(R.id.action_record_toggle)

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            playPauseItem.isVisible = !hasFocus
            recordToggleItem.isVisible = !hasFocus
        }

        searchView.setOnCloseListener {
            removeLastSearchRunnableCallback()
            searchViewActive = false
            if (!reachedBlank) {
                onSearchViewClose()
            }
            playPauseItem.isVisible = true
            recordToggleItem.isVisible = true
            false
        }
    }

    private fun onSearchAction(logcat: Logcat, newText: String) {
        Logger.logDebug(LogcatLiveFragment::class, "onSearchAction: $newText")
        searchTask?.cancel(true)
        searchTask = SearchTask(this, logcat, newText)
        searchTask!!.execute()
    }

    private fun onSearchViewClose() {
        val logcat = logcatService?.logcat ?: return
        logcat.pause()
        logcat.removeFilter(SEARCH_FILTER_TAG)

        adapter.clear()
        addAllLogs(logcat.getLogsFiltered())
        if (lastLogId == -1) {
            scrollRecyclerView()
        } else {
            viewModel.autoScroll = linearLayoutManager.findLastCompletelyVisibleItemPosition() ==
                    adapter.itemCount - 1
            if (!viewModel.autoScroll) {
                viewModel.scrollPosition = lastLogId
                linearLayoutManager.scrollToPositionWithOffset(lastLogId, 0)
            }
            lastLogId = -1
        }

        resumeLogcat()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // do nothing

        val playPauseItem = menu.findItem(R.id.action_play_pause)
        val recordToggleItem = menu.findItem(R.id.action_record_toggle)

        if (logcatService?.paused == true) {
            playPauseItem.icon = ContextCompat.getDrawable(activity!!,
                    R.drawable.ic_play_arrow_white_24dp)
            playPauseItem.title = getString(R.string.resume)
        } else {
            playPauseItem.icon = ContextCompat.getDrawable(activity!!,
                    R.drawable.ic_pause_white_24dp)
            playPauseItem.title = getString(R.string.pause)

            if (logcatService?.recording == true) {
                recordToggleItem.icon = ContextCompat.getDrawable(activity!!,
                        R.drawable.ic_stop_white_24dp)
                recordToggleItem.title = getString(R.string.stop_recording)
            } else {
                recordToggleItem.icon = ContextCompat.getDrawable(activity!!,
                        R.drawable.ic_fiber_manual_record_white_24dp)
                recordToggleItem.title = getString(R.string.start_recording)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                true
            }
            R.id.action_play_pause -> {
                val logcat = logcatService?.logcat
                if (logcat != null) {
                    val newPausedState = !logcatService!!.paused
                    if (newPausedState) {
                        logcat.pause()
                    } else {
                        logcat.resume()
                    }
                    logcatService!!.paused = newPausedState
                    activity?.invalidateOptionsMenu()
                }
                true
            }
            R.id.action_record_toggle -> {
                val recording = !logcatService!!.recording
                logcatService?.updateNotification(recording)
                val logcat = logcatService?.logcat
                if (logcat != null) {
                    if (recording) {
                        Snackbar.make(view!!, getString(R.string.started_recording),
                                Snackbar.LENGTH_SHORT)
                                .show()
                        logcat.startRecording()
                    } else {
                        val logs = logcat.stopRecording()
                        saveToFile(logs)
                    }
                    logcatService!!.recording = recording
                    activity?.invalidateOptionsMenu()
                }
                true
            }
            R.id.clear_action -> {
                logcatService?.logcat?.clearLogs {
                    adapter.clear()
                    updateToolbarSubtitle(adapter.itemCount)
                }
                true
            }
            R.id.filters_action -> {
                val intent = Intent(activity!!, FiltersActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.exclusions_action -> {
                val intent = Intent(activity!!, FiltersActivity::class.java)
                intent.putExtra(FiltersActivity.EXTRA_EXCLUSIONS, true)
                startActivity(intent)
                true
            }
            R.id.action_save -> {
                trySaveToFile()
                true
            }
            R.id.action_view_saved_logs -> {
                startActivity(Intent(activity, SavedLogsActivity::class.java))
                true
            }
            R.id.action_restart_logcat -> {
                adapter.clear()
                logcatService?.logcat?.restart()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun tryStopRecording() {
        viewModel.stopRecording = true
        if (logcatService != null) {
            stopRecording()
        }
    }

    private fun stopRecording() {
        if (logcatService?.recording == true) {
            logcatService?.updateNotification(false)

            val logcat = logcatService?.logcat
            if (logcat != null) {
                val logs = logcat.stopRecording()
                saveToFile(logs)
            }

            logcatService!!.recording = false
            activity?.invalidateOptionsMenu()
        }
        viewModel.stopRecording = false
    }

    private fun isUsingCustomSaveLocation() =
            activity!!.getDefaultSharedPreferences().getString(
                    PreferenceKeys.Logcat.KEY_SAVE_LOCATION, "")!!.isNotEmpty()

    private fun saveToFile(logs: List<Log>) {
        val timeStamp = SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.getDefault())
                .format(Date())
        val fileName = "logcat_$timeStamp"

        if (logs.isEmpty()) {
            Logger.logDebug(LogcatLiveFragment::class, "Nothing to save")
            showSnackbar(view, getString(R.string.nothing_to_save))
            return
        }

        val saveLocationPref = activity!!.getDefaultSharedPreferences().getString(
                PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
                PreferenceKeys.Logcat.Default.SAVE_LOCATION
        )!!

        val uri = if (saveLocationPref.isEmpty()) {
            val file = File(context!!.filesDir, LOGCAT_DIR)
            file.mkdirs()
            File(file, "$fileName.txt").toUri()
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                val documentFile = DocumentFile.fromTreeUri(context!!, Uri.parse(saveLocationPref))
                val file = documentFile?.createFile("text/plain", fileName)
                file?.uri
            } else {
                val file = File(saveLocationPref, LOGCAT_DIR)
                file.mkdirs()
                File(file, "$fileName.txt").toUri()
            }
        }

        SaveFileTask(this, uri, logs).execute()
    }

    private fun viewSavedLog(uri: Uri): Boolean {
        val intent = Intent(context, SavedLogsViewerActivity::class.java)
        intent.setDataAndType(uri, "text/plain")
        startActivity(intent)
        return true
    }

    private fun trySaveToFile() {
        val logcat = logcatService?.logcat
        if (logcat != null) {
            saveToFile(logcat.getLogsFiltered())
        }
    }

    override fun onStart() {
        super.onStart()
        serviceBinder.bind(activity!!)
    }

    override fun onStop() {
        super.onStop()
        filterSubscription?.dispose()
        serviceBinder.unbind(activity!!)
    }

    private fun removeLastSearchRunnableCallback() {
        if (lastSearchRunnable != null) {
            handler.removeCallbacks(lastSearchRunnable)
            lastSearchRunnable = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity!!.getDefaultSharedPreferences().unregisterOnSharedPreferenceChangeListener(adapter)

        removeLastSearchRunnableCallback()
        searchTask?.cancel(true)

        recyclerView.removeOnScrollListener(onScrollListener)
        logcatService?.logcat?.setEventListener(null)
        logcatService?.logcat?.unbind(activity as AppCompatActivity)
        serviceBinder.close()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        Logger.logDebug(LogcatLiveFragment::class, "onServiceConnected")
        logcatService = (service as LogcatService.LocalBinder).getLogcatService()
        val logcat = logcatService!!.logcat
        logcat.pause() // resume on updateFilters callback

        if (adapter.itemCount == 0) {
            Logger.logDebug(LogcatLiveFragment::class, "Added all logs")
            addAllLogs(logcat.getLogsFiltered())
        } else if (logcatService!!.restartedLogcat) {
            Logger.logDebug(LogcatLiveFragment::class, "Logcat restarted")
            logcatService!!.restartedLogcat = false
            adapter.clear()
        }

        scrollRecyclerView()

        logcat.setEventListener(this)
        logcat.bind(activity as AppCompatActivity)

        if (viewModel.stopRecording || arguments?.getBoolean(STOP_RECORDING) == true) {
            arguments?.putBoolean(STOP_RECORDING, false)
            stopRecording()
        }

        updateFilters()
    }

    private fun updateFilters() {
        filterSubscription = MyDB.getInstance(activity!!)
                .filterDao()
                .getAll()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val logcat = logcatService?.logcat
                    if (logcat != null) {
                        logcat.pause()
                        logcat.clearFilters()
                        logcat.clearExclusions()

                        for (filter in it) {
                            if (filter.exclude) {
                                logcat.addExclusion("${filter.hashCode()}",
                                        LogFilter(filter))
                            } else {
                                logcat.addFilter("${filter.hashCode()}",
                                        LogFilter(filter))
                            }
                        }

                        adapter.clear()
                        adapter.addItems(logcat.getLogsFiltered())
                        updateToolbarSubtitle(adapter.itemCount)
                        scrollRecyclerView()
                        resumeLogcat()
                    }
                }
    }

    override fun onLogEvents(logs: List<Log>) {
        adapter.addItems(logs)
        updateToolbarSubtitle(adapter.itemCount)
        if (viewModel.autoScroll) {
            linearLayoutManager.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun addAllLogs(logs: List<Log>) {
        adapter.addItems(logs)
        updateToolbarSubtitle(adapter.itemCount)
    }

    private fun scrollRecyclerView() {
        if (viewModel.autoScroll) {
            linearLayoutManager.scrollToPosition(adapter.itemCount - 1)
        } else {
            linearLayoutManager.scrollToPositionWithOffset(viewModel.scrollPosition, 0)
        }
    }

    private fun updateToolbarSubtitle(count: Int) {
        if (count > 1) {
            (activity as BaseActivityWithToolbar).toolbar.subtitle = "$count"
        } else {
            (activity as BaseActivityWithToolbar).toolbar.subtitle = null
        }
    }

    private fun resumeLogcat() {
        if (logcatService != null && !logcatService!!.paused) {
            logcatService?.logcat?.resume()
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Logger.logDebug(LogcatLiveFragment::class, "onServiceDisconnected")
        logcatService = null
    }

    private class SearchTask(fragment: LogcatLiveFragment,
                             val logcat: Logcat, val searchText: String) :
            AsyncTask<String, Void, List<Log>>() {

        private var fragRef: WeakReference<LogcatLiveFragment> = WeakReference(fragment)

        override fun onPreExecute() {
            logcat.pause()
            logcat.addFilter(SEARCH_FILTER_TAG, object : Filter {
                override fun apply(log: Log): Boolean {
                    return log.tag.containsIgnoreCase(searchText) ||
                            log.msg.containsIgnoreCase(searchText)
                }
            })
        }

        override fun doInBackground(vararg params: String?): List<Log> =
                logcat.getLogsFiltered()

        override fun onCancelled(result: List<Log>?) {
            fragRef.get()?.resumeLogcat()
        }

        override fun onPostExecute(result: List<Log>?) {
            val frag = fragRef.get() ?: return
            if (result != null) {
                frag.adapter.clear()
                frag.adapter.addItems(result)
                frag.viewModel.autoScroll = false
                frag.linearLayoutManager.scrollToPositionWithOffset(0, 0)
            }
        }
    }

    private class LogFilter(filterInfo: FilterInfo) : Filter {
        val type = filterInfo.type
        val content = filterInfo.content
        lateinit var logLevels: MutableSet<String>

        init {
            if (filterInfo.type == FilterType.LOG_LEVELS) {
                logLevels = mutableSetOf()
                filterInfo.content.split(",")
                        .filter { it.isNotEmpty() }
                        .forEach {
                            logLevels.add(it)
                        }
            }
        }

        override fun apply(log: Log): Boolean {
            if (content.isEmpty()) {
                return true
            }

            when (type) {
                FilterType.LOG_LEVELS -> {
                    return content.contains(log.priority)
                }
                FilterType.KEYWORD -> {
                    return log.msg.containsIgnoreCase(content)
                }
                FilterType.TAG -> {
                    return log.tag.containsIgnoreCase(content)
                }
                FilterType.PID -> {
                    return log.pid.containsIgnoreCase(content)
                }
                FilterType.TID -> {
                    return log.tid.containsIgnoreCase(content)
                }
                else -> {
                    return false
                }
            }
        }
    }

    class SaveFileTask(frag: LogcatLiveFragment,
                       private val uri: Uri?,
                       private val logs: List<Log>) : AsyncTask<Void, Void, String?>() {

        private val ref = WeakReference(frag)
        private val snackBarProgress = IndeterminateProgressSnackBar(frag.view!!,
                frag.getString(R.string.saving))
        private val db = MyDB.getInstance(frag.context!!)
        private val isUsingCustomLocation = frag.isUsingCustomSaveLocation()

        override fun onPreExecute() {
            snackBarProgress.show()
        }

        override fun doInBackground(vararg params: Void?): String? {
            var fileName: String? = null
            val frag = ref.get()
            if (frag != null && uri != null) {
                val context = frag.context!!
                val result: Boolean
                if (isUsingCustomLocation && Build.VERSION.SDK_INT >= 21) {
                    result = Logcat.writeToFile(context, logs, uri)
                } else {
                    result = Logcat.writeToFile(logs, uri.toFile())
                }


                if (result && frag.activity != null) {
                    fileName = if (isUsingCustomLocation && Build.VERSION.SDK_INT >= 21) {
                        DocumentFile.fromSingleUri(frag.activity!!, uri)?.name
                    } else {
                        uri.toFile().name
                    }
                    if (fileName != null) {
                        db.savedLogsDao().insert(SavedLogInfo(fileName, uri.toString(),
                                isUsingCustomLocation))
                    }
                }
            }

            return fileName
        }

        override fun onPostExecute(fileName: String?) {
            snackBarProgress.dismiss()
            val frag = ref.get() ?: return
            if (frag.activity == null) {
                return
            }

            if (fileName != null && uri != null) {
                newSnakcbar(frag.view, frag.getString(R.string.saved_as_filename).format(fileName),
                        Snackbar.LENGTH_LONG)
                        ?.setAction(frag.getString(R.string.view_log)) {
                            if (!frag.viewSavedLog(uri)) {
                                showSnackbar(frag.view, frag.getString(R.string.could_not_open_log_file))
                            }
                        }?.show()
            } else {
                showSnackbar(frag.view, frag.getString(R.string.failed_to_save_logs))
            }
        }
    }
}