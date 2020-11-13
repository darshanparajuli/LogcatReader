package com.dp.logcatapp.fragments.savedlogsviewer

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dp.logcat.Log
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.BaseActivityWithToolbar
import com.dp.logcatapp.fragments.base.BaseFragment
import com.dp.logcatapp.fragments.shared.dialogs.CopyToClipboardDialogFragment
import com.dp.logcatapp.util.containsIgnoreCase
import com.dp.logcatapp.util.getAndroidViewModel
import com.dp.logcatapp.util.inflateLayout
import com.dp.logcatapp.util.showToast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedLogsViewerFragment : BaseFragment() {
  companion object {
    val TAG = SavedLogsViewerFragment::class.qualifiedName

    private val KEY_FILE_URI = TAG + "_key_filename"

    fun newInstance(uri: Uri): SavedLogsViewerFragment {
      val bundle = Bundle()
      bundle.putString(KEY_FILE_URI, uri.toString())
      val frag = SavedLogsViewerFragment()
      frag.arguments = bundle
      return frag
    }
  }

  private lateinit var recyclerView: RecyclerView
  private lateinit var linearLayoutManager: LinearLayoutManager
  private lateinit var viewModel: SavedLogsViewerViewModel
  private lateinit var adapter: MyRecyclerViewAdapter
  private lateinit var fabUp: FloatingActionButton
  private lateinit var fabDown: FloatingActionButton
  private lateinit var progressBar: ProgressBar
  private lateinit var textViewEmpty: TextView
  private var ignoreScrollEvent = false
  private var searchViewActive = false
  private var lastLogId = -1
  private var lastSearchRunnable: Runnable? = null
  private var searchTask: Job? = null

  private val hideFabUpRunnable: Runnable = Runnable {
    fabUp.hide()
  }

  private val hideFabDownRunnable: Runnable = Runnable {
    fabDown.hide()
  }

  private val onScrollListener = object : RecyclerView.OnScrollListener() {
    var lastDy = 0

    override fun onScrolled(
      recyclerView: RecyclerView,
      dx: Int,
      dy: Int
    ) {
      if (dy > 0 && lastDy <= 0) {
        hideFabUp()
        showFabDown()
      } else if (dy < 0 && lastDy >= 0) {
        showFabUp()
        hideFabDown()
      }
      lastDy = dy
    }

    override fun onScrollStateChanged(
      recyclerView: RecyclerView,
      newState: Int
    ) {
      when (newState) {
        RecyclerView.SCROLL_STATE_DRAGGING -> {
          viewModel.autoScroll = false
          if (lastDy > 0) {
            hideFabUp()
            showFabDown()
          } else if (lastDy < 0) {
            showFabUp()
            hideFabDown()
          }
        }
        else -> {
          var firstPos = -1
          if (searchViewActive && !viewModel.autoScroll &&
            newState == RecyclerView.SCROLL_STATE_IDLE
          ) {
            firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
            if (firstPos != RecyclerView.NO_POSITION) {
              val log = adapter[firstPos]
              lastLogId = log.id
            }
          }

          val pos = linearLayoutManager.findLastCompletelyVisibleItemPosition()
          if (pos == RecyclerView.NO_POSITION) {
            viewModel.autoScroll = false
            return
          }

          if (ignoreScrollEvent) {
            if (pos == adapter.itemCount) {
              ignoreScrollEvent = false
            }
            return
          }

          if (pos == 0) {
            hideFabUp()
          }

          if (firstPos == RecyclerView.NO_POSITION) {
            firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
          }

          viewModel.scrollPosition = firstPos
          viewModel.autoScroll = pos == adapter.itemCount - 1

          if (viewModel.autoScroll) {
            hideFabUp()
            hideFabDown()
          }
        }
      }
    }
  }

  private fun showFabUp() {
    handler.removeCallbacks(hideFabUpRunnable)
    fabUp.show()
    handler.postDelayed(hideFabUpRunnable, 2000)
  }

  private fun hideFabUp() {
    handler.removeCallbacks(hideFabUpRunnable)
    fabUp.hide()
  }

  private fun showFabDown() {
    handler.removeCallbacks(hideFabDownRunnable)
    fabDown.show()
    handler.postDelayed(hideFabDownRunnable, 2000)
  }

  private fun hideFabDown() {
    handler.removeCallbacks(hideFabDownRunnable)
    fabDown.hide()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    adapter = MyRecyclerViewAdapter(requireActivity())
    viewModel = getAndroidViewModel()
    viewModel.init(Uri.parse(requireArguments().getString(KEY_FILE_URI)))
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? =
    inflateLayout(R.layout.fragment_saved_logs_viewer)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    progressBar = view.findViewById(R.id.progressBar)
    textViewEmpty = view.findViewById(R.id.textViewEmpty)

    recyclerView = view.findViewById(R.id.recyclerView)
    linearLayoutManager = LinearLayoutManager(activity)
    recyclerView.layoutManager = linearLayoutManager
    recyclerView.itemAnimator = null
    recyclerView.addItemDecoration(
      DividerItemDecoration(
        activity,
        linearLayoutManager.orientation
      )
    )
    recyclerView.adapter = adapter

    recyclerView.addOnScrollListener(onScrollListener)

    fabDown = view.findViewById(R.id.fabDown)
    fabDown.setOnClickListener {
      hideFabDown()
      ignoreScrollEvent = true
      viewModel.autoScroll = true
      linearLayoutManager.scrollToPosition(adapter.itemCount - 1)
    }

    fabUp = view.findViewById(R.id.fabUp)
    fabUp.setOnClickListener {
      hideFabUp()
      viewModel.autoScroll = false
      linearLayoutManager.scrollToPositionWithOffset(0, 0)
    }

    hideFabUp()
    hideFabDown()

    adapter.setOnClickListener { v ->
      val pos = linearLayoutManager.getPosition(v)
      if (pos >= 0) {
        viewModel.autoScroll = false
        val log = adapter[pos]
        CopyToClipboardDialogFragment.newInstance(log)
          .show(parentFragmentManager, CopyToClipboardDialogFragment.TAG)
      }
    }

    viewModel.getLogs().observe(viewLifecycleOwner, Observer {
      progressBar.visibility = View.GONE
      if (it != null) {
        when (it) {
          is SavedLogsViewerViewModel.SavedLogsResult.Success -> {
            if (it.logs.isEmpty()) {
              textViewEmpty.visibility = View.VISIBLE
            } else {
              textViewEmpty.visibility = View.GONE
              setLogs(it.logs)
              scrollRecyclerView()
            }
          }
          is SavedLogsViewerViewModel.SavedLogsResult.FileOpenError -> {
            context?.showToast(getString(R.string.error_opening_source))
          }
          is SavedLogsViewerViewModel.SavedLogsResult.FileParseError -> {
            context?.showToast(getString(R.string.unsupported_source))
          }
        }
      } else {
        textViewEmpty.visibility = View.VISIBLE
      }
    })
  }

  override fun onCreateOptionsMenu(
    menu: Menu,
    inflater: MenuInflater
  ) {
    super.onCreateOptionsMenu(menu, inflater)

    inflater.inflate(R.menu.saved_logs_viewer, menu)
    val searchItem = menu.findItem(R.id.action_search)
    val searchView = searchItem.actionView as SearchView

    var reachedBlank = false
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextChange(newText: String): Boolean {
        searchViewActive = true
        removeLastSearchRunnableCallback()

        if (newText.isBlank()) {
          reachedBlank = true
          onSearchViewClose()
        } else {
          reachedBlank = false
          lastSearchRunnable = Runnable {
            viewModel.getLogs().value?.let {
              if (it is SavedLogsViewerViewModel.SavedLogsResult.Success) {
                runSearchTask(it.logs, newText)
              }
            }
          }.also {
            handler.postDelayed(it, 300)
          }
        }
        return true
      }

      override fun onQueryTextSubmit(query: String) = false
    })

    searchView.setOnCloseListener {
      removeLastSearchRunnableCallback()
      searchViewActive = false
      if (!reachedBlank) {
        onSearchViewClose()
      }
      false
    }
  }

  private fun onSearchViewClose() {
    val result = viewModel.getLogs().value

    val logs: List<Log>
    if (result !is SavedLogsViewerViewModel.SavedLogsResult.Success) {
      logs = emptyList()
    } else {
      logs = result.logs
    }
    setLogs(logs)

    if (lastLogId == -1) {
      scrollRecyclerView()
    } else {
      viewModel.autoScroll = linearLayoutManager.findLastCompletelyVisibleItemPosition() ==
        adapter.itemCount - 1
      if (!viewModel.autoScroll) {
        viewModel.scrollPosition = lastLogId
        linearLayoutManager.scrollToPositionWithOffset(lastLogId, 0)
      }
      lastLogId = -1
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_search -> {
        true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  private fun removeLastSearchRunnableCallback() {
    lastSearchRunnable?.let {
      handler.removeCallbacks(it)
    }
    lastSearchRunnable = null
  }

  override fun onDestroy() {
    super.onDestroy()
    (activity as BaseActivityWithToolbar).toolbar.subtitle = null
    removeLastSearchRunnableCallback()
    searchTask?.cancel()
    recyclerView.removeOnScrollListener(onScrollListener)
  }

  private fun setLogs(logs: List<Log>) {
    if (adapter.itemCount == logs.size) {
      return
    }

    adapter.setItems(logs)

    if (logs.isEmpty()) {
      (activity as BaseActivityWithToolbar).toolbar.subtitle = null
    } else {
      (activity as BaseActivityWithToolbar).toolbar.subtitle = "${logs.size}"
    }
  }

  private fun scrollRecyclerView() {
    if (viewModel.autoScroll) {
      linearLayoutManager.scrollToPosition(adapter.itemCount - 1)
    } else {
      linearLayoutManager.scrollToPositionWithOffset(viewModel.scrollPosition, 0)
    }
  }

  private fun runSearchTask(
    logs: List<Log>,
    searchText: String
  ) {
    searchTask?.cancel()
    searchTask = scope.launch {
      val filteredLogs = withContext(IO) {
        logs.filter {
          it.tag.containsIgnoreCase(searchText) ||
            it.msg.containsIgnoreCase(searchText)
        }
      }
      adapter.setItems(filteredLogs)
      viewModel.autoScroll = false
      linearLayoutManager.scrollToPositionWithOffset(0, 0)
    }
  }
}