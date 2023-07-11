package org.obd.graphs.preferences.metadata

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.VehicleMetadata
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.setText

class VehicleMetadataViewAdapter internal constructor(
    context: Context?,
    private var data: MutableCollection<VehicleMetadata>
) : RecyclerView.Adapter<VehicleMetadataViewAdapter.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.item_vehicle_metadata, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        data.elementAt(position).run {
            holder.name.setText(name, COLOR_PHILIPPINE_GREEN,Typeface.NORMAL, 0.8f)
            holder.value.setText(value, Color.GRAY,Typeface.NORMAL, 1f)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.metadata_name)
        var value: TextView = itemView.findViewById(R.id.metadata_value)
    }
}