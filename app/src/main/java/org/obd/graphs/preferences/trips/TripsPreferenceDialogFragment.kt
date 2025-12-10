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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.*
import org.obd.graphs.activity.navigateToScreen
import org.obd.graphs.bl.trip.TripFileDesc
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.preferences.CoreDialogFragment

 data class TripFileDescDetails(
     val source: TripFileDesc,
     var checked: Boolean = false,
 )

 class TripsPreferenceDialogFragment : CoreDialogFragment() {

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        requestWindowFeatures()

        val root = inflater.inflate(R.layout.dialog_trip, container, false)
        val adapter = TripViewAdapter(context, tripManager.findAllTripsBy().map { TripFileDescDetails(source=it)}.toMutableList())
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        attachCloseButton(root){
            navigateToScreen(R.id.navigation_graph)
        }

        root.findViewById<Button>(R.id.trip_action_delete_all).apply {
            setOnClickListener {
                val builder = AlertDialog.Builder(context)
                val title = context.getString(R.string.trip_delete_dialog_ask_question)
                val yes = context.getString(R.string.trip_delete_dialog_ask_question_yes)
                val no = context.getString(R.string.trip_delete_dialog_ask_question_no)

                builder.setMessage(title)
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
                    }
                    .setNegativeButton(no) { dialog, _ ->
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

                builder.setMessage(title)
                    .setCancelable(false)
                    .setPositiveButton(yes) { _, _ ->

                        adapter.data.forEach {
                            if (it.checked){
                                Log.e("TripsPreferenceDialogFragment","Selected trip=${it.source.fileName}")
                            }
                        }
                    }
                    .setNegativeButton(no) { dialog, _ ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }


        return root
    }
}
