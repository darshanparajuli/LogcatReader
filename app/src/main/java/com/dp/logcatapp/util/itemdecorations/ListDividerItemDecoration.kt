package com.dp.logcatapp.util.itemdecorations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class ListDividerItemDecoration : RecyclerView.ItemDecoration {

    companion object {
        const val HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL
        const val VERTICAL_LIST = LinearLayoutManager.VERTICAL
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
    }

    private var divider: Drawable
    private var orientation: Int
    private var leftOffset: Int

    constructor(context: Context, orientation: Int) :
            this(context, orientation, 0)

    constructor(context: Context, orientation: Int, leftOffset: Int) :
            this(context, orientation, null, leftOffset)

    constructor(context: Context, orientation: Int, divider: Drawable?,
                leftOffset: Int) {
        checkOrientation(orientation)
        this.orientation = orientation
        this.divider = if (divider == null) {
            val a = context.obtainStyledAttributes(ATTRS)
            try {
                a.getDrawable(0)
            } finally {
                a.recycle()
            }
        } else {
            divider
        }
        this.leftOffset = leftOffset
    }

    fun setOrientation(orientation: Int) {
        checkOrientation(orientation)
        this.orientation = orientation
    }

    private fun checkOrientation(orientation: Int) {
        if (orientation != HORIZONTAL_LIST &&
                orientation != VERTICAL_LIST) {
            throw IllegalStateException("invalid orientation")
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (orientation == VERTICAL_LIST) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        val top = parent.paddingTop
        val bottom = parent.height - parent.paddingBottom
        val childCount = parent.childCount
        for (i in 0..childCount) {
            val child = parent.getChildAt(i)
            if (parent.getChildAdapterPosition(child) == parent.layoutManager.itemCount - 1) {
                continue
            }

            val params = child.layoutParams as RecyclerView.LayoutParams
            val left = child.right + params.rightMargin
            val right = left + divider.intrinsicHeight
            divider.setBounds(left, top, right, bottom)
            divider.draw(canvas)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (parent.getChildAdapterPosition(child) == parent.layoutManager.itemCount - 1) {
                continue
            }

            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight
            divider.setBounds(left + leftOffset, top, right, bottom)
            divider.draw(canvas)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State) {
        if (parent.getChildAdapterPosition(view) == parent.layoutManager.itemCount - 1) {
            return
        }

        if (orientation == VERTICAL_LIST) {
            outRect.set(0, 0, 0, divider.intrinsicHeight)
        } else {
            outRect.set(0, 0, divider.intrinsicWidth, 0)
        }
    }
}