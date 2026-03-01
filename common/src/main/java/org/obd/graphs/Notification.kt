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
package org.obd.graphs

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import org.obd.graphs.commons.R

const val NOTIFICATION_CHANNEL_ID = "data_logger_channel_v2"
const val NOTIFICATION_ID = 12345

private const val NOTIFICATION_TITLE = "Vehicle Telemetry Service"

object Notification {
    fun sendBasicNotification(
        context: Context,
        contentText: String,
        pendingIntent: PendingIntent? = null,
    ) {
        try {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val channelId = NOTIFICATION_CHANNEL_ID

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelName = NOTIFICATION_TITLE
                val descriptionText = contentText
                val importance = NotificationManager.IMPORTANCE_DEFAULT

                val channel =
                    NotificationChannel(channelId, channelName, importance).apply {
                        description = descriptionText
                    }

                notificationManager.createNotificationChannel(channel)
            }

            val notification = notification(context, contentText, pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            } else {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e("Notification", " Failed to send notification", e)
        }
    }

    fun notification(
        context: Context,
        contentText: String,
        pendingIntent: PendingIntent? = null,
    ): Notification =
        NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_mygiulia_logo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                pendingIntent?.let { setContentIntent(it) }
            }.setOngoing(true)
            .setAutoCancel(true)
            .build()
}
