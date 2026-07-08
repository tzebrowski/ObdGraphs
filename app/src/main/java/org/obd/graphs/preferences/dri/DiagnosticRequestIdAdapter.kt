package org.obd.graphs.preferences.dri

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R

class DiagnosticRequestIdAdapter(
    private val items: MutableList<DiagnosticMappingItem>,
    private val onEditClicked: (DiagnosticMappingItem) -> Unit,
    private val onDeleteClicked: (DiagnosticMappingItem) -> Unit
) : RecyclerView.Adapter<DiagnosticRequestIdAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKey: TextView = view.findViewById(R.id.tvKey)
        val tvValue: TextView = view.findViewById(R.id.tvValue)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diagnostic_request_id, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvKey.text = "Key: ${item.requestKey}"
        holder.tvValue.text = "Value: ${item.headerValue}"

        holder.btnEdit.setOnClickListener { onEditClicked(item) }
        holder.btnDelete.setOnClickListener { onDeleteClicked(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DiagnosticMappingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
