package org.obd.graphs.preferences.pid


import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_RAINBOW_INDIGO
import org.obd.graphs.ui.common.setText


class PIDsViewAdapter internal constructor(
    context: Context?,
    var data: List<PidDefinitionDetails>
) : RecyclerView.Adapter<PIDsViewAdapter.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.item_pids, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        data.elementAt(position).run {
            holder.mode.setText(source.resourceFile, COLOR_PHILIPPINE_GREEN, Typeface.NORMAL, 0.7f)
            holder.name.setText(source.description, COLOR_RAINBOW_INDIGO, Typeface.NORMAL, 1f)

            if (source.stable) {
                holder.status.setText("Yes", Color.GRAY, Typeface.NORMAL, 0.8f)
            } else {
                holder.status.setText("No", COLOR_DYNAMIC_SELECTOR_SPORT, Typeface.NORMAL, 0.8f)
            }

            if (supported) {
                holder.supported.setText("Yes", Color.GRAY, Typeface.NORMAL, 0.8f)
            } else {
                holder.supported.setText("No", COLOR_DYNAMIC_SELECTOR_SPORT, Typeface.NORMAL, 0.8f)
            }

            holder.selected.isChecked = checked
            holder.selected.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isShown) {
                    checked = isChecked
                }
            }
            if (source.alertLowerThreshold != null || source.alertUpperThreshold != null){
                var text =  ""
                if (source.alertLowerThreshold != null){
                    text += " x<"  + source.alertLowerThreshold
                }

                if (source.alertUpperThreshold != null){
                    text += " x>"  + source.alertUpperThreshold
                }

                holder.alert.setText(text, Color.GRAY, Typeface.NORMAL, 0.6f)
            } else {
                holder.alert.setText("", Color.GRAY, Typeface.NORMAL, 0.6f)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mode: TextView = itemView.findViewById(R.id.pid_file)
        val name: TextView = itemView.findViewById(R.id.pid_name)
        val status: TextView = itemView.findViewById(R.id.pid_status)
        val selected: CheckBox = itemView.findViewById(R.id.pid_selected)
        val alert: TextView = itemView.findViewById(R.id.pid_alert)
        val supported: TextView = itemView.findViewById(R.id.pid_supported)

    }
}