package com.dp.logcatapp.fragments.savedlogs

import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.FileProvider
import android.support.v4.provider.DocumentFile
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.BaseActivityWithToolbar
import com.dp.logcatapp.activities.CabToolbarCallback
import com.dp.logcatapp.activities.SavedLogsActivity
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.*
import java.io.*
import java.lang.ref.WeakReference

class SavedLogsFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener,
        Toolbar.OnMenuItemClickListener, CabToolbarCallback {
    companion object {
        val TAG = SavedLogsFragment::class.qualifiedName
        private const val SAVE_REQ = 12
    }

    private lateinit var viewModel: SavedLogsViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var recyclerViewAdapter: MyRecyclerViewAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)
                .get(SavedLogsViewModel::class.java)

        recyclerViewAdapter = MyRecyclerViewAdapter(activity!!, this,
                this, viewModel.selectedItems)
        viewModel.fileNames.observe(this, Observer {
            progressBar.visibility = View.GONE
            if (it != null) {
                if (it.logFiles.isNotEmpty()) {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerViewAdapter.setItems(it.logFiles)

                    if (it.totalSize.isEmpty()) {
                        (activity as BaseActivityWithToolbar).toolbar.subtitle =
                                "${it.logFiles.size}"
                    } else {
                        val totalLogCountFmt = if (it.totalLogCount == 1L) {
                            resources.getString(R.string.log_count_fmt)
                        } else {
                            resources.getString(R.string.log_count_fmt_plural)
                        }

                        val totalLogCountStr = totalLogCountFmt.format(it.totalLogCount)
                        (activity as BaseActivityWithToolbar).toolbar.subtitle =
                                "${it.logFiles.size} ($totalLogCountStr, ${it.totalSize})"
                    }

                    if (viewModel.selectedItems.isNotEmpty()) {
                        (activity as SavedLogsActivity).openCabToolbar(this,
                                this)
                        (activity as SavedLogsActivity).setCabToolbarTitle(viewModel
                                .selectedItems.size.toString())
                    }
                } else {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    recyclerViewAdapter.setItems(emptyList())
                    (activity as BaseActivityWithToolbar).toolbar.subtitle = null
                }
            } else {
                emptyView.visibility = View.VISIBLE
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflateLayout(R.layout.fragment_saved_logs)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyView = view.findViewById(R.id.textViewEmpty)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView = view.findViewById(R.id.recyclerView)
        linearLayoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = null
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = recyclerViewAdapter

        fragmentManager?.findFragmentByTag(RenameDialogFragment.TAG)
                ?.setTargetFragment(this, 0)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.list_item_root -> {
                val pos = linearLayoutManager.getPosition(v)
                if (pos != RecyclerView.NO_POSITION) {
                    if ((activity as SavedLogsActivity).isCabToolbarActive()) {
                        onSelect(pos)
                        if (viewModel.selectedItems.isEmpty()) {
                            (activity as SavedLogsActivity).closeCabToolbar()
                        }
                    } else {
                        val fileInfo = recyclerViewAdapter.getItem(pos)
                        val intent = Intent(context, SavedLogsViewerActivity::class.java)
                        intent.setDataAndType(fileInfo.uri, "text/plain")
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun onSelect(pos: Int) {
        if (pos in viewModel.selectedItems) {
            viewModel.selectedItems -= pos
        } else {
            viewModel.selectedItems += pos
        }
        (activity as SavedLogsActivity).setCabToolbarTitle(viewModel.selectedItems.size.toString())
        (activity as SavedLogsActivity).invalidateCabToolbarMenu()
        recyclerViewAdapter.notifyItemChanged(pos)

    }

    override fun onLongClick(v: View): Boolean {
        when (v.id) {
            R.id.list_item_root -> {
                if ((activity as SavedLogsActivity).isCabToolbarActive()) {
                    return false
                }

                val pos = linearLayoutManager.getPosition(v)
                if (pos != RecyclerView.NO_POSITION) {
                    viewModel.selectedItems.clear()
                    onSelect(pos)
                    return (activity as SavedLogsActivity).openCabToolbar(this,
                            this)
                }
            }
        }
        return false
    }

    private fun selectAll() {
        viewModel.selectedItems.clear()
        for (i in 0 until recyclerViewAdapter.itemCount) {
            onSelect(i)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_rename -> {
                val fileInfo = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])
                val folder = File(context!!.filesDir, LogcatLiveFragment.LOGCAT_DIR)
                val file = File(folder, fileInfo.name)
                val frag = RenameDialogFragment.newInstance(file.absolutePath)
                frag.setTargetFragment(this, 0)
                frag.show(fragmentManager, RenameDialogFragment.TAG)
                true
            }
            R.id.action_save -> {
                if (Build.VERSION.SDK_INT >= 19) {
                    saveToDeviceKitkat()
                } else {
                    saveToDeviceFallback()
                }
                true
            }
            R.id.action_share -> {
                val fileInfo = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])
                val folder = File(context!!.filesDir, LogcatLiveFragment.LOGCAT_DIR)
                val file = File(folder, fileInfo.name)

                try {
                    val intent = Intent(Intent.ACTION_SEND)
                    val uri = FileProvider.getUriForFile(context!!,
                            "${context!!.packageName}.${BuildConfig.FILE_PROVIDER}", file)
                    intent.setDataAndType(uri, "text/*")
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                    startActivity(Intent.createChooser(intent, getString(R.string.share)))
                } catch (e: Exception) {
                    context?.showToast("Unable to share")
                }
                true
            }
            R.id.action_select_all -> {
                selectAll()
                true
            }
            R.id.action_delete -> {
                deleteSelectedLogFiles()
                true
            }
            else -> false
        }
    }

    private fun getCustomLocation(): String =
            context!!.getDefaultSharedPreferences().getString(PreferenceKeys.Logcat.KEY_SAVE_LOCATION, "")!!

    private fun deleteSelectedLogFiles() {
        val deleted = mutableListOf<String>()

        val deleteHelper = { folder: File ->
            viewModel.selectedItems
                    .map {
                        File(folder, recyclerViewAdapter.data[it].name)
                    }
                    .filter { it.delete() }
                    .map { it.name }
                    .forEach { deleted += it }
        }

        // delete internal
        val folder = File(context!!.filesDir, LogcatLiveFragment.LOGCAT_DIR)
        deleteHelper(folder)

        val customLocation = getCustomLocation()
        if (customLocation.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= 21) {
                val customFolder = DocumentFile.fromTreeUri(context!!, Uri.parse(customLocation))
                viewModel.selectedItems
                        .mapNotNull {
                            customFolder?.findFile(recyclerViewAdapter.data[it].name)
                        }
                        .forEach {
                            val name = it.name
                            if (it.delete()) {
                                if (name != null) {
                                    deleted += name
                                }
                            }
                        }
            } else {
                val customFolder = File(customLocation)
                viewModel.selectedItems
                        .map {
                            File(customFolder, recyclerViewAdapter.data[it].name)
                        }
                        .filter { it.delete() }
                        .map { it.name }
                        .forEach { deleted += it }
            }
        }

        val updatedList = recyclerViewAdapter.data
                .filter { it.name !in deleted }.toList()

        viewModel.selectedItems.clear()
        viewModel.fileNames.update(updatedList)

        (activity as SavedLogsActivity).closeCabToolbar()
    }

    private fun saveToDeviceFallback() {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            activity!!.showToast(getString(R.string.external_storage_not_mounted_error))
            return
        }

        val fileInfo = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])
        val fileName = fileInfo.name
        val srcFolder = File(context!!.filesDir, LogcatLiveFragment.LOGCAT_DIR)
        val src = File(srcFolder, fileName)

        val documentsFolder = Environment.getExternalStoragePublicDirectory("Documents")
        val destFolder = File(documentsFolder, "LogcatReader")
        if (!destFolder.exists()) {
            if (!destFolder.mkdirs()) {
                activity!!.showToast(getString(R.string.error_saving))
                return
            }
        }

        val dest = File(destFolder, fileName)

        try {
            SaveFileTask(this, FileInputStream(src), FileOutputStream(dest)).execute()
        } catch (e: IOException) {
            activity!!.showToast(getString(R.string.error_saving))
        }
    }

    @TargetApi(19)
    private fun saveToDeviceKitkat() {
        try {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "text/plain"
            startActivityForResult(intent, SAVE_REQ)
        } catch (e: Exception) {
            context?.showToast(getString(R.string.error))
        }
    }

    private fun onSaveCallback(uri: Uri) {
        val fileInfo = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])
        val folder = File(context!!.filesDir, LogcatLiveFragment.LOGCAT_DIR)
        val file = File(folder, fileInfo.name)

        try {
            val src = FileInputStream(file)
            val dest = context!!.contentResolver.openOutputStream(uri)
            SaveFileTask(this, src, dest).execute()
        } catch (e: IOException) {
            activity!!.showToast(getString(R.string.error_saving))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SAVE_REQ -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        onSaveCallback(data.data)
                    }
                }
            }
        }
    }

    override fun onCabToolbarOpen(toolbar: Toolbar) {
    }

    override fun onCabToolbarInvalidate(toolbar: Toolbar) {
        val share = toolbar.menu.findItem(R.id.action_share)
        val save = toolbar.menu.findItem(R.id.action_save)
        val rename = toolbar.menu.findItem(R.id.action_rename)

        val visible = viewModel.selectedItems.size <= 1
        share.isVisible = visible
        save.isVisible = visible
        rename.isVisible = visible
    }

    override fun onCabToolbarClose(toolbar: Toolbar) {
        viewModel.selectedItems.clear()
        recyclerViewAdapter.notifyDataSetChanged()
    }

    private class MyRecyclerViewAdapter(
            context: Context,
            val onClickListener: View.OnClickListener,
            val onLongClickListener: View.OnLongClickListener,
            val selectedItems: Set<Int>
    ) : RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {

        val data = mutableListOf<LogFileInfo>()
        val logFormat: String = context.resources.getString(R.string.log_count_fmt)
        val logsFormat: String = context.resources.getString(R.string.log_count_fmt_plural)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.fragment_saved_logs_list_item, parent, false)
            view.setOnClickListener(onClickListener)
            view.setOnLongClickListener(onLongClickListener)
            return MyViewHolder(view)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val fileInfo = data[position]
            holder.fileName.text = fileInfo.name
            holder.fileSize.text = fileInfo.sizeStr

            // no need to check for 0 as the app does not allow for saving empty logs
            if (fileInfo.count == 1L) {
                holder.logCount.text = logFormat.format(fileInfo.count)
            } else {
                holder.logCount.text = logsFormat.format(fileInfo.count)
            }

            holder.itemView.isSelected = selectedItems.contains(position)
        }

        fun getItem(index: Int) = data[index]

        fun setItems(items: List<LogFileInfo>) {
            val previousSize = data.size
            data.clear()
            notifyItemRangeRemoved(0, previousSize)
            data += items
            notifyItemRangeInserted(0, items.size)
        }

        class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fileName: TextView = itemView.findViewById(R.id.fileName)
            val fileSize: TextView = itemView.findViewById(R.id.fileSize)
            val logCount: TextView = itemView.findViewById(R.id.logCount)
        }
    }

    class SaveFileTask(frag: SavedLogsFragment,
                       private val src: InputStream,
                       private val dest: OutputStream) :
            AsyncTask<Void, Void, Boolean>() {

        private val ref = WeakReference(frag)

        override fun doInBackground(vararg params: Void?): Boolean {
            return try {
                src.copyTo(dest)
                true
            } catch (e: IOException) {
                false
            } finally {
                src.closeQuietly()
                dest.closeQuietly()
            }
        }

        override fun onPostExecute(result: Boolean) {
            val frag = ref.get() ?: return
            if (frag.activity == null) {
                return
            }

            if (result) {
                frag.activity!!.showToast(frag.getString(R.string.saved))
            } else {
                frag.activity!!.showToast(frag.getString(R.string.error_saving))
            }
        }
    }

    class RenameDialogFragment : BaseDialogFragment() {

        companion object {
            val TAG = RenameDialogFragment::class.qualifiedName

            private val KEY_PATH = TAG + "_key_path"

            fun newInstance(path: String): RenameDialogFragment {
                val bundle = Bundle()
                bundle.putString(KEY_PATH, path)
                val frag = RenameDialogFragment()
                frag.arguments = bundle
                return frag
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = inflateLayout(R.layout.rename_dialog)
            val editText = view.findViewById<EditText>(R.id.editText)

            val path = arguments!!.getString(KEY_PATH)
            val file = File(path)

            editText.setText(file.name)
            editText.selectAll()

            val dialog = AlertDialog.Builder(activity!!)
                    .setTitle(R.string.rename)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val newName = editText.text.toString()
                        if (newName.isNotEmpty()) {
                            if (file.renameTo(File(file.parent, newName))) {
                                (targetFragment as SavedLogsFragment).viewModel.fileNames.load()
                            } else {
                                activity!!.showToast(getString(R.string.error))
                            }
                            (activity as SavedLogsActivity).closeCabToolbar()
                        }
                        dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        dismiss()
                    }
                    .create()

            dialog.setOnShowListener {
                editText.requestFocus()
                val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as
                        InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }

            return dialog
        }
    }
}