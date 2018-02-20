package com.dp.logcatapp.fragments.savedlogs

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.inflateLayout
import java.io.File

class SavedLogsFragment : BaseFragment(), View.OnClickListener {
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
        recyclerViewAdapter = MyRecyclerViewAdapter(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this)
                .get(SavedLogsViewModel::class.java)
        viewModel.fileNames.observe(this, Observer { fileNames ->
            if (fileNames != null) {
                if (fileNames.isNotEmpty()) {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerViewAdapter.setItems(fileNames)
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
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.list_item_root -> {
                val pos = linearLayoutManager.getPosition(v)
                if (pos != RecyclerView.NO_POSITION) {
                    val fileName = recyclerViewAdapter.getItem(pos)

                    val path = File(LogcatLiveFragment.LOGCAT_DIR, fileName)
                    val uri = FileProvider.getUriForFile(context!!,
                            context!!.applicationContext.packageName + ".provider", path)

                    val intent = Intent(context, SavedLogsViewerActivity::class.java)
                    intent.setDataAndType(uri, "text/plain")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    private class MyRecyclerViewAdapter(val onClickListener: View.OnClickListener) :
            RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {

        val data = mutableListOf<String>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.fragment_saved_logs_list_item, parent, false)
            view.setOnClickListener(onClickListener)
            return MyViewHolder(view)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.textView.text = data[position]
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