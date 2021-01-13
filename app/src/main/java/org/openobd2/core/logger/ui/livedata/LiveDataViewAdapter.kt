package org.openobd2.core.logger.ui.livedata


import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.dash.valueAsString


class LiveDataViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<CommandReply<*>>
) :
    RecyclerView.Adapter<LiveDataViewAdapter.ViewHolder>() {
    var mData: MutableCollection<CommandReply<*>> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.livedata_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val commandReply = mData.elementAt(position)
        var valueTxt: String? = commandReply.valueAsString()
        if (valueTxt != null) {
            valueTxt += " " + (commandReply.command as ObdCommand).pid.units
        }

        holder.metricNameTextView.text =
            UIUtils.spannedText(commandReply.command.label, Color.GRAY, 1.1f)
        holder.metricValueTextView.text =
            UIUtils.spannedText(valueTxt, Color.parseColor("#01804F"), 1.4f)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var metricNameTextView: TextView
        var metricValueTextView: TextView
        override fun onClick(view: View?) {

        }

        init {
            metricNameTextView = itemView.findViewById(R.id.metric_name)
            metricValueTextView = itemView.findViewById(R.id.metric_value)

            itemView.setOnClickListener(this)
        }
    }
}