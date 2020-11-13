package com.dp.logcatapp.fragments.filters

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dp.logcat.Log
import com.dp.logcatapp.R
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.filters.dialogs.FilterDialogFragment
import com.dp.logcatapp.model.LogcatMsg
import com.dp.logcatapp.util.getAndroidViewModel
import com.dp.logcatapp.util.inflateLayout

class FiltersFragment : BaseFragment() {

  companion object {
    val TAG = FiltersFragment::class.qualifiedName
    private val KEY_EXCLUSIONS = TAG + "_key_exclusions"
    private val KEY_LOG = TAG + "_key_log"

    fun newInstance(
      log: Log?,
      exclusions: Boolean
    ): FiltersFragment {
      val frag = FiltersFragment()
      val bundle = Bundle()
      bundle.putBoolean(KEY_EXCLUSIONS, exclusions)
      bundle.putParcelable(KEY_LOG, log)
      frag.arguments = bundle
      return frag
    }
  }

  private lateinit var viewModel: FiltersViewModel
  private lateinit var recyclerViewAdapter: MyRecyclerViewAdapter
  private lateinit var linearLayoutManager: LinearLayoutManager
  private lateinit var emptyMessage: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    viewModel = requireActivity().getAndroidViewModel()
    recyclerViewAdapter = MyRecyclerViewAdapter {
      onRemoveClicked(it)
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.getFilters(isExclusions()).observe(viewLifecycleOwner, Observer {
      if (it != null) {
        if (it.isEmpty()) {
          emptyMessage.visibility = View.VISIBLE
        } else {
          emptyMessage.visibility = View.GONE
        }
        recyclerViewAdapter.setData(it)
      }
    })
  }

  override fun onResume() {
    super.onResume()
    if (getLog() != null) showAddFilter()
  }

  fun isExclusions() = arguments?.getBoolean(KEY_EXCLUSIONS) ?: false

  private fun getLog() = arguments?.getParcelable<Log>(KEY_LOG)

  @SuppressLint("CheckResult")
  private fun onRemoveClicked(v: View) {
    val pos = linearLayoutManager.getPosition(v)
    if (pos != RecyclerView.NO_POSITION) {
      val item = recyclerViewAdapter[pos]
      viewModel.deleteFilter(item.info, isExclusions())
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val rootView = inflateLayout(R.layout.filters_fragment)

    emptyMessage = rootView.findViewById(R.id.textViewEmpty)

    val activity = requireActivity()
    val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerView)
    recyclerView.addItemDecoration(
      DividerItemDecoration(
        activity,
        DividerItemDecoration.VERTICAL
      )
    )
    linearLayoutManager = LinearLayoutManager(activity)
    recyclerView.layoutManager = linearLayoutManager
    recyclerView.adapter = recyclerViewAdapter

    return rootView
  }

  override fun onCreateOptionsMenu(
    menu: Menu,
    inflater: MenuInflater
  ) {
    inflater.inflate(R.menu.filters, menu)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.add_action -> {
        showAddFilter()
        true
      }
      R.id.clear_action -> {
        viewModel.deleteAllFilters(isExclusions())
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun showAddFilter() {
    var frag =
      parentFragmentManager.findFragmentByTag(FilterDialogFragment.TAG) as? FilterDialogFragment
    if (frag == null) {
      frag = FilterDialogFragment.newInstance(getLog())
    }

    frag.setTargetFragment(this, 0)
    frag.show(parentFragmentManager, FilterDialogFragment.TAG)
  }

  @SuppressLint("CheckResult")
  fun addFilter(logcatMsg: LogcatMsg) {
    val list = mutableListOf<FilterInfo>()
    val exclude = isExclusions()

    if (logcatMsg.keyword.isNotEmpty()) {
      list.add(FilterInfo(FilterType.KEYWORD, logcatMsg.keyword, exclude))
    }

    if (logcatMsg.tag.isNotEmpty()) {
      list.add(FilterInfo(FilterType.TAG, logcatMsg.tag, exclude))
    }

    if (logcatMsg.pid.isNotEmpty()) {
      list.add(FilterInfo(FilterType.PID, logcatMsg.pid, exclude))
    }

    if (logcatMsg.tid.isNotEmpty()) {
      list.add(FilterInfo(FilterType.TID, logcatMsg.tid, exclude))
    }

    if (logcatMsg.logLevels.isNotEmpty()) {
      list.add(
        FilterInfo(
          FilterType.LOG_LEVELS,
          logcatMsg.logLevels.sorted().joinToString(","), exclude
        )
      )
    }

    if (list.isNotEmpty()) {
      viewModel.addFilters(list, isExclusions())
    }
  }
}

data class FilterListItem(
  val type: String,
  val content: String,
  val info: FilterInfo
)

internal class MyRecyclerViewAdapter(private val onRemoveListener: (View) -> Unit) :
  RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {

  private val data = mutableListOf<FilterListItem>()

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): MyViewHolder {
    val view = parent.context.inflateLayout(R.layout.filters_fragment_list_item, parent)
    view.findViewById<ImageButton>(R.id.removeFilterIcon).setOnClickListener {
      onRemoveListener(it.parent as ViewGroup)
    }
    return MyViewHolder(view)
  }

  override fun getItemCount() = data.size

  operator fun get(index: Int) = data[index]

  fun setData(data: List<FilterListItem>) {
    val diffCallback = DataDiffCallback(this.data, data)
    val diffResult = DiffUtil.calculateDiff(diffCallback)

    this.data.clear()
    this.data.addAll(data)
    diffResult.dispatchUpdatesTo(this)
  }

  override fun onBindViewHolder(
    holder: MyViewHolder,
    position: Int
  ) {
    val item = data[position]
    holder.content.text = item.content
    holder.type.text = item.type
  }

  class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val content: TextView = itemView.findViewById(R.id.content)
    val type: TextView = itemView.findViewById(R.id.type)
  }

  private class DataDiffCallback(
    private val old: List<FilterListItem>,
    private val new: List<FilterListItem>
  ) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
      return old.size
    }

    override fun getNewListSize(): Int {
      return new.size
    }

    override fun areItemsTheSame(
      p0: Int,
      p1: Int
    ): Boolean {
      return old[p0].info == new[p1].info
    }

    override fun areContentsTheSame(
      p0: Int,
      p1: Int
    ): Boolean {
      return old[p0] == new[p1]
    }
  }
}