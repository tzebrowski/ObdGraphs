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
package org.obd.graphs.profile

import android.os.Environment
import android.util.Log
import org.obd.graphs.getContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

private const val BACKUP_FILE_NAME = "obd_graphs.backup"
private const val LOG_TAG = "BackupManager"

internal class BackupManager(
    private val repository: ProfileRepository,
) {
    fun export(): File? =
        try {
            val file = getBackupFile()
            val props = Properties()

            repository.getAll().forEach { (key, value) ->
                props[key] =
                    when (value) {
                        is String -> "\"$value\""
                        else -> value.toString()
                    }
            }

            FileOutputStream(file).use { props.store(it, "Backup") }
            Log.i(LOG_TAG, "Backup exported to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Export failed", e)
            null
        }

    fun restore(file: File = getBackupFile()): Properties {
        val props = Properties()
        if (file.exists()) {
            FileInputStream(file).use { props.load(it) }
        }
        return props
    }

    private fun getBackupFile(): File = File(getContext()!!.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), BACKUP_FILE_NAME)
}
