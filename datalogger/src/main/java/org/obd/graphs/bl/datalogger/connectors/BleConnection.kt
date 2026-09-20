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
package org.obd.graphs.bl.datalogger.connectors

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import org.obd.graphs.Network
import org.obd.metrics.transport.AdapterConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val LOGGER_TAG = "BleConnection"

/** Client Characteristic Configuration descriptor - notifications never arrive without it. */
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/**
 * GATT connect is unreliable on a first attempt even against an already-paired adapter, and the
 * stack needs a moment to tear down the failed attempt before it will accept another - hence an
 * escalating backoff rather than a fixed one.
 */
private val GATT_RETRY_BACKOFF_MS = longArrayOf(0, 500, 1200)

private const val OPERATION_TIMEOUT_MS = 10_000L
private const val WRITE_TIMEOUT_MS = 2000L
private const val REQUESTED_MTU = 517

/**
 * Bluetooth LE (GATT) transport, for adapters that expose no SPP record and so cannot be reached
 * by [BluetoothClassicConnection].
 *
 * The GATT profile is resolved by PROBING [profiles] after the services have been discovered, not
 * by filtering the device on an advertised service: OBD adapters advertise a local name and expose
 * their services only once connected, so filtering on the service UUID matches nothing.
 */
internal class BleConnection(
    private val context: Context,
    private val deviceAddress: String,
    private val profiles: List<BleProfile>
) : AdapterConnection {

    private var gatt: BluetoothGatt? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var input: BleInputStream? = null
    private var output: BleOutputStream? = null

    @Volatile
    private var mtu = BLE_DEFAULT_CHUNK_SIZE + 3

    private val closed = AtomicBoolean(false)

    private var connectionLatch: CountDownLatch? = null
    private var servicesLatch: CountDownLatch? = null
    private var mtuLatch: CountDownLatch? = null
    private var writeLatch: CountDownLatch? = null
    private var descriptorLatch: CountDownLatch? = null

    @Volatile
    private var writeStatus = BluetoothGatt.GATT_SUCCESS

    init {
        Log.i(LOGGER_TAG, "Created instance of BleConnection for: $deviceAddress, ${profiles.size} candidate profile(s)")
    }

    private val callback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                Log.i(LOGGER_TAG, "Connection state changed, status=$status, newState=$newState")

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> connectionLatch?.countDown()

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        // Unblocks both a connect still waiting and a reader parked on the queue.
                        connectionLatch?.countDown()
                        servicesLatch?.countDown()
                        mtuLatch?.countDown()
                        writeLatch?.countDown()
                        descriptorLatch?.countDown()
                        input?.close()
                    }
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {
                Log.i(LOGGER_TAG, "Services discovered, status=$status")
                servicesLatch?.countDown()
            }

            override fun onMtuChanged(
                gatt: BluetoothGatt,
                mtu: Int,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    this@BleConnection.mtu = mtu
                    Log.i(LOGGER_TAG, "Negotiated MTU: $mtu")
                }
                mtuLatch?.countDown()
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                Log.i(LOGGER_TAG, "Descriptor ${descriptor.uuid} written, status=$status")
                descriptorLatch?.countDown()
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                writeStatus = status
                writeLatch?.countDown()
            }

            // API 33+ delivers the payload as a parameter; below that it lives on the characteristic.
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                input?.onBytesReceived(value)
            }

            @Deprecated("Deprecated in API 33, still the callback that fires below it")
            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    characteristic.value?.let { input?.onBytesReceived(it) }
                }
            }
        }

    @Throws(IOException::class)
    override fun connect() {
        closed.set(false)

        val device =
            resolveBluetoothDevice(deviceAddress, requireBonded = false)
                ?: throw IOException("Did not resolve BLE device: $deviceAddress")

        if (profiles.isEmpty()) {
            throw IOException("No BLE profile to probe. Check the custom service/characteristic UUIDs.")
        }

        try {
            val server = connectWithRetry(device)

            if (!awaitServices(server)) {
                throw IOException("Failed to discover GATT services of $deviceAddress")
            }

            negotiateMtu(server)

            if (!resolveProfile(server)) {
                throw IOException(describeProfileMismatch(server))
            }

            enableNotifications(server)

            input = BleInputStream()
            output = BleOutputStream(chunkSize = { mtu - 3 }, writeChunk = ::writeChunk)

            gatt = server
            Log.i(LOGGER_TAG, "Successfully established BLE connection to: $deviceAddress")
        } catch (e: SecurityException) {
            Log.e(LOGGER_TAG, "Failed to obtain BT Permissions", e)
            Network.requestBluetoothPermissions()
            throw IOException("Missing Bluetooth permissions", e)
        }
    }

    override fun openInputStream(): InputStream? = input

    override fun openOutputStream(): OutputStream? = output

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }

        input.closeQuietly()
        output.closeQuietly()

        try {
            gatt?.let { server ->
                notifyCharacteristic?.let { server.setCharacteristicNotification(it, false) }
                server.disconnect()
                // Must be closed as well - disconnect() alone leaks the client interface, and the
                // stack has a hard cap on how many may be open at once.
                server.close()
            }
        } catch (e: SecurityException) {
            Log.e(LOGGER_TAG, "Failed to close GATT connection", e)
        }

        gatt = null
        notifyCharacteristic = null
        writeCharacteristic = null
        input = null
        output = null

        Log.i(LOGGER_TAG, "BLE connection to the device: $deviceAddress is closed.")
    }

    @Throws(IOException::class)
    override fun reconnect() {
        Log.i(LOGGER_TAG, "Reconnecting to the device: $deviceAddress")
        close()
        TimeUnit.MILLISECONDS.sleep(1000)
        connect()
        Log.i(LOGGER_TAG, "Successfully reconnect to the device: $deviceAddress")
    }

    @Throws(IOException::class)
    private fun connectWithRetry(device: BluetoothDevice): BluetoothGatt {
        GATT_RETRY_BACKOFF_MS.forEachIndexed { attempt, backoff ->
            if (backoff > 0) {
                TimeUnit.MILLISECONDS.sleep(backoff)
            }

            Log.i(LOGGER_TAG, "Opening GATT connection to $deviceAddress, attempt ${attempt + 1}")

            val latch = CountDownLatch(1)
            connectionLatch = latch

            val server =
                device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)

            if (server != null &&
                latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS) &&
                isConnected(server)
            ) {
                return server
            }

            Log.w(LOGGER_TAG, "GATT connection attempt ${attempt + 1} failed")
            server?.close()
        }

        throw IOException("Could not open a BLE connection to $deviceAddress after ${GATT_RETRY_BACKOFF_MS.size} attempts")
    }

    private fun isConnected(server: BluetoothGatt): Boolean =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)
            ?.getConnectionState(server.device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED

    private fun awaitServices(server: BluetoothGatt): Boolean {
        val latch = CountDownLatch(1)
        servicesLatch = latch

        return if (!server.discoverServices()) {
            false
        } else {
            latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS) && server.services.isNotEmpty()
        }
    }

    /**
     * A larger MTU means fewer chunks per command. Best effort: a refusal just leaves the default,
     * which every adapter supports.
     */
    private fun negotiateMtu(server: BluetoothGatt) {
        val latch = CountDownLatch(1)
        mtuLatch = latch

        if (server.requestMtu(REQUESTED_MTU)) {
            latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } else {
            Log.w(LOGGER_TAG, "MTU request was rejected, keeping the default of $mtu")
        }
    }

    private fun resolveProfile(server: BluetoothGatt): Boolean {
        profiles.forEach { profile ->
            val service = server.getService(profile.service)
            val notify = service?.getCharacteristic(profile.notifyCharacteristic)
            val write =
                if (profile.writeCharacteristic == profile.notifyCharacteristic) {
                    notify
                } else {
                    service?.getCharacteristic(profile.writeCharacteristic)
                }

            if (notify != null && write != null) {
                Log.i(LOGGER_TAG, "Matched BLE profile: ${profile.name}")
                notifyCharacteristic = notify
                writeCharacteristic = write
                return true
            }
        }
        return false
    }

    /** Says what the adapter DOES expose - that is what tells the user which UUIDs to enter. */
    private fun describeProfileMismatch(server: BluetoothGatt): String {
        val exposed = server.services.joinToString { it.uuid.toString() }
        return "None of the ${profiles.size} candidate BLE profiles fit $deviceAddress. " +
            "It exposes: ${exposed.ifEmpty { "no services" }}"
    }

    /**
     * Notifications do not arrive from setCharacteristicNotification() alone - the CCCD has to be
     * written too. The write is AWAITED: GATT permits one outstanding operation, so returning
     * while it is still in flight makes the stack reject the first command the connector sends.
     */
    private fun enableNotifications(server: BluetoothGatt) {
        val characteristic = notifyCharacteristic ?: return

        server.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.w(LOGGER_TAG, "Notify characteristic has no CCCD; notifications may not arrive")
            return
        }

        val latch = CountDownLatch(1)
        descriptorLatch = latch

        val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val submitted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                server.writeDescriptor(descriptor, value) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                server.writeDescriptor(descriptor)
            }

        if (submitted) {
            latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } else {
            Log.w(LOGGER_TAG, "GATT rejected the CCCD write; notifications may not arrive")
        }
    }

    private fun writeChunk(chunk: ByteArray): Boolean {
        val server = gatt ?: return false
        val characteristic = writeCharacteristic ?: return false

        return try {
            val writeType =
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }

            val latch = CountDownLatch(1)
            writeLatch = latch
            writeStatus = BluetoothGatt.GATT_SUCCESS

            val submitted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    server.writeCharacteristic(characteristic, chunk, writeType) == BluetoothGatt.GATT_SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.writeType = writeType
                    @Suppress("DEPRECATION")
                    characteristic.value = chunk
                    @Suppress("DEPRECATION")
                    server.writeCharacteristic(characteristic)
                }

            if (!submitted) {
                Log.e(LOGGER_TAG, "GATT rejected a write of ${chunk.size} bytes")
                false
            } else {
                latch.await(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                writeStatus == BluetoothGatt.GATT_SUCCESS
            }
        } catch (e: SecurityException) {
            Log.e(LOGGER_TAG, "Failed to obtain BT Permissions", e)
            Network.requestBluetoothPermissions()
            false
        }
    }
}
