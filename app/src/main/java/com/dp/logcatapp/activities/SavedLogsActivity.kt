package com.dp.logcatapp.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.commit
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.savedlogs.SavedLogsFragment

class SavedLogsActivity : BaseActivityWithToolbar() {

  companion object {
    val TAG = SavedLogsActivity::class.qualifiedName
  }

  lateinit var cabToolbar: Toolbar
    private set
  private var cabToolbarCallback: CabToolbarCallback? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_saved_logs)
    setupToolbar()
    enableDisplayHomeAsUp()

    onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (cabToolbar.visibility == View.VISIBLE) {
          closeCabToolbar()
          return
        }
        finish()
      }
    })

    cabToolbar = findViewById(R.id.cabToolbar)
    ViewCompat.setOnApplyWindowInsetsListener(cabToolbar) { v, windowInsets ->
      val insets = windowInsets.getInsets(systemBars() or displayCutout())
      v.updateLayoutParams<MarginLayoutParams> {
        topMargin = insets.top
        leftMargin = insets.left
        rightMargin = insets.right
      }
      WindowInsetsCompat.CONSUMED
    }
    cabToolbar.inflateMenu(R.menu.saved_logs_cab)
    cabToolbar.isClickable = true
    cabToolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp)
    cabToolbar.setNavigationOnClickListener {
      closeCabToolbar()
    }

    if (savedInstanceState == null) {
      supportFragmentManager.commit {
        replace(R.id.content_frame, SavedLogsFragment(), SavedLogsFragment.TAG)
      }
    }
  }

  fun openCabToolbar(
    callback: CabToolbarCallback,
    menuItemClickListener: Toolbar.OnMenuItemClickListener
  ): Boolean {
    cabToolbar.setOnMenuItemClickListener(menuItemClickListener)
    cabToolbarCallback = callback

    cabToolbarCallback?.onCabToolbarOpen(cabToolbar)
    cabToolbarCallback?.onCabToolbarInvalidate(cabToolbar)

    cabToolbar.alpha = 0.0f
    cabToolbar.scaleX = 1.25f
    cabToolbar.scaleY = 0.75f
    cabToolbar.visibility = View.VISIBLE
    cabToolbar.animate()
      .alpha(1.0f)
      .scaleX(1.0f)
      .scaleY(1.0f)
      .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
      .setInterpolator(DecelerateInterpolator())
      .setListener(null)
    return true
  }

  fun closeCabToolbar() {
    cabToolbarCallback?.onCabToolbarClose(cabToolbar)
    cabToolbarCallback = null

    cabToolbar.setOnMenuItemClickListener(null)
    cabToolbar.animate()
      .alpha(0.0f)
      .scaleX(1.25f)
      .scaleY(0.75f)
      .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
      .setInterpolator(DecelerateInterpolator())
      .setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          cabToolbar.visibility = View.GONE
        }
      })
  }

  fun invalidateCabToolbarMenu() {
    cabToolbarCallback?.onCabToolbarInvalidate(cabToolbar)
  }

  fun isCabToolbarActive() = cabToolbar.visibility == View.VISIBLE

  fun setCabToolbarTitle(title: String) {
    cabToolbar.title = title
  }

  override fun getToolbarIdRes(): Int = R.id.toolbar

  override fun getToolbarTitle(): String = getString(R.string.saved_logs)
}

interface CabToolbarCallback {
  fun onCabToolbarOpen(toolbar: Toolbar)

  fun onCabToolbarInvalidate(toolbar: Toolbar)

  fun onCabToolbarClose(toolbar: Toolbar)
}