package org.obd.graphs.ui.common

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.sendBroadcastEvent

const val TOGGLE_TOOLBAR_ACTION: String = "TOGGLE_TOOLBAR"

private class DoubleClickGestureListener(val context: Context) :
    GestureDetector.SimpleOnGestureListener() {
    override fun onDoubleTap(e: MotionEvent): Boolean {
        sendBroadcastEvent(TOGGLE_TOOLBAR_ACTION)
        return true
    }
}


open class ToggleToolbarDoubleClickListener(
    context: Context?
) : RecyclerView.OnItemTouchListener {


    private var gestureDetector: GestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return true
            }
        })

    override fun onInterceptTouchEvent(view: RecyclerView, e: MotionEvent): Boolean {
        val childView = view.findChildViewUnder(e.x, e.y)
        if (childView != null && gestureDetector.onTouchEvent(e)) {
            sendBroadcastEvent(TOGGLE_TOOLBAR_ACTION)
        }
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    override fun onTouchEvent(view: RecyclerView, motionEvent: MotionEvent) {}
}

fun onDoubleClickListener(context: Context): View.OnTouchListener {
    val gestureDetector = GestureDetector(context, DoubleClickGestureListener(context))
    return View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) v.performClick()

        gestureDetector.onTouchEvent(
            event
        )
    }
}