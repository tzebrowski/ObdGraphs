/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.ui.common

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.sendBroadcastEvent

const val TOGGLE_TOOLBAR_ACTION: String = "toolbar.toggle.event"


private class DoubleClickGestureListener :
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
    val gestureDetector = GestureDetector(context, DoubleClickGestureListener())
    return View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) v.performClick()

        gestureDetector.onTouchEvent(
            event
        )
    }
}