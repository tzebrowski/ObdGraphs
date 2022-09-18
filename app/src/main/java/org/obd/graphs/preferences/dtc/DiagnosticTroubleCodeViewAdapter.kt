package org.obd.graphs.preferences.dtc

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.common.setText

class DiagnosticTroubleCodeViewAdapter internal constructor(
    context: Context?,
    private var data: MutableCollection<String>
) : RecyclerView.Adapter<DiagnosticTroubleCodeViewAdapter.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.item_dtc, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        data.elementAt(position).run {
            holder.code.setText(this, Color.GRAY,Typeface.NORMAL, 1f)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var code: TextView = itemView.findViewById(R.id.dtc_code)
        var description: TextView = itemView.findViewById(R.id.dtc_description)
    }
}