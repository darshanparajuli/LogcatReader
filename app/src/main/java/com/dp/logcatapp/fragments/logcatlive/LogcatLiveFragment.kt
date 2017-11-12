package com.dp.logcatapp.fragments.logcatlive

import android.arch.lifecycle.ViewModelProviders
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.TextView
import com.dp.logcat.Log
import com.dp.logcat.LogcatEventListener
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.SettingsActivity
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.util.ServiceBinder

class LogcatLiveFragment : BaseFragment(), ServiceConnection {
    private var serviceBinder: ServiceBinder? = null
    private var logcatService: LogcatService? = null
    private var viewModel: LogcatLiveViewModel? = null
    private val logcatEventListener = object : LogcatEventListener {
        override fun onStartEvent() {
        }

        override fun onLogEvent(log: Log) {
            handler.post {
                viewModel?.logs?.add(log)
            }
        }

        override fun onFailEvent() {
        }

        override fun onStopEvent() {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(this)
                .get(LogcatLiveViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        serviceBinder = ServiceBinder(LogcatService::class.java, this)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = LayoutInflater.from(activity)
                .inflate(R.layout.fragment_logcat_live, null, false)

        val tv = rootView.findViewById<TextView>(R.id.textview)

        return rootView
    }

    override fun onStart() {
        super.onStart()
        serviceBinder?.bind(activity)
    }

    override fun onStop() {
        super.onStop()
        serviceBinder?.unbind(activity)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceBinder?.close()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.logcat_live, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        val startRecording = menu?.findItem(R.id.start_recording)
        val stopRecording = menu?.findItem(R.id.stop_recording)

        startRecording?.isVisible = !(viewModel?.isRecording ?: false)
        stopRecording?.isVisible = viewModel?.isRecording ?: true

        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.start_recording -> {
                startRecording()
                return true
            }
            R.id.stop_recording -> {
                stopRecording()
                return true
            }
            R.id.settings -> {
                startActivity(SettingsActivity.newIntent(activity))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startRecording() {
        viewModel?.isRecording = true
        activity.invalidateOptionsMenu()
        logcatService?.startLogcat(logcatEventListener)
    }

    private fun stopRecording() {
        viewModel?.isRecording = false
        activity.invalidateOptionsMenu()
        logcatService?.stopLogcat()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        logcatService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        logcatService = (service as LogcatService.LocalBinder).getLogcatService()
    }

    companion object {
        val TAG = LogcatLiveFragment::class.qualifiedName
    }
}