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
package org.obd.graphs.bl.datalogger

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

object DataLoggerConnector {
    private var service: DataLoggerService? = null
    private var isBound = false
    private val pendingTasks = mutableListOf<DataLoggerService.() -> Unit>()

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                className: ComponentName,
                binder: IBinder,
            ) {
                Log.i("DataLoggerConnector", "Service connected globally.")
                val localBinder = binder as DataLoggerService.LocalBinder
                service = localBinder.getService()
                isBound = true

                synchronized(pendingTasks) {
                    pendingTasks.forEach { task -> service?.task() }
                    pendingTasks.clear()
                }
            }

            override fun onServiceDisconnected(className: ComponentName) {
                service = null
                isBound = false
            }
        }

    /**
     * The core function. Binds lazily (on first use) and executes the action.
     */
    fun run(
        context: Context,
        action: DataLoggerService.() -> Unit,
    ) {
        val currentService = service

        if (currentService != null) {
            currentService.action()
        } else {
            Log.i("DataLoggerConnector", "Service not ready. Queueing task...")
            synchronized(pendingTasks) {
                pendingTasks.add(action)
            }
            bind(context)
        }
    }

    private fun bind(context: Context) {
        if (!isBound) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, DataLoggerService::class.java)
            appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            isBound = true
        }
    }

    fun release(context: Context) {
        if (isBound) {
            context.applicationContext.unbindService(serviceConnection)
            isBound = false
            service = null
        }
    }
}
