 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.preferences.trips

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.profile.profile
import org.obd.graphs.ui.common.Colors
import org.obd.graphs.ui.common.setText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LOGGER_KEY = "TripsViewAdapter"

class TripViewAdapter internal constructor(
    context: Context?,
    var data: MutableCollection<TripLogDetails>,
    private val showDeleteButton: Boolean = true,
) : RecyclerView.Adapter<TripViewAdapter.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())

    private val profileColors =
        mutableMapOf<String, Int>().apply {
            val colors = Colors().get()
            profile.getAvailableProfiles().forEach { (s, _) ->
                if (colors.hasNext()) {
                    put(s, colors.nextInt())
                }
            }
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder = ViewHolder(mInflater.inflate(R.layout.item_trip, parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        data.elementAt(position).run {
            holder.vehicleProfile.setText(
                source.profileLabel,
                profileColors[source.profileId]!!,
                Typeface.NORMAL,
                0.6f,
            )
            var startTs = source.startTime
            source.startTime.toLongOrNull()?.let {
                startTs = dateFormat.format(Date(it))
            }

            holder.tripStartDate.setText(startTs, Color.GRAY, Typeface.NORMAL, 0.9f)

            holder.selected.isChecked = checked
            holder.selected.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isShown) {
                    checked = isChecked
                }
            }

            holder.tripTime.let {
                val seconds: Int = source.tripTimeSec.toInt() % 60
                var hours: Int = source.tripTimeSec.toInt() / 60
                val minutes = hours % 60
                hours /= 60
                val text = "${hours.toString().padStart(2, '0')}:${
                    minutes.toString().padStart(2, '0')
                }:${seconds.toString().padStart(2, '0')}s"

                it.setText(text, Color.GRAY, Typeface.BOLD, 0.9f)
            }
        }
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder internal constructor(
        binding: View,
    ) : RecyclerView.ViewHolder(binding) {
        val selected: CheckBox = binding.findViewById(R.id.trip_selected)
        val vehicleProfile: TextView = binding.findViewById(R.id.trip_profile)
        val tripStartDate: TextView = binding.findViewById(R.id.trip_start_date)
        val tripTime: TextView = binding.findViewById(R.id.trip_length)
        private val loadTrip: Button = binding.findViewById(R.id.trip_load)
        private val deleteTrip: Button = binding.findViewById(R.id.trip_delete)
        private val actionsContainer: LinearLayout = binding.findViewById(R.id.actions_container)

        init {
            if (showDeleteButton) {
                loadTrip.setOnClickListener {
                    tripManager.loadTripAsync(data.elementAt(adapterPosition).source.fileName)
                }

                deleteTrip.setOnClickListener {
                    val builder = AlertDialog.Builder(binding.context)
                    val title = binding.context.getString(R.string.trip_delete_dialog_ask_question)
                    val yes = binding.context.getString(R.string.dialog_ask_question_yes)
                    val no = binding.context.getString(R.string.dialog_ask_question_no)

                    builder
                        .setMessage(title)
                        .setCancelable(false)
                        .setPositiveButton(yes) { _, _ ->
                            val trip = data.elementAt(adapterPosition)
                            Log.i(LOGGER_KEY, "Trip selected to delete: $trip")
                            data.remove(trip)
                            tripManager.deleteTrip(trip.source)
                            notifyDataSetChanged()
                        }.setNegativeButton(no) { dialog, _ ->
                            dialog.dismiss()
                        }
                    builder.create().show()
                }
            } else {
                loadTrip.visibility = View.GONE
                deleteTrip.visibility = View.GONE
                actionsContainer.visibility = View.GONE
            }
        }
    }
}
