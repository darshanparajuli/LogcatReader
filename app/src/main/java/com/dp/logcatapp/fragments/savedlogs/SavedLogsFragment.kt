package com.dp.logcatapp.fragments.savedlogs

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.BaseActivityWithToolbar
import com.dp.logcatapp.activities.CabToolbarCallback
import com.dp.logcatapp.activities.SavedLogsActivity
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.inflateLayout
import kotlinx.android.synthetic.main.app_bar.*
import java.io.File

class SavedLogsFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener,
        Toolbar.OnMenuItemClickListener, CabToolbarCallback {
    companion object {
        val TAG = SavedLogsFragment::class.qualifiedName
    }

    private lateinit var viewModel: SavedLogsViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var recyclerViewAdapter: MyRecyclerViewAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)
                .get(SavedLogsViewModel::class.java)

        recyclerViewAdapter = MyRecyclerViewAdapter(this, this,
                viewModel.selectedItems)
        viewModel.fileNames.observe(this, Observer {
            if (it != null) {
                if (it.fileNames.isNotEmpty()) {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerViewAdapter.setItems(it.fileNames)

                    if (it.totalSize.isEmpty()) {
                        (activity as BaseActivityWithToolbar).toolbar.subtitle =
                                "${it.fileNames.size}"
                    } else {
                        (activity as BaseActivityWithToolbar).toolbar.subtitle =
                                "${it.fileNames.size} (${it.totalSize})"
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
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflateLayout(R.layout.fragment_saved_logs)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyView = view.findViewById(R.id.textViewEmpty)

        recyclerView = view.findViewById(R.id.recyclerView)
        linearLayoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = null
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = recyclerViewAdapter
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
                        val fileName = recyclerViewAdapter.getItem(pos)
                        val folder = File(context!!.filesDir, LogcatLiveFragment.LOGCAT_DIR)
                        val file = File(folder, fileName)
                        val intent = Intent(context, SavedLogsViewerActivity::class.java)
                        intent.setDataAndType(Uri.fromFile(file), "text/plain")
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

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                true
            }
            R.id.action_delete -> {
                val folder = File(context!!.filesDir, LogcatLiveFragment.LOGCAT_DIR)
                val deleted = viewModel.selectedItems
                        .map {
                            File(folder, recyclerViewAdapter.data[it])
                        }
                        .filter { it.delete() }
                        .map { it.name }
                        .toList()

                val updatedList = recyclerViewAdapter.data.filter { it !in deleted }.toList()

                viewModel.selectedItems.clear()
                viewModel.fileNames.update(updatedList)

                (activity as SavedLogsActivity).closeCabToolbar()
                true
            }
            else -> false
        }
    }

    override fun onCabToolbarOpen(toolbar: Toolbar) {
    }

    override fun onCabToolbarInvalidate(toolbar: Toolbar) {
        val share = toolbar.menu.findItem(R.id.action_share)
        share.isVisible = viewModel.selectedItems.size <= 1
    }

    override fun onCabToolbarClose(toolbar: Toolbar) {
        viewModel.selectedItems.clear()
        recyclerViewAdapter.notifyDataSetChanged()
    }

    private class MyRecyclerViewAdapter(
            val onClickListener: View.OnClickListener,
            val onLongClickListener: View.OnLongClickListener,
            val selectedItems: Set<Int>
    ) :
            RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {

        val data = mutableListOf<String>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.fragment_saved_logs_list_item, parent, false)
            view.setOnClickListener(onClickListener)
            view.setOnLongClickListener(onLongClickListener)
            return MyViewHolder(view)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.textView.text = data[position]
            holder.itemView.isSelected = selectedItems.contains(position)
        }

        fun getItem(index: Int) = data[index]

        fun setItems(items: List<String>) {
            val previousSize = data.size
            data.clear()
            notifyItemRangeRemoved(0, previousSize)
            data += items
            notifyItemRangeInserted(0, items.size)
        }

        class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.fileName)
        }
    }
}