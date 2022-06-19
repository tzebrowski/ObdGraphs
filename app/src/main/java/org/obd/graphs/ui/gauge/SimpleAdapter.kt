package org.obd.graphs.ui.gauge

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric

abstract class SimpleAdapter<T : RecyclerView.ViewHolder>(
    protected val context: Context,
    val data: MutableList<ObdMetric>,
    protected val resourceId: Int,
    protected val height: Int? = null
) : RecyclerView.Adapter<T>()