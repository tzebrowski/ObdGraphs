/**
 * Copyright 2019-2025, Tomasz Żebrowski
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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.obd.graphs.R
import org.obd.graphs.SCREEN_LOCK_PROGRESS_EVENT
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.TRIPS_UPLOAD_NO_FILES_SELECTED
import org.obd.graphs.activity.navigateToScreen
import org.obd.graphs.bl.trip.TripFileDesc
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.integrations.gcp.gdrive.TripsDriveManager
import org.obd.graphs.preferences.CoreDialogFragment
import org.obd.graphs.sendBroadcastEvent
import java.io.File

data class TripFileDescDetails(
    val source: TripFileDesc,
    var checked: Boolean = false,
)

class TripsListDialogFragment : CoreDialogFragment() {
    private lateinit var tripsDriveManager: TripsDriveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripsDriveManager = TripsDriveManager.instance(getString(R.string.ANDROID_WEB_CLIENT_ID), requireActivity(), this)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        requestWindowFeatures()

        val root = inflater.inflate(R.layout.dialog_trip, container, false)
        val adapter = TripViewAdapter(context, tripManager.findAllTripsBy().map { TripFileDescDetails(source = it) }.toMutableList())
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        attachCloseButton(root) {
            navigateToScreen(R.id.navigation_graph)
        }

        root.findViewById<Button>(R.id.trip_action_delete_all).apply {
            setOnClickListener {
                val builder = AlertDialog.Builder(context)
                val title = context.getString(R.string.trip_delete_dialog_ask_question)
                val yes = context.getString(R.string.trip_delete_dialog_ask_question_yes)
                val no = context.getString(R.string.trip_delete_dialog_ask_question_no)

                builder
                    .setMessage(title)
                    .setCancelable(false)
                    .setPositiveButton(yes) { _, _ ->
                        try {
                            sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT)
                            adapter.data.forEach {
                                tripManager.deleteTrip(it.source)
                            }
                            adapter.data.clear()
                            adapter.notifyDataSetChanged()
                        } finally {
                            sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
                        }
                    }.setNegativeButton(no) { dialog, _ ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }

        root.findViewById<Button>(R.id.trip_action_send_to_cloud).apply {
            setOnClickListener {
                val builder = AlertDialog.Builder(context)
                val title = context.getString(R.string.trip_send_to_cloud_dialog_ask_question)
                val yes = context.getString(R.string.trip_delete_dialog_ask_question_yes)
                val no = context.getString(R.string.trip_delete_dialog_ask_question_no)
                builder
                    .setMessage(title)
                    .setCancelable(false)
                    .setPositiveButton(yes) { _, _ ->
                        val directory = tripManager.getTripsDirectory(context)
                        val files = adapter.data.filter { it.checked }.map { File(directory, it.source.fileName) }
                        if (files.isEmpty()) {
                            Log.w("TripsListDialogFragment", "User selected no tripe")
                            sendBroadcastEvent(TRIPS_UPLOAD_NO_FILES_SELECTED)
                        } else {
                            lifecycleScope.launch {
                                tripsDriveManager.exportTrips(files)
                            }
                        }
                    }.setNegativeButton(no) { dialog, _ ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }

        return root
    }
}
