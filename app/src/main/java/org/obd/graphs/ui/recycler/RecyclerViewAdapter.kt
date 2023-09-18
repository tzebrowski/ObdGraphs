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
package org.obd.graphs.ui.recycler

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.bl.collector.CarMetric
import java.util.Collections

abstract class RecyclerViewAdapter<T : RecyclerView.ViewHolder>(
    protected val context: Context,
    val data: MutableList<CarMetric>,
    protected val resourceId: Int,
    protected val height: Int? = null
) : RecyclerView.Adapter<T>() {

    fun swapItems(fromPosition: Int, toPosition: Int){
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }
}