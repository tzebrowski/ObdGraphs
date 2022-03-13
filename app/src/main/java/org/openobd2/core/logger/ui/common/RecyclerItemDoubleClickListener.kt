package org.openobd2.core.logger.ui.common

import android.content.Context
import android.content.Intent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

const val TOGGLE_TOOLBAR_ACTION: String = "TOGGLE_TOOLBAR"

private class GestureListener(val context: Context) : GestureDetector.SimpleOnGestureListener() {
    override fun onDoubleTap(e: MotionEvent): Boolean {
        context.sendBroadcast(Intent().apply {
            action = TOGGLE_TOOLBAR_ACTION
        })
        return true
    }
}

fun onDoubleClickListener(context: Context) : View.OnTouchListener{
    val gestureDetector = GestureDetector(context, GestureListener(context))
    return  View.OnTouchListener { _, event -> gestureDetector.onTouchEvent(
        event
    ) }
}

class ToggleToolbarDoubleClickListener(context: Context?) :
    RecyclerItemDoubleClickListener(context, object :
        OnItemDoubleClickListener {
        override fun onItemDoubleClick(view: View?, position: Int) {
            context?.sendBroadcast(Intent().apply {
                action = TOGGLE_TOOLBAR_ACTION
            })
        }
    })

open class RecyclerItemDoubleClickListener(
    context: Context?,
    private val doubleClickListener: OnItemDoubleClickListener?
) :
    RecyclerView.OnItemTouchListener {

    interface OnItemDoubleClickListener {
        fun onItemDoubleClick(view: View?, position: Int)
    }

    var mGestureDetector: GestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return true
            }
        })

    override fun onInterceptTouchEvent(view: RecyclerView, e: MotionEvent): Boolean {
        val childView = view.findChildViewUnder(e.x, e.y)
        if (childView != null && doubleClickListener != null && mGestureDetector.onTouchEvent(e)) {
            doubleClickListener.onItemDoubleClick(childView, view.getChildPosition(childView))
        }
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    override fun onTouchEvent(view: RecyclerView, motionEvent: MotionEvent) {}
}