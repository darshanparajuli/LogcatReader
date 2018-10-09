package com.dp.logcatapp.fragments.logcatlive

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.filters.FilterType
import com.dp.logcatapp.fragments.logcatlive.dialogs.InstructionToGrantPermissionDialogFragment
import com.dp.logcatapp.fragments.shared.dialogs.CopyToClipboardDialogFragment
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.util.*
import com.dp.logcatapp.views.IndeterminateProgressSnackBar
import com.dp.logger.Logger
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.Dispatchers.Default
import kotlinx.coroutines.experimental.Dispatchers.Main

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
    private lateinit var snackBarProgress: IndeterminateProgressSnackBar
    private var logcatService: LogcatService? = null
    private var ignoreScrollEvent = false
    private var searchViewActive = false
    private var lastLogId = -1
    private var lastSearchRunnable: Runnable? = null
    private var searchTask: Job? = null
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
        viewModel.getFileSaveNotifier().observe(this, Observer { saveInfo ->
            saveInfo?.let {
                when (it.result) {
                    SaveInfo.IN_PROGRESS -> {
                        snackBarProgress.show()
                    }
                    else -> {
                        snackBarProgress.dismiss()
                        when (it.result) {
                            SaveInfo.SUCCESS -> {
                                OnSavedBottomSheetDialogFragment.newInstance(it.fileName!!, it.uri!!)
                                        .show(fragmentManager, OnSavedBottomSheetDialogFragment.TAG)
                            }
                            SaveInfo.ERROR_EMPTY_LOGS -> {
                                showSnackbar(view, getString(R.string.nothing_to_save))
                            }
                            else -> {
                                showSnackbar(view, getString(R.string.failed_to_save_logs))
                            }
                        }
                    }
                }
            }
        })
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

        snackBarProgress = IndeterminateProgressSnackBar(view, getString(R.string.saving))

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
                    logcatService?.logcat?.let {
                        lastSearchRunnable = Runnable {
                            runSearchTask(it, newText)
                        }

                        handler.postDelayed(lastSearchRunnable, 100)
                    }

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

    private fun onSearchViewClose() {
        logcatService?.logcat?.let {
            it.pause()
            it.removeFilter(SEARCH_FILTER_TAG)

            adapter.clear()
            addAllLogs(it.getLogsFiltered())
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
                logcatService?.let {
                    val newPausedState = !it.paused
                    if (newPausedState) {
                        it.logcat.pause()
                    } else {
                        it.logcat.resume()
                    }
                    it.paused = newPausedState
                    activity?.invalidateOptionsMenu()
                }
                true
            }
            R.id.action_record_toggle -> {
                logcatService?.let {
                    val recording = !it.recording
                    it.updateNotification(recording)

                    val logcat = it.logcat
                    if (recording) {
                        Snackbar.make(view!!, getString(R.string.started_recording),
                                Snackbar.LENGTH_SHORT)
                                .show()
                        logcat.startRecording()
                    } else {
                        saveToFile(true)
                    }

                    it.recording = recording
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
                saveToFile(false)
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
        logcatService?.let {
            if (it.recording) {
                it.updateNotification(false)
                saveToFile(true)
                it.recording = false
                activity?.invalidateOptionsMenu()
            }
        }
        viewModel.stopRecording = false
    }

    private fun saveToFile(recorded: Boolean) {
        viewModel.save {
            if (recorded) {
                logcatService?.logcat?.stopRecording()
            } else {
                logcatService?.logcat?.getLogsFiltered()
            }
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
        searchTask?.cancel()

        recyclerView.removeOnScrollListener(onScrollListener)
        logcatService?.let {
            it.logcat.setEventListener(null)
            it.logcat.unbind(activity as AppCompatActivity)
        }
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
                .subscribe { filterInfoList ->
                    logcatService?.let {
                        val logcat = it.logcat
                        logcat.pause()
                        logcat.clearFilters(exclude = SEARCH_FILTER_TAG)
                        logcat.clearExclusions()

                        for (filter in filterInfoList) {
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
        logcatService?.let {
            if (!it.paused) {
                it.logcat.resume()
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Logger.logDebug(LogcatLiveFragment::class, "onServiceDisconnected")
        logcatService = null
    }

    private class SearchFilter(private val searchText: String) : Filter {
        override fun apply(log: Log): Boolean {
            return log.tag.containsIgnoreCase(searchText) ||
                    log.msg.containsIgnoreCase(searchText)
        }

    }

    private fun runSearchTask(logcat: Logcat, searchText: String) {
        searchTask?.cancel()
        searchTask = GlobalScope.launch(Main) {
            logcat.pause()
            logcat.addFilter(SEARCH_FILTER_TAG, SearchFilter(searchText))

            val filteredLogs = async(Default) { logcat.getLogsFiltered() }.await()
            adapter.clear()
            adapter.addItems(filteredLogs)
            viewModel.autoScroll = false
            linearLayoutManager.scrollToPositionWithOffset(0, 0)
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
}

class OnSavedBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        val TAG = OnSavedBottomSheetDialogFragment::class.qualifiedName

        private val KEY_URI = TAG + "_uri"
        private val KEY_FILE_NAME = TAG + "_file_name"

        fun newInstance(fileName: String, uri: Uri): OnSavedBottomSheetDialogFragment {
            val fragment = OnSavedBottomSheetDialogFragment()
            val bundle = Bundle()
            bundle.putString(KEY_URI, uri.toString())
            bundle.putString(KEY_FILE_NAME, fileName)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.saved_log_bottom_sheet, container, false)

        val arguments = arguments!!
        val fileName = arguments.getString(KEY_FILE_NAME)
        val uri = Uri.parse(arguments.getString(KEY_URI))

        rootView.findViewById<TextView>(R.id.savedFileName).text = fileName
        rootView.findViewById<TextView>(R.id.actionView).setOnClickListener {
            if (!viewSavedLog(uri)) {
                showSnackbar(view, getString(R.string.could_not_open_log_file))
            }
            dismiss()
        }

        rootView.findViewById<TextView>(R.id.actionShare).setOnClickListener {
            ShareUtils.shareSavedLogs(context!!, uri, Utils.isUsingCustomSaveLocation(context!!))
            dismiss()
        }

        return rootView
    }

    private fun viewSavedLog(uri: Uri): Boolean {
        val intent = Intent(context, SavedLogsViewerActivity::class.java)
        intent.setDataAndType(uri, "text/plain")
        startActivity(intent)
        return true
    }
}
