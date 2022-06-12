package org.openobd2.core.logger.ui.common

import android.content.Context
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

interface SwappableAdapter {
    fun swapItems(fromPosition: Int, toPosition: Int)
    fun deleteItems(fromPosition: Int)
    fun storePreferences(context: Context)
}

internal class DragManageAdapter(
    private var context: Context,
    dragDirs: Int,
    swipeDirs: Int,
    private var adapter: SwappableAdapter
) :
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.swapItems(viewHolder.adapterPosition, target.adapterPosition)
        adapter.storePreferences(context)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.deleteItems(viewHolder.adapterPosition)
    }
}