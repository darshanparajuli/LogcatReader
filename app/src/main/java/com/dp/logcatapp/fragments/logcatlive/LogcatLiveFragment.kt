package com.dp.logcatapp.fragments.logcatlive

import android.arch.lifecycle.ViewModelProviders
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.dp.logcat.Log
import com.dp.logcat.LogcatEventListener
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.BaseActivity
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.util.ServiceBinder
import com.dp.logcatapp.util.itemdecorations.ListDividerItemDecoration
import com.dp.logcatapp.util.showToast
import kotlinx.android.synthetic.main.app_bar.*

class LogcatLiveFragment : BaseFragment(), ServiceConnection {
    private lateinit var serviceBinder: ServiceBinder
    private var logcatService: LogcatService? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: LogcatLiveViewModel
    private lateinit var adapter: MyRecyclerViewAdapter
    private lateinit var fab: FloatingActionButton
    private var ignoreScrollEvent = false

    private val logcatEventListener = object : LogcatEventListener {

        override fun onStartEvent() {
            activity.showToast("Logcat started")
            adapter.clear()
        }

        override fun onLogEvent(log: Log) {
            runOnUIThread {
                adapter.addItem(log)
                updateUIOnLogEvent(adapter.itemCount)
            }
        }

        override fun onLogsEvent(logs: List<Log>) {
            adapter.addItems(logs)
            updateUIOnLogEvent(adapter.itemCount)
        }

        override fun onStartFailedEvent() {
            activity.showToast("Failed to run logcat")
        }

        override fun onStopEvent() {
            activity.showToast("Logcat stopped")
        }
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        var lastDy = 0

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy > 0 && lastDy <= 0) {
                fab.show()
            } else if (dy < 0 && lastDy >= 0) {
                fab.hide()
            }
            lastDy = dy
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            val lm = recyclerView.layoutManager as LinearLayoutManager
            when (newState) {
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    viewModel.autoScroll = false
                }
                else -> {
                    val pos = lm.findLastCompletelyVisibleItemPosition()
                    if (ignoreScrollEvent) {
                        if (pos == adapter.itemCount) {
                            ignoreScrollEvent = false
                        }
                        return
                    }

                    viewModel.scrollPosition = pos
                    viewModel.autoScroll = pos >= adapter.itemCount - 1
                    if (viewModel.autoScroll) {
                        fab.hide()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(this)
                .get(LogcatLiveViewModel::class.java)
        adapter = MyRecyclerViewAdapter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        serviceBinder = ServiceBinder(LogcatService::class.java, this)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = LayoutInflater.from(activity)
                .inflate(R.layout.fragment_logcat_live, null, false)

        recyclerView = rootView.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = null
        recyclerView.addItemDecoration(ListDividerItemDecoration(activity,
                ListDividerItemDecoration.VERTICAL_LIST))
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(onScrollListener)

        fab = rootView.findViewById(R.id.fab)
        fab.setOnClickListener {
            fab.hide()
            ignoreScrollEvent = true
            viewModel.autoScroll = true
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        }

        if (viewModel.autoScroll) {
            fab.hide()
        }

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.logcat_live, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // do nothing
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search -> {
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        serviceBinder.bind(activity)
    }

    override fun onStop() {
        super.onStop()
        serviceBinder.unbind(activity)
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView.removeOnScrollListener(onScrollListener)
        logcatService?.setLogcatEventListener(null)
        logcatService?.detachFromActivity(activity as AppCompatActivity)
        serviceBinder.close()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        logcatService = (service as LogcatService.LocalBinder).getLogcatService()
        adapter.addItems(logcatService!!.getLogs())
        updateToolbarSubtitle(adapter.itemCount)
        if (viewModel.autoScroll) {
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        } else {
            recyclerView.scrollToPosition(viewModel.scrollPosition)
        }
        logcatService?.setLogcatEventListener(logcatEventListener)
        logcatService?.attatchToActivity(activity as AppCompatActivity)
    }

    private fun updateToolbarSubtitle(count: Int) {
        if (count > 1) {
            (activity as BaseActivity).toolbar.subtitle = "$count logs"
        } else {
            (activity as BaseActivity).toolbar.subtitle = null
        }
    }

    private fun updateUIOnLogEvent(count: Int) {
        updateToolbarSubtitle(count)
        if (viewModel.autoScroll) {
            recyclerView.scrollToPosition(count - 1)
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        logcatService = null
    }

    companion object {
        val TAG = LogcatLiveFragment::class.qualifiedName
    }
}