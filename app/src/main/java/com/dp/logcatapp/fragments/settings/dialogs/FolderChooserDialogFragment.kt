package com.dp.logcatapp.fragments.settings.dialogs

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.settings.SettingsFragment
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.getAndroidViewModel
import com.dp.logcatapp.util.getAttributeDrawable
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.inflateLayout
import java.io.File

class FolderChooserDialogFragment : BaseDialogFragment(), View.OnClickListener {

  companion object {
    val TAG = FolderChooserDialogFragment::class.qualifiedName
  }

  private lateinit var recyclerViewAdapter: MyRecyclerViewAdapter
  private lateinit var layoutManager: LinearLayoutManager
  private lateinit var viewModel: MyViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    recyclerViewAdapter = MyRecyclerViewAdapter(requireContext(), this)

    viewModel = getAndroidViewModel()
    viewModel.files.observe(this, Observer<List<FileHolder>> {
      if (it != null) {
        recyclerViewAdapter.setData(it)
      }
    })
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val rootView = inflateLayout(R.layout.folder_chooser)
    val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerView)

    val activity = requireActivity()
    layoutManager = LinearLayoutManager(activity)

    recyclerView.layoutManager = layoutManager
    recyclerView.adapter = recyclerViewAdapter

    return AlertDialog.Builder(activity)
      .setTitle("Select a folder")
      .setView(rootView)
      .setPositiveButton(getString(R.string.select)) { _, _ ->
        val folder = if (recyclerViewAdapter.itemCount > 0) {
          val fileHolder = recyclerViewAdapter[0]
          if (fileHolder.isParent) {
            fileHolder.file
          } else {
            null
          }
        } else {
          null
        }

        (targetFragment as SettingsFragment).setupCustomSaveLocationPreLollipop(folder)
      }
      .setNegativeButton(android.R.string.cancel) { _, _ ->
        dismiss()
      }
      .create()
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.folderChooserListItem -> {
        val pos = layoutManager.getPosition(v)
        if (pos != RecyclerView.NO_POSITION) {
          val fileHolder = recyclerViewAdapter[pos]
          if (fileHolder.isParent) {
            fileHolder.file.parentFile?.let {
              viewModel.update(it)
            }
          } else {
            if (fileHolder.file.isDirectory) {
              viewModel.update(fileHolder.file)
            }
          }
        }
      }
    }
  }
}

internal class MyViewModel(application: Application) : AndroidViewModel(application) {
  val files = MutableLiveData<List<FileHolder>>()

  init {
    val path = application.getDefaultSharedPreferences().getString(
      PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
      ""
    )!!

    val file = if (path.isEmpty()) {
      @Suppress("DEPRECATION")
      Environment.getExternalStorageDirectory()
    } else {
      File(path)
    }

    update(file)
  }

  fun update(file: File) {
    val files = mutableListOf<FileHolder>()

    if (file.parentFile != null) {
      files.add(FileHolder(file, isParent = true))
    }

    file.listFiles()
      ?.map { FileHolder(it) }
      ?.sortedBy { it.file.name }
      ?.forEach { files.add(it) }

    this.files.value = files
  }
}

internal data class FileHolder(
  val file: File,
  val isParent: Boolean = false
)

private class MyRecyclerViewAdapter(
  context: Context,
  private val onClickListener: View.OnClickListener
) :
  RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {

  private var data = listOf<FileHolder>()

  private val drawableFolder = context.getAttributeDrawable(R.attr.ic_folder)!!
  private val drawableFile = context.getAttributeDrawable(R.attr.ic_insert_drive_file)!!

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): MyViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(
      R.layout.folder_chooser_list_item,
      parent, false
    )
    view.setOnClickListener(onClickListener)
    return MyViewHolder(view)
  }

  override fun getItemCount() = data.size

  fun setData(items: List<FileHolder>) {
    data = items
    notifyDataSetChanged()
  }

  operator fun get(index: Int) = data[index]

  override fun onBindViewHolder(
    holder: MyViewHolder,
    position: Int
  ) {
    val fileHolder = data[position]
    if (fileHolder.isParent) {
      holder.fileName.text = ".."
    } else {
      holder.fileName.text = fileHolder.file.name
    }
    if (fileHolder.file.isDirectory) {
      holder.fileIcon.setImageDrawable(drawableFolder)
    } else {
      holder.fileIcon.setImageDrawable(drawableFile)
    }
  }

  class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val fileIcon: ImageView = itemView.findViewById(R.id.fileIcon)
    val fileName: TextView = itemView.findViewById(R.id.fileName)
  }
}