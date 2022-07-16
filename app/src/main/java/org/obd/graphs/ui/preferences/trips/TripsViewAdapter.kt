package org.obd.graphs.ui.preferences.trips

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.trip.TripFileDesc
import org.obd.graphs.bl.trip.TripRecorder
import org.obd.graphs.ui.common.Colors
import org.obd.graphs.ui.common.setText
import org.obd.graphs.ui.graph.LOADED_TRIP_PREFERENCE_ID
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.profile.getProfileList
import org.obd.graphs.ui.preferences.updateString

class TripsViewAdapter internal constructor(
    context: Context?,
    private var data: MutableCollection<TripFileDesc>
) : RecyclerView.Adapter<TripsViewAdapter.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val profileColors = mutableMapOf<String,Int>().apply {
        val colors = Colors().generate()
        getProfileList().forEach { (s, _) ->
            put(s,colors.nextInt())
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.trip_item, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        data.elementAt(position).run {
            holder.vehicleProfile.setText(profileLabel, profileColors[profileId]!!, 0.6f)
            holder.tripStartDate.setText(startTime, Color.parseColor("#FFFFFF"), 0.9f)

            holder.tripTime.let {
                val seconds: Int = tripTimeSec.toInt() % 60
                var hours: Int = tripTimeSec.toInt() / 60
                val minutes = hours % 60
                hours /= 60
                val text = "${hours}:${minutes}:${seconds}s"

                it.setText(text, Color.parseColor("#FFFFFF"), 0.9f)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var vehicleProfile: TextView = itemView.findViewById(R.id.vehicle_profile)
        var tripStartDate: TextView = itemView.findViewById(R.id.trip_start_date)
        var tripTime: TextView = itemView.findViewById(R.id.trip_time)
        private var loadTrip: Button = itemView.findViewById(R.id.trip_load)
        private var deleteTrip: Button = itemView.findViewById(R.id.trip_delete)

        init {

            loadTrip.setOnClickListener {
                val trip = data.elementAt(adapterPosition)
                Log.i("TripsViewAdapter", "Trip selected to load: $trip")
                Prefs.updateString(LOADED_TRIP_PREFERENCE_ID, trip.fileName)
            }

            deleteTrip.setOnClickListener {
                val trip = data.elementAt(adapterPosition)
                Log.i("TripsViewAdapter", "Trip selected to delete: $trip")
                data.remove(trip)
                notifyItemChanged(adapterPosition)
                TripRecorder.instance.deleteTrip(trip)
            }

        }
    }
}