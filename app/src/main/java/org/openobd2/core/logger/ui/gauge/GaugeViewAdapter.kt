package org.openobd2.core.logger.ui.gauge


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.R


class GaugeViewAdapter internal constructor(
    context: Context?,
    data: MutableCollection<CommandReply<*>>
) :
    RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {
    var mData: MutableCollection<CommandReply<*>> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.gauge_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val commandReply = mData.elementAt(position)
        var valueTxt: String? = commandReply.value?.toString()
        if (valueTxt != null) {
            valueTxt += " " + (commandReply.command as ObdCommand).pid.units
        }

        holder.labelTextView.text = commandReply.command.label

        holder.valueTextView.text =valueTxt

    }
    override fun getItemCount(): Int {
        return mData.size
    }
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var labelTextView: TextView
        var valueTextView: TextView

        override fun onClick(view: View?) {

        }

        init {
            valueTextView = itemView.findViewById(R.id.value)
            labelTextView  = itemView.findViewById(R.id.label)

            itemView.setOnClickListener(this)
        }
    }
}