package com.dp.logcatapp.fragments.logcat_live

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.Toast
import com.dp.logcat.Log
import com.dp.logcat.LogcatEventListener
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.SettingsActivity
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.services.logcat.LogcatService
import com.dp.logcatapp.util.ServiceBinder

class LogcatLiveFragment : BaseFragment(), ServiceConnection {
    private var isRecording: Boolean = false
    private var serviceBinder: ServiceBinder? = null
    private var logcatService: LogcatService? = null
    private val logcatEventListener = object : LogcatEventListener {
        override fun onStartEvent() {
            Toast.makeText(activity, "Logcat started", Toast.LENGTH_SHORT).show()
        }

        override fun onLogEvent(log: Log) {
        }

        override fun onFailEvent() {
            Toast.makeText(activity, "Logcat failed to start", Toast.LENGTH_SHORT).show()
        }

        override fun onStopEvent() {
            Toast.makeText(activity, "Logcat stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        serviceBinder = ServiceBinder(LogcatService::class.java, this)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = LayoutInflater.from(activity)
                .inflate(R.layout.fragment_logcat_live, null, false)

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

        startRecording?.isVisible = !isRecording
        stopRecording?.isVisible = isRecording

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
        isRecording = true
        activity.invalidateOptionsMenu()
        logcatService?.startLogcat(logcatEventListener)
    }

    private fun stopRecording() {
        isRecording = false
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