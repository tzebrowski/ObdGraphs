package org.obd.graphs.ui.common

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import java.util.Collections

abstract class SimpleAdapter<T : RecyclerView.ViewHolder>(
    protected val context: Context,
    val data: MutableList<ObdMetric>,
    protected val resourceId: Int,
    protected val height: Int? = null
) : RecyclerView.Adapter<T>() {

    fun swapItems(fromPosition: Int, toPosition: Int){
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }
}