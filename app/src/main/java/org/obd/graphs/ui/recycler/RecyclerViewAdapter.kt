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