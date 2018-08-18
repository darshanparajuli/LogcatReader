package com.dp.logcatapp.fragments.filters

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.MyDB
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.filters.dialogs.FilterDialogFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveViewModel
import com.dp.logcatapp.util.inflateLayout
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class FiltersFragment : BaseFragment() {

    companion object {
        val TAG = FiltersFragment::class.qualifiedName
        private val KEY_EXCLUSIONS = TAG + "_key_exclusions"

        fun newInstance(exclusions: Boolean): FiltersFragment {
            val frag = FiltersFragment()
            val bundle = Bundle()
            bundle.putBoolean(KEY_EXCLUSIONS, exclusions)
            frag.arguments = bundle
            return frag
        }
    }

    private lateinit var viewModel: LogcatLiveViewModel
    private lateinit var recyclerViewAdapter: MyRecyclerViewAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var emptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(activity!!)
                .get(LogcatLiveViewModel::class.java)
        recyclerViewAdapter = MyRecyclerViewAdapter {
            onRemoveClicked(it)
        }

        val dao = MyDB.getInstance(activity!!).filterDao()
        val flowable = if (isExclusions()) {
            dao.getExclusions()
        } else {
            dao.getFilters()
        }

        flowable.observeOn(AndroidSchedulers.mainThread())
                .subscribe { list ->
                    val data = list.map {
                        val displayText: String
                        val type: String
                        when (it.type) {
                            FilterType.LOG_LEVELS -> {
                                type = getString(R.string.log_level)
                                displayText = it.content.split(",")
                                        .joinToString(", ") { s ->
                                            when (s) {
                                                LogPriority.ASSERT -> "Assert"
                                                LogPriority.ERROR -> "Error"
                                                LogPriority.DEBUG -> "Debug"
                                                LogPriority.FATAL -> "Fatal"
                                                LogPriority.INFO -> "Info"
                                                LogPriority.VERBOSE -> "Verbose"
                                                LogPriority.WARNING -> "warning"
                                                else -> ""
                                            }
                                        }
                            }
                            else -> {
                                displayText = it.content
                                when (it.type) {
                                    FilterType.KEYWORD -> type = getString(R.string.keyword)
                                    FilterType.TAG -> type = getString(R.string.tag)
                                    FilterType.PID -> type = getString(R.string.process_id)
                                    FilterType.TID -> type = getString(R.string.thread_id)
                                    else -> throw IllegalStateException("invalid type: ${it.type}")
                                }
                            }
                        }
                        FilterListItem(type, displayText, it)
                    }

                    if (data.isEmpty()) {
                        emptyMessage.visibility = View.VISIBLE
                    } else {
                        emptyMessage.visibility = View.GONE
                    }
                    recyclerViewAdapter.setData(data)
                }
    }

    fun isExclusions() = arguments?.getBoolean(KEY_EXCLUSIONS) ?: false

    private fun onRemoveClicked(v: View) {
        val pos = linearLayoutManager.getPosition(v)
        if (pos != RecyclerView.NO_POSITION) {
            val item = recyclerViewAdapter[pos]
            recyclerViewAdapter.remove(pos)
            Flowable.just(MyDB.getInstance(context!!))
                    .subscribeOn(Schedulers.io())
                    .subscribe {
                        it.filterDao().delete(item.info)
                    }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflateLayout(R.layout.filters_fragment)

        emptyMessage = rootView.findViewById(R.id.textViewEmpty)

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.addItemDecoration(DividerItemDecoration(activity!!,
                DividerItemDecoration.VERTICAL))
        linearLayoutManager = LinearLayoutManager(activity!!)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = recyclerViewAdapter

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.filters, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_action -> {
                val frag = FilterDialogFragment()
                frag.setTargetFragment(this, 0)
                frag.show(fragmentManager, FilterDialogFragment.TAG)
                true
            }
            R.id.clear_action -> {
                Flowable.just(MyDB.getInstance(context!!))
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            it.filterDao().deleteAll(isExclusions())
                        }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun addFilter(keyword: String, tag: String, pid: String, tid: String, logLevels: Set<String>) {
        val list = mutableListOf<FilterInfo>()
        val exclude = isExclusions()

        if (keyword.isNotEmpty()) {
            list.add(FilterInfo(FilterType.KEYWORD, keyword, exclude))
        }

        if (tag.isNotEmpty()) {
            list.add(FilterInfo(FilterType.TAG, tag, exclude))
        }

        if (pid.isNotEmpty()) {
            list.add(FilterInfo(FilterType.PID, pid, exclude))
        }

        if (tid.isNotEmpty()) {
            list.add(FilterInfo(FilterType.TID, tid, exclude))
        }

        if (logLevels.isNotEmpty()) {
            list.add(FilterInfo(FilterType.LOG_LEVELS,
                    logLevels.sorted().joinToString(","), exclude))
        }

        if (list.isNotEmpty()) {
            Flowable.just(MyDB.getInstance(context!!))
                    .subscribeOn(Schedulers.io())
                    .subscribe {
                        it.filterDao().insert(*list.toTypedArray())
                    }
        }
    }
}

internal data class FilterListItem(val type: String,
                                   val content: String,
                                   val info: FilterInfo)

internal class MyRecyclerViewAdapter(private val onRemoveListener: (View) -> Unit) :
        RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {

    private val data = mutableListOf<FilterListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = parent.context.inflateLayout(R.layout.filters_fragment_list_item, parent)
        view.findViewById<ImageButton>(R.id.removeFilterIcon).setOnClickListener {
            onRemoveListener(it.parent as ViewGroup)
        }
        return MyViewHolder(view)
    }

    override fun getItemCount() = data.size

    operator fun get(index: Int) = data[index]

    fun remove(index: Int) {
        data.removeAt(index)
        notifyItemRemoved(index)
    }

    fun setData(data: List<FilterListItem>) {
        val diffCallback = DataDiffCallback(this.data, data)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.data.clear()
        this.data.addAll(data)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = data[position]
        holder.content.text = item.content
        holder.type.text = item.type
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val content: TextView = itemView.findViewById(R.id.content)
        val type: TextView = itemView.findViewById(R.id.type)
    }

    private class DataDiffCallback(private val old: List<FilterListItem>,
                                   private val new: List<FilterListItem>) : DiffUtil.Callback() {

        override fun areItemsTheSame(p0: Int, p1: Int): Boolean {
            return old[p0].info == new[p1].info
        }

        override fun getOldListSize(): Int {
            return old.size
        }

        override fun getNewListSize(): Int {
            return new.size
        }

        override fun areContentsTheSame(p0: Int, p1: Int): Boolean {
            return old[p0] == new[p1]
        }
    }
}