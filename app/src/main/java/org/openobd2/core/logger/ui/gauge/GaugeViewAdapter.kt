package org.openobd2.core.logger.ui.gauge


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLogger
import java.util.*


class GaugeViewAdapter internal constructor(
    val context: Context,
    val data: MutableList<ObdMetric>,
    val resourceId: Int
) :
    RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {
    var mData: MutableList<ObdMetric> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var view: View
    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(mData, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        view = mInflater.inflate(resourceId, parent, false)
        view.layoutParams.height = 200

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val metric = mData.elementAt(position)
        holder.labelTextView.text = metric.command.label
        holder.unitsTextView.text = (metric.command as ObdCommand).pid.units
        holder.valueTextView.text = metric.valueToString()

        holder.minTextView.text = ""
        holder.maxTextView.text = ""

        val statistic =
            DataLogger.INSTANCE.statistics().findBy(metric.command.pid)
        holder.minTextView.text = statistic.min.toString()
        holder.maxTextView.text = statistic.max.toString()

//        animation()

    }

    private fun animation() {
        val colorFrom = ContextCompat.getColor(this.context, R.color.purple_200)
        val colorTo = ContextCompat.getColor(this.context, R.color.purple_500)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 3000

        val color1 = ContextCompat.getColor(this.context, R.color.purple_200)
        val color2 = ContextCompat.getColor(this.context, R.color.purple_500)
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(color1, color2)
        )
        gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT

        colorAnimation.addUpdateListener { animator ->
            gradientDrawable.colors = intArrayOf(animator.animatedValue as Int, color2)
            view.background = gradientDrawable
        }

        colorAnimation.start()
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var labelTextView: TextView = itemView.findViewById(R.id.label)
        var valueTextView: TextView = itemView.findViewById(R.id.value)
        var unitsTextView: TextView = itemView.findViewById(R.id.unit)
        var minTextView: TextView = itemView.findViewById(R.id.min_value)
        var maxTextView: TextView = itemView.findViewById(R.id.max_value)
    }
}