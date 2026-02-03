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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.bl.datalogger.DataLoggerService

fun Fragment.withDataLogger(onConnected: (DataLoggerService) -> Unit) {
    val connector = DataLoggerConnector(requireContext(), onConnected)
    this.lifecycle.addObserver(connector)
}

fun ComponentActivity.withDataLogger(onConnected: (DataLoggerService) -> Unit) {
    val connector = DataLoggerConnector(this, onConnected)
    this.lifecycle.addObserver(connector)
}

class DataLoggerConnector(
    private val context: Context,
    private val onServiceConnected: (DataLoggerService) -> Unit,
) : DefaultLifecycleObserver {
    private var dataLogger: DataLoggerService? = null
    private var isBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                className: ComponentName,
                service: IBinder,
            ) {
                val binder = service as DataLoggerService.LocalBinder
                val logger = binder.getService()
                dataLogger = logger
                isBound = true
                onServiceConnected(logger)
            }

            override fun onServiceDisconnected(className: ComponentName) {
                dataLogger = null
                isBound = false
            }
        }

    override fun onStart(owner: LifecycleOwner) {
        val intent = Intent(context, DataLoggerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop(owner: LifecycleOwner) {

        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        dataLogger = null
    }
}
