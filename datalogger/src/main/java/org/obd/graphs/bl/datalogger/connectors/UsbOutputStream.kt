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
package org.obd.graphs.bl.datalogger.connectors

import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import java.io.OutputStream

private const val LOGGER_TAG = "USB_CONNECTION"
class UsbOutputStream(val port: UsbSerialPort) : OutputStream() {
    override fun write(p0: Int) {
    }

    override fun write(b: ByteArray) {
        try {
            port.write(b, 2 * IO_TIMEOUT)
        } catch (e: IOException) {
            Log.e(LOGGER_TAG, "Failed to write command ${String(b)}", e)
        }
    }
}