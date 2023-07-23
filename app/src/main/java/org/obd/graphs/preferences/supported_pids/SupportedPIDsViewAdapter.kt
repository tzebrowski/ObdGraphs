package org.obd.graphs.preferences.supported_pids


import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.setText

class SupportedPIDsViewAdapter internal constructor(
    context: Context?,
    private var data: MutableList<String>
) : RecyclerView.Adapter<SupportedPIDsViewAdapter.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.item_supported_pids, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        data.elementAt(position).run {
            val pidList = dataLogger.getPidDefinitionRegistry().findAll()
            val pid = pidList.firstOrNull { it.pid == uppercase() }
            if  (pid == null) {
                holder.mode.setText("", COLOR_PHILIPPINE_GREEN,Typeface.NORMAL, 0.7f)
                holder.name.setText("PID ${uppercase()}", Color.GRAY,Typeface.NORMAL, 1f)
                holder.status.setText("not supported", Color.RED,Typeface.NORMAL, 1f)
            } else{
                holder.mode.setText(pid.resourceFile, COLOR_PHILIPPINE_GREEN,Typeface.NORMAL, 0.7f)
                holder.name.setText(pid.description, Color.GRAY,Typeface.NORMAL, 1f)
                holder.status.setText("supported", Color.GRAY,Typeface.NORMAL, 1f)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mode: TextView = itemView.findViewById(R.id.supported_pid_file)
        var name: TextView = itemView.findViewById(R.id.supported_pid_name)
        var status: TextView = itemView.findViewById(R.id.supported_pid_status)
    }
}