package com.dp.logcatapp.fragments.savedlogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat.getColor
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.BaseActivityWithToolbar
import com.dp.logcatapp.activities.CabToolbarCallback
import com.dp.logcatapp.activities.SavedLogsActivity
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.db.MyDB
import com.dp.logcatapp.db.SavedLogInfo
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.ShareUtils
import com.dp.logcatapp.util.closeQuietly
import com.dp.logcatapp.util.getAndroidViewModel
import com.dp.logcatapp.util.getParcelableSafe
import com.dp.logcatapp.util.inflateLayout
import com.dp.logcatapp.util.showToast
import com.dp.logcatapp.views.IndeterminateProgressSnackBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

class SavedLogsFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener,
  Toolbar.OnMenuItemClickListener, CabToolbarCallback, FragmentResultListener {
  companion object {
    val TAG = SavedLogsFragment::class.qualifiedName
  }

  private lateinit var viewModel: SavedLogsViewModel
  private lateinit var recyclerView: RecyclerView
  private lateinit var emptyView: View
  private lateinit var recyclerViewAdapter: MyRecyclerViewAdapter
  private lateinit var linearLayoutManager: LinearLayoutManager
  private lateinit var progressBar: ProgressBar
  private lateinit var snackBarProgress: IndeterminateProgressSnackBar
  private lateinit var createDocumentLauncher: ActivityResultLauncher<String>

  private var exportFormat: ExportFormat? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getAndroidViewModel()

    createDocumentLauncher = registerForActivityResult(
      CreateDocument(mimeType = "text/plain"),
      object : ActivityResultCallback<Uri?> {
        override fun onActivityResult(result: Uri?) {
          if (result != null) {
            onSaveCallback(result)
          }
        }
      }
    )

    recyclerViewAdapter = MyRecyclerViewAdapter(
      requireActivity(),
      this, this, viewModel.selectedItems
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? =
    inflateLayout(R.layout.fragment_saved_logs)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    emptyView = view.findViewById(R.id.textViewEmpty)
    progressBar = view.findViewById(R.id.progressBar)

    recyclerView = view.findViewById(R.id.recyclerView)
    linearLayoutManager = LinearLayoutManager(context)
    recyclerView.itemAnimator = null
    recyclerView.layoutManager = linearLayoutManager
    recyclerView.adapter = recyclerViewAdapter
    snackBarProgress = IndeterminateProgressSnackBar(view, getString(R.string.saving))

    parentFragmentManager.setFragmentResultListener(RenameDialogFragment.REQ_RENAME, this, this)

    viewModel.getFileNames().observe(viewLifecycleOwner, Observer {
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
            (activity as SavedLogsActivity).openCabToolbar(
              this,
              this
            )
            (activity as SavedLogsActivity).setCabToolbarTitle(
              viewModel
                .selectedItems.size.toString()
            )
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

  override fun onFragmentResult(requestKey: String, result: Bundle) {
    when (requestKey) {
      RenameDialogFragment.REQ_RENAME -> {
        val newName = requireNotNull(result.getString(RenameDialogFragment.RESULT_FILE_NAME))
        val uri = requireNotNull(result.getString(RenameDialogFragment.RESULT_FILE_URI)).toUri()
        onRename(newName, uri)
      }
      ChooseExportFormatTypeDialogFragment.REQ_CHOOSE_EXPORT_FORMAT -> {
        val exportFormat = requireNotNull(
          result.getParcelableSafe<ExportFormat>(
            ChooseExportFormatTypeDialogFragment.RESULT_EXPORT_FORMAT
          )
        )
        handleExportAction(exportFormat)
      }
    }
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
            intent.setDataAndType(Uri.parse(fileInfo.info.path), "text/plain")
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
          return (activity as SavedLogsActivity).openCabToolbar(
            this,
            this
          )
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
        val frag = RenameDialogFragment.newInstance(fileInfo.info.fileName, fileInfo.info.path)
        frag.show(parentFragmentManager, RenameDialogFragment.TAG)
        true
      }
      R.id.action_export -> {
        val dialog = ChooseExportFormatTypeDialogFragment()
        dialog.show(parentFragmentManager, ChooseExportFormatTypeDialogFragment.TAG)
        true
      }
      R.id.action_share -> {
        val fileInfo = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])
        ShareUtils.shareSavedLogs(
          requireContext(), Uri.parse(fileInfo.info.path),
          fileInfo.info.isCustom
        )
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

  private fun handleExportAction(exportFormat: ExportFormat) {
    this.exportFormat = exportFormat
    saveToDevice()
  }

  private fun deleteSelectedLogFiles() {
    val deleteList = viewModel.selectedItems
      .map { recyclerViewAdapter.data[it] }
      .toList()

    val db = MyDB.getInstance(requireContext())
    scope.launch {
      withContext(IO) {
        val deleted = deleteList
          .filter {
            with(Uri.parse(it.info.path)) {
              if (it.info.isCustom) {
                val file = DocumentFile.fromSingleUri(requireContext(), this)
                file != null && file.delete()
              } else {
                this.toFile().delete()
              }
            }
          }
          .map { it.info.fileName }
          .toList()

        val deletedSavedLogInfoList = deleteList
          .map { it.info }
          .filter { it.fileName in deleted }
          .toTypedArray()
        db.savedLogsDao().delete(*deletedSavedLogInfoList)
      }

      viewModel.selectedItems.clear()
      viewModel.load()
    }

    (activity as SavedLogsActivity).closeCabToolbar()
  }

  private fun saveToDevice() {
    try {
      val fileInfo = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])
      createDocumentLauncher.launch(fileInfo.info.fileName)
    } catch (_: Exception) {
      context?.showToast(getString(R.string.error))
    }
  }

  private fun onSaveCallback(uri: Uri) {
    val fileInfo = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])
    val folder = File(requireContext().filesDir, LogcatLiveFragment.LOGCAT_DIR)
    val file = File(folder, fileInfo.info.fileName)

    try {
      val src = FileInputStream(file)
      val dest = requireContext().contentResolver.openOutputStream(uri)
      runSaveFileTask(src, dest!!)
    } catch (_: IOException) {
      requireActivity().showToast(getString(R.string.error_saving))
    }
  }

  override fun onCabToolbarOpen(toolbar: Toolbar) {
  }

  override fun onCabToolbarInvalidate(toolbar: Toolbar) {
    val share = toolbar.menu.findItem(R.id.action_share)
    val save = toolbar.menu.findItem(R.id.action_export)
    val rename = toolbar.menu.findItem(R.id.action_rename)

    val visible = viewModel.selectedItems.size == 1
    share.isVisible = visible
    save.isVisible = visible

    if (visible) {
      val info = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])
      rename.isVisible = !info.info.isCustom
    } else {
      rename.isVisible = false
    }
  }

  override fun onCabToolbarClose(toolbar: Toolbar) {
    viewModel.selectedItems.clear()
    recyclerViewAdapter.notifyItemRangeChanged(0, recyclerViewAdapter.data.size)
  }

  private fun onRename(
    newName: String,
    newPath: Uri
  ) {
    val fileInfo = recyclerViewAdapter.getItem(viewModel.selectedItems.toIntArray()[0])

    val db = MyDB.getInstance(requireContext())
    scope.launch {
      withContext(IO) {
        db.savedLogsDao().delete(fileInfo.info)
        db.savedLogsDao().insert(SavedLogInfo(newName, newPath.toString(), fileInfo.info.isCustom))
      }

      viewModel.load()
    }

    (activity as SavedLogsActivity).closeCabToolbar()
  }

  class ChooseExportFormatTypeDialogFragment : BaseDialogFragment() {
    private var exportFormat = ExportFormat.DEFAULT

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
      return AlertDialog.Builder(requireActivity())
        .setTitle(getString(R.string.select_export_format))
        .setSingleChoiceItems(R.array.export_format, 0) { _, which ->
          exportFormat = ExportFormat.entries[which]
        }
        .setPositiveButton(R.string.export) { _, _ ->
          setFragmentResult(
            requestKey = REQ_CHOOSE_EXPORT_FORMAT,
            result = Bundle().apply { putParcelable(RESULT_EXPORT_FORMAT, exportFormat) },
          )
        }
        .create()
    }

    companion object {
      val TAG = ChooseExportFormatTypeDialogFragment::class.qualifiedName
      const val REQ_CHOOSE_EXPORT_FORMAT = "req-choose-export-format"
      const val RESULT_EXPORT_FORMAT = "result-export-format"
    }
  }

  @Parcelize
  private enum class ExportFormat : Parcelable {
    DEFAULT,
    SINGLE
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

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ): MyViewHolder {
      val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.fragment_saved_logs_list_item, parent, false)
      view.setOnClickListener(onClickListener)
      view.setOnLongClickListener(onLongClickListener)
      return MyViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(
      holder: MyViewHolder,
      position: Int
    ) {
      val fileInfo = data[position]
      holder.fileName.text = fileInfo.info.fileName
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

    fun setItems(data: List<LogFileInfo>) {
      val callback = DataDiffCallback(this.data, data)
      val result = DiffUtil.calculateDiff(callback)

      this.data.clear()
      this.data += data
      result.dispatchUpdatesTo(this)
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val fileName: TextView = itemView.findViewById(R.id.fileName)
      val fileSize: TextView = itemView.findViewById(R.id.fileSize)
      val logCount: TextView = itemView.findViewById(R.id.logCount)
    }

    private class DataDiffCallback(
      private val old: List<LogFileInfo>,
      private val new: List<LogFileInfo>
    ) : DiffUtil.Callback() {

      override fun areItemsTheSame(
        p0: Int,
        p1: Int
      ): Boolean {
        return old[p0].info.path == new[p1].info.path
      }

      override fun getOldListSize(): Int {
        return old.size
      }

      override fun getNewListSize(): Int {
        return new.size
      }

      override fun areContentsTheSame(
        p0: Int,
        p1: Int
      ): Boolean {
        return old[p0] == new[p1]
      }
    }
  }

  private fun runSaveFileTask(
    src: InputStream,
    dest: OutputStream
  ) {
    scope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        snackBarProgress.show()
        val result = withContext(IO) {
          try {
            when (this@SavedLogsFragment.exportFormat ?: return@withContext false) {
              ExportFormat.DEFAULT -> {
                src.copyTo(dest)
              }
              ExportFormat.SINGLE -> {
                val bufferedWriter = BufferedWriter(OutputStreamWriter(dest))
                LogcatStreamReader(src).use {
                  for (log in it) {
                    val metadata = log.metadataToString()
                    val msgTokens = log.msg.split("\n")
                    for (msg in msgTokens) {
                      bufferedWriter.write(metadata)
                      bufferedWriter.write(" ")
                      bufferedWriter.write(msg)
                      bufferedWriter.newLine()
                    }
                  }
                }
              }
            }
            true
          } catch (_: IOException) {
            false
          } finally {
            src.closeQuietly()
            dest.closeQuietly()
          }
        }

        snackBarProgress.dismiss()
        view?.let {
          if (result) {
            Snackbar.make(it, R.string.saved, LENGTH_SHORT)
              .show()
            (activity as? SavedLogsActivity)?.closeCabToolbar()
          } else {
            Snackbar.make(it, R.string.error_saving, LENGTH_SHORT)
              .setBackgroundTint(getColor(requireContext(), R.color.color_primary_error))
              .show()
          }
        }
      }
    }
  }

  class RenameDialogFragment : BaseDialogFragment() {

    companion object {
      val TAG = RenameDialogFragment::class.qualifiedName

      private val KEY_FILENAME = TAG + "_key_filename"
      private val KEY_PATH = TAG + "_key_path"
      const val REQ_RENAME = "req-rename"
      const val RESULT_FILE_NAME = "file-name"
      const val RESULT_FILE_URI = "file-uri"

      fun newInstance(
        fileName: String,
        path: String
      ): RenameDialogFragment {
        val bundle = Bundle()
        bundle.putString(KEY_FILENAME, fileName)
        bundle.putString(KEY_PATH, path)
        val frag = RenameDialogFragment()
        frag.arguments = bundle
        return frag
      }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
      val view = inflateLayout(R.layout.rename_dialog)
      val editText = view.findViewById<EditText>(R.id.editText)
      editText.setText(requireArguments().getString(KEY_FILENAME))
      editText.selectAll()

      val dialog = AlertDialog.Builder(requireActivity())
        .setTitle(R.string.rename)
        .setView(view)
        .setPositiveButton(android.R.string.ok) { _, _ ->
          val newName = editText.text.toString()
          if (newName.isNotEmpty()) {
            val file = requireArguments().getString(KEY_PATH)!!.toUri().toFile()
            val newFile = File(file.parent, newName)
            if (file.renameTo(newFile)) {
              setFragmentResult(REQ_RENAME, Bundle().apply {
                putString(RESULT_FILE_NAME, newName)
                putString(RESULT_FILE_URI, newFile.toUri().toString())
              })
            } else {
              requireActivity().showToast(getString(R.string.error))
            }
          }
          dismiss()
        }
        .setNegativeButton(android.R.string.cancel) { _, _ ->
          dismiss()
        }
        .create()

      dialog.setOnShowListener {
        editText.requestFocus()
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as
          InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
      }

      return dialog
    }
  }
}