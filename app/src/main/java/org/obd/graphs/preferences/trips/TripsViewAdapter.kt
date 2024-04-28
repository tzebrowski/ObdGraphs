/**
 * Copyright 2019-2024, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.preferences.trips

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.*
import org.obd.graphs.bl.trip.TripFileDesc
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.profile.profile
import org.obd.graphs.ui.common.Colors
import org.obd.graphs.ui.common.setText
import java.text.SimpleDateFormat
import java.util.*

private const val LOGGER_KEY = "TripsViewAdapter"

class TripsViewAdapter internal constructor(
    context: Context?,
    var data: MutableCollection<TripFileDesc>
) : RecyclerView.Adapter<TripsViewAdapter.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())

    private val profileColors = mutableMapOf<String, Int>().apply {
        val colors = Colors().generate()
        profile.getAvailableProfiles().forEach { (s, _) ->
            if (colors.hasNext()) {
                put(s, colors.nextInt())
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(mInflater.inflate(R.layout.item_trip, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        data.elementAt(position).run {
            holder.vehicleProfile.setText(
                profileLabel,
                profileColors[profileId]!!,
                Typeface.NORMAL,
                0.6f
            )
            var startTs = startTime
            startTime.toLongOrNull()?.let {
                startTs = dateFormat.format(Date(it))
            }

            holder.tripStartDate.setText(startTs, Color.GRAY, Typeface.NORMAL, 0.9f)

            holder.tripTime.let {
                val seconds: Int = tripTimeSec.toInt() % 60
                var hours: Int = tripTimeSec.toInt() / 60
                val minutes = hours % 60
                hours /= 60
                val text = "${hours.toString().padStart(2, '0')}:${
                    minutes.toString().padStart(2, '0')
                }:${seconds.toString().padStart(2, '0')}s"

                it.setText(text, Color.GRAY, Typeface.BOLD, 0.9f)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var vehicleProfile: TextView = itemView.findViewById(R.id.vehicle_profile)
        var tripStartDate: TextView = itemView.findViewById(R.id.trip_start_date)
        var tripTime: TextView = itemView.findViewById(R.id.trip_length)
        private var loadTrip: Button = itemView.findViewById(R.id.trip_load)
        private var deleteTrip: Button = itemView.findViewById(R.id.trip_delete)

        init {

            loadTrip.setOnClickListener {
                tripManager.loadTripAsync( data.elementAt(adapterPosition).fileName)
            }

            deleteTrip.setOnClickListener {
                val builder = AlertDialog.Builder(itemView.context)
                val title = itemView.context.getString(R.string.trip_delete_dialog_ask_question)
                val yes = itemView.context.getString(R.string.trip_delete_dialog_ask_question_yes)
                val no = itemView.context.getString(R.string.trip_delete_dialog_ask_question_no)

                builder.setMessage(title)
                    .setCancelable(false)
                    .setPositiveButton(yes) { _, _ ->
                        val trip = data.elementAt(adapterPosition)
                        Log.i(LOGGER_KEY, "Trip selected to delete: $trip")
                        data.remove(trip)
                        tripManager.deleteTrip(trip)
                        notifyDataSetChanged()
                    }
                    .setNegativeButton(no) { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.create().show()
            }
        }
    }
}