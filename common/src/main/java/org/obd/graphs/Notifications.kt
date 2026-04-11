/*
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

private const val LOG_TAG = "Notification"

object Notifications {

    fun show(
        context: Context,
        notificationId: Int,
        channelId: String,
        channelName: String,
        channelDescription: String,
        title: String,
        text: String,
        pendingIntent: PendingIntent? = null,
        isOngoing: Boolean = false,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(LOG_TAG, "Permission POST_NOTIFICATIONS is missing. Cannot show: $title")
                    return
                }
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = channelDescription
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_mygiulia_logo)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(isOngoing)
                .setAutoCancel(!isOngoing)

            pendingIntent?.let { builder.setContentIntent(it) }

            notificationManager.notify(notificationId, builder.build())
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to send notification: $title", e)
        }
    }

    fun buildForegroundServiceNotification(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        title: String,
        text: String,
        pendingIntent: PendingIntent? = null,
        importance: Int = NotificationManager.IMPORTANCE_LOW
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mygiulia_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        pendingIntent?.let { builder.setContentIntent(it) }

        return builder.build()
    }
}
