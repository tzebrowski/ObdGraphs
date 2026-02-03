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
package org.obd.graphs.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.query.Query

abstract class BaseFragment : Fragment() {

    protected var dataLogger: DataLoggerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as DataLoggerService.LocalBinder
            dataLogger = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            dataLogger = null
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(requireContext(), DataLoggerService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            requireContext().unbindService(serviceConnection)
            isBound = false
        }
        dataLogger = null
    }

    protected fun attachToFloatingButton(
        activity: Activity?,
        query: Query
    ) {
        activity?.let {
            val btn = activity.findViewById<FloatingActionButton>(R.id.connect_btn)
            btn?.setOnClickListener {

                if (DataLoggerRepository.isRunning()) {
                    Log.i("BaseFragment", "DragRacingFragment: Start data logging")
                    dataLogger?.stop()
                } else {
                    Log.i("BaseFragment", "Start data logging")
                    dataLogger?.start(query)
                }
            }

            btn?.backgroundTintList =
                ContextCompat.getColorStateList(activity, if (DataLoggerRepository.isRunning()) org.obd.graphs.commons.R.color.cardinal
                else org.obd.graphs.commons.R.color.philippine_green)
        }
    }
}
