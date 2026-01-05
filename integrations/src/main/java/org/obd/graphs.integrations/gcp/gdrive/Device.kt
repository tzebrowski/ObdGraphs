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
package org.obd.graphs.integrations.gcp.gdrive

import android.content.Context
import org.obd.graphs.getContext
import java.security.SecureRandom

internal object Device {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val KEY_ID = "local_id"

    internal fun id(): String {
        val prefs = getContext()!!.getSharedPreferences("assets", Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_ID, null)

        if (id == null) {
            id = generateNanoId()
            prefs.edit().putString(KEY_ID, id).apply()
        }
        return id
    }

    private fun generateNanoId(length: Int = 12): String {
        val random = SecureRandom()
        val id = StringBuilder()

        for (i in 0 until length) {
            id.append(ALPHABET[random.nextInt(ALPHABET.length)])
        }
        return id.toString()
    }
}
