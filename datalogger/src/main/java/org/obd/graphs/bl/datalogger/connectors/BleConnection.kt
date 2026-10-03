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
import org.obd.graphs.bl.datalogger.DATA_LOGGER_BLE_NOT_REACHABLE
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_CONNECT_EVENT
import org.obd.graphs.sendBroadcastEvent
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
 * How each attempt connects, in order. The backoff escalates because the stack needs a moment to
 * tear down a failed attempt before it will accept another.
 *
 * The transport matters as much as the retry. A dual-mode adapter paired over Classic does not
 * advertise, so a cold TRANSPORT_LE connect finds nothing and fails with status 147. What works is
 * attaching to a link the system ALREADY holds - verified against a CCY STN-2120 4.0, which
 * connected only once it had been connected from the system Bluetooth settings, and then answered
 * on the FFE0 (HM-10) profile. TRANSPORT_AUTO lets the stack use that existing link.
 */
private val GATT_ATTEMPTS =
    listOf(
        GattAttempt(backoffMs = 0, autoConnect = false, transport = BluetoothDevice.TRANSPORT_LE),
        GattAttempt(backoffMs = 1000, autoConnect = false, transport = BluetoothDevice.TRANSPORT_AUTO),
        GattAttempt(backoffMs = 2000, autoConnect = true, transport = BluetoothDevice.TRANSPORT_LE)
    )

private data class GattAttempt(
    val backoffMs: Long,
    val autoConnect: Boolean,
    val transport: Int
)

/**
 * A DIRECT connect (autoConnect = false) is the platform's own ~30s attempt, so anything shorter
 * gives up while the stack is still trying. A 10s budget here closed and reopened the client
 * three times without a single onConnectionStateChange callback ever arriving.
 */
private const val CONNECT_TIMEOUT_MS = 30_000L

/**
 * The last attempt uses autoConnect, which does not time out at all: it queues the connection and
 * completes whenever the device next becomes connectable. That is what actually works for an
 * adapter advertising at a slow interval, but it needs to be waited on for longer than a direct
 * attempt would take.
 */
private const val AUTO_CONNECT_TIMEOUT_MS = 45_000L

/**
 * How long a failed attempt is given to report its disconnect before the client is closed. The
 * client interface is only released once the stack has processed the disconnect, and closing it
 * early is the usual reason the NEXT connectGatt comes back with status 133.
 */
private const val DISCONNECT_TIMEOUT_MS = 600L

/**
 * Service discovery started in the same breath as the connection callback fails outright on
 * several stacks; they need the link to settle first.
 */
private const val DISCOVERY_SETTLE_MS = 600L

/**
 * Service discovery is flaky enough on its own (status 129 / an empty result on a link that is
 * demonstrably up) that a single shot is not a verdict.
 */
private const val DISCOVERY_ATTEMPTS = 3

/**
 * How long the adapter is given to volunteer its banner after being subscribed to, before anything
 * still queued is thrown away as unsolicited.
 */
private const val BANNER_SETTLE_MS = 400L

private const val OPERATION_TIMEOUT_MS = 10_000L
private const val WRITE_TIMEOUT_MS = 2000L
private const val REQUESTED_MTU = 517

/** Either kind of server-initiated update is usable; only the CCCD value written differs. */
private const val NOTIFY_PROPERTIES =
    BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE

private const val WRITE_PROPERTIES =
    BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE

private fun BluetoothGattCharacteristic.supports(properties: Int): Boolean = this.properties and properties != 0

/**
 * Bluetooth LE (GATT) transport, for adapters that expose no SPP record and so cannot be reached
 * by [BluetoothClassicConnection].
 *
 * The GATT profile is resolved by PROBING [profiles] after the services have been discovered, not
 * by filtering the device on an advertised service: OBD adapters advertise a local name and expose
 * their services only once connected, so filtering on the service UUID matches nothing.
 *
 * Everything the GATT callback touches is @Volatile. The callbacks arrive on a binder thread while
 * connect() runs on the caller's, and without that there is no happens-before edge between them -
 * a callback could see a stale latch (a connect that "times out" although the device connected) or
 * a null input stream (notifications silently dropped).
 */
internal class BleConnection(
    private val context: Context,
    private val deviceAddress: String,
    private val profiles: List<BleProfile>
) : AdapterConnection {

    @Volatile
    private var gatt: BluetoothGatt? = null

    @Volatile
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    @Volatile
    private var input: BleInputStream? = null

    @Volatile
    private var output: BleOutputStream? = null

    @Volatile
    private var mtu = BLE_DEFAULT_CHUNK_SIZE + 3

    private val closed = AtomicBoolean(false)

    /** The callback of the attempt in flight; older ones ignore everything they are handed. */
    @Volatile
    private var currentCallback: GattCallback? = null

    @Volatile
    private var servicesLatch: CountDownLatch? = null

    @Volatile
    private var mtuLatch: CountDownLatch? = null

    @Volatile
    private var writeLatch: CountDownLatch? = null

    @Volatile
    private var descriptorLatch: CountDownLatch? = null

    @Volatile
    private var writeStatus = BluetoothGatt.GATT_SUCCESS

    @Volatile
    private var servicesStatus = BluetoothGatt.GATT_SUCCESS

    init {
        Log.i(LOGGER_TAG, "Created instance of BleConnection for: $deviceAddress, ${profiles.size} candidate profile(s)")
    }

    /**
     * One per connection attempt. A single shared instance let a late event from a client we had
     * already given up on count down the latch of the attempt that replaced it.
     */
    private inner class GattCallback : BluetoothGattCallback() {

        /** Released by the first terminal state, whichever it is. */
        val connectionLatch = CountDownLatch(1)

        /** Released once the stack confirms the client may be closed. */
        val disconnectLatch = CountDownLatch(1)

        @Volatile
        var connected = false

        @Volatile
        var lastStatus = BluetoothGatt.GATT_SUCCESS

        private val active: Boolean
            get() = this === currentCallback

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            Log.i(
                LOGGER_TAG,
                "Connection state changed, status=$status (${describeGattStatus(status)}), newState=$newState"
            )

            lastStatus = status

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // Only GATT_SUCCESS is a connection. The stack also reports CONNECTED with a
                    // failure status, and taking that as success handed the caller a dead client.
                    connected = status == BluetoothGatt.GATT_SUCCESS
                    connectionLatch.countDown()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    connected = false
                    connectionLatch.countDown()
                    disconnectLatch.countDown()

                    if (active) {
                        // Unblocks anything parked on an operation, and fails the reader rather
                        // than letting a dropped link look like an ordinary quiet line.
                        servicesLatch?.countDown()
                        mtuLatch?.countDown()
                        writeLatch?.countDown()
                        descriptorLatch?.countDown()
                        input?.onLinkLost()
                    }
                }
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            Log.i(LOGGER_TAG, "Services discovered, status=$status (${describeGattStatus(status)})")
            if (active) {
                servicesStatus = status
                servicesLatch?.countDown()
            }
        }

        override fun onMtuChanged(
            gatt: BluetoothGatt,
            mtu: Int,
            status: Int
        ) {
            if (!active) {
                return
            }

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
            if (active) {
                descriptorLatch?.countDown()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (active) {
                writeStatus = status
                writeLatch?.countDown()
            }
        }

        // API 33+ delivers the payload as a parameter; below that it lives on the characteristic.
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (active) {
                input?.onBytesReceived(value)
            }
        }

        @Deprecated("Deprecated in API 33, still the callback that fires below it")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (active && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                characteristic.value?.let { input?.onBytesReceived(it) }
            }
        }
    }

    @Throws(IOException::class)
    override fun connect() {
        closed.set(false)

        // A client left open by a previous attempt keeps its slot in the stack's hard cap, and the
        // next connectGatt then fails with 133 until the process is restarted.
        gatt?.let { releaseAfterFailure(it) }

        var server: BluetoothGatt? = null
        var reported = false

        // Everything that can fail belongs inside: a throw from out here is swallowed by the
        // workflow and leaves the UI on "connecting" with no event ever sent.
        try {
            if (profiles.isEmpty()) {
                throw IOException("No BLE profile to probe. Check the custom service/characteristic UUIDs.")
            }

            server = connectWithRetry(resolveDevice()) { reported = true }

            // Raises the connection interval from the stack's lazy default to ~11-15ms. An ELM327
            // is strictly request/response, so every command otherwise waits out a full interval
            // in each direction - the difference between a usable poll rate and a stuttering one.
            requestHighPriority(server)

            TimeUnit.MILLISECONDS.sleep(DISCOVERY_SETTLE_MS)

            if (!awaitServices(server)) {
                throw IOException("Failed to discover GATT services of $deviceAddress")
            }

            logDiscoveredServices(server)
            negotiateMtu(server)

            if (!resolveProfile(server)) {
                throw IOException(describeProfileMismatch(server))
            }

            // The streams and `gatt` have to exist BEFORE notifications are enabled: a lot of
            // adapters push their banner the moment the CCCD is written, and a notification that
            // lands while `input` is still null is gone.
            input = BleInputStream()
            output = BleOutputStream(chunkSize = { mtu - 3 }, writeChunk = ::writeChunk)
            gatt = server

            enableNotifications(server)
            discardBanner()

            Log.i(LOGGER_TAG, "Successfully established BLE connection to: $deviceAddress")
        } catch (e: SecurityException) {
            releaseAfterFailure(server)
            Log.e(LOGGER_TAG, "Failed to obtain BT Permissions", e)
            Network.requestBluetoothPermissions()
            throw IOException("Missing Bluetooth permissions", e)
        } catch (e: Throwable) {
            releaseAfterFailure(server)
            Log.e(LOGGER_TAG, "Failed to establish BLE connection to $deviceAddress: ${e.message}", e)

            // The workflow swallows whatever connect() throws and then spins with a null connector,
            // so without an event of our own a failure here shows up as "connecting" forever.
            if (!reported) {
                sendBroadcastEvent(DATA_LOGGER_ERROR_CONNECT_EVENT)
            }
            throw e
        }
    }

    /**
     * A device the system already knows is best: it carries the address type it was really seen
     * with. Failing that the adapter is looked for on the air, because `getRemoteDevice(mac)`
     * always labels an address PUBLIC - connecting a random-address adapter that way brings up a
     * link that exposes nothing, or fails with 133/147 for no visible reason.
     *
     * A synthesised device remains the fallback rather than a hard failure: an adapter the phone
     * is ALREADY connected to does not advertise, so a scan finding nothing is not a verdict.
     */
    @Throws(IOException::class)
    private fun resolveDevice(): BluetoothDevice {
        Network.knownDeviceByAddress(deviceAddress)?.let { return it }

        Log.i(LOGGER_TAG, "$deviceAddress is neither bonded nor connected; scanning for it before connecting")
        Network.findAdvertisingBleDevice(deviceAddress)?.let { return it }

        Log.w(LOGGER_TAG, "$deviceAddress did not advertise; connecting blind on its MAC")

        return resolveBluetoothDevice(deviceAddress, requireBonded = false)
            ?: throw IOException("Did not resolve BLE device: $deviceAddress")
    }

    /** Best effort - a stack that refuses just keeps its default interval. */
    private fun requestHighPriority(server: BluetoothGatt) {
        try {
            val accepted = server.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            Log.i(LOGGER_TAG, "Requested a high connection priority: $accepted")
        } catch (e: Throwable) {
            Log.w(LOGGER_TAG, "Could not raise the connection priority", e)
        }
    }

    /** The raw numbers are meaningless in a bug report; these are the ones that actually occur. */
    private fun describeGattStatus(status: Int): String =
        when (status) {
            BluetoothGatt.GATT_SUCCESS -> "success"
            8 -> "link supervision timeout - the adapter went away mid-connection"
            19 -> "terminated by the adapter"
            22 -> "terminated by the phone"
            62 -> "failed to establish - the adapter did not accept the connection"
            133 -> "generic GATT error - usually a stale client, try again"
            147 -> "connection timeout - the adapter is not broadcasting"
            else -> "unmapped status"
        }

    /**
     * The stack caps how many client interfaces may be open at once, and a leaked one makes every
     * later attempt fail until the app is killed - so a half-built connection has to be released
     * here, where close() cannot reach it yet.
     */
    private fun releaseAfterFailure(server: BluetoothGatt?) {
        val callback = currentCallback
        currentCallback = null

        input.closeQuietly()
        input = null
        output = null
        gatt = null
        notifyCharacteristic = null
        writeCharacteristic = null

        releaseGatt(server, callback)
    }

    /**
     * close() must not follow disconnect() immediately: the client is only released once the stack
     * has reported the disconnect, and closing it early is what leaves the next connectGatt failing
     * with status 133.
     */
    private fun releaseGatt(
        server: BluetoothGatt?,
        callback: GattCallback?
    ) {
        if (server == null) {
            return
        }

        try {
            server.disconnect()

            if (callback == null || !callback.disconnectLatch.await(DISCONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                // An attempt that never reached the device gets no disconnect callback at all.
                TimeUnit.MILLISECONDS.sleep(DISCONNECT_TIMEOUT_MS)
            }

            server.close()
        } catch (e: Throwable) {
            Log.w(LOGGER_TAG, "Failed to release the GATT client", e)
        }
    }

    /**
     * Always logged, not only on a mismatch: the service and characteristic UUIDs an adapter
     * actually exposes are what tell you which profile it needs, or what to enter as a custom one.
     */
    private fun logDiscoveredServices(server: BluetoothGatt) {
        Log.i(LOGGER_TAG, "Discovered ${server.services.size} GATT service(s) on $deviceAddress")

        server.services.forEach { service ->
            val characteristics =
                service.characteristics.joinToString { "${it.uuid} (properties=${it.properties})" }
            Log.i(LOGGER_TAG, "  service ${service.uuid}: $characteristics")
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

        val callback = currentCallback
        currentCallback = null

        try {
            gatt?.let { server ->
                notifyCharacteristic?.let { server.setCharacteristicNotification(it, false) }
                // Must be closed as well - disconnect() alone leaks the client interface, and the
                // stack has a hard cap on how many may be open at once.
                releaseGatt(server, callback)
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
    private fun connectWithRetry(
        device: BluetoothDevice,
        onReported: () -> Unit
    ): BluetoothGatt {
        GATT_ATTEMPTS.forEachIndexed { index, attempt ->
            if (attempt.backoffMs > 0) {
                TimeUnit.MILLISECONDS.sleep(attempt.backoffMs)
            }

            val timeout = if (attempt.autoConnect) AUTO_CONNECT_TIMEOUT_MS else CONNECT_TIMEOUT_MS

            Log.i(
                LOGGER_TAG,
                "Opening GATT connection to $deviceAddress, attempt ${index + 1}, " +
                    "autoConnect=${attempt.autoConnect}, transport=${describeTransport(attempt.transport)}, timeout=${timeout}ms"
            )

            val callback = GattCallback()
            currentCallback = callback

            val server = device.connectGatt(context, attempt.autoConnect, callback, attempt.transport)

            // `connected` is the authority, set by the callback for THIS client. Querying the
            // stack instead (BluetoothManager.getConnectionState) reports the state of the device
            // across the whole system, so an already-connected dual-mode adapter made every failed
            // attempt look successful.
            if (server != null && callback.connectionLatch.await(timeout, TimeUnit.MILLISECONDS) && callback.connected) {
                return server
            }

            Log.w(
                LOGGER_TAG,
                "GATT connection attempt ${index + 1} failed, last status=${callback.lastStatus} " +
                    "(${describeGattStatus(callback.lastStatus)})"
            )

            currentCallback = null
            releaseGatt(server, callback)
        }

        // Every attempt heard nothing from the device. Reported as its own event so the user gets
        // the one thing that actually helps instead of the generic "check your network settings".
        sendBroadcastEvent(DATA_LOGGER_BLE_NOT_REACHABLE)
        onReported()

        throw IOException(
            "Could not open a BLE connection to $deviceAddress after ${GATT_ATTEMPTS.size} attempts. " +
                "A dual-mode adapter paired over Bluetooth Classic does not advertise: connect it " +
                "in the system Bluetooth settings first, then retry."
        )
    }

    private fun describeTransport(transport: Int): String =
        when (transport) {
            BluetoothDevice.TRANSPORT_LE -> "LE"
            BluetoothDevice.TRANSPORT_AUTO -> "AUTO"
            else -> "BR/EDR"
        }

    /**
     * Discovery is retried, and the stale service cache is dropped between attempts.
     *
     * A bonded dual-mode adapter is the awkward case: the platform keeps a per-device GATT service
     * cache, and for a device that was bonded over Classic that cache can be empty or from another
     * transport - discovery then "succeeds" and reports no services at all, over a link that is
     * demonstrably up. Clearing it forces a real rediscovery off the device.
     *
     * Each outcome is logged distinctly because they mean completely different things: refused to
     * start, never answered, or answered with nothing.
     */
    private fun awaitServices(server: BluetoothGatt): Boolean {
        repeat(DISCOVERY_ATTEMPTS) { attempt ->
            // Only on a RETRY. A first attempt over a healthy link discovers fine, and dropping a
            // valid database costs a round trip to the adapter for nothing; the stale-cache case
            // is exactly the one that comes back empty and gets here a second time. The settle is
            // not optional either - discovery started in the same breath as refresh() fails.
            if (attempt > 0) {
                refreshDeviceCache(server)
                TimeUnit.MILLISECONDS.sleep(DISCOVERY_SETTLE_MS)
            }

            val latch = CountDownLatch(1)
            servicesLatch = latch
            servicesStatus = BluetoothGatt.GATT_SUCCESS

            val label = "attempt ${attempt + 1}/$DISCOVERY_ATTEMPTS"

            when {
                !server.discoverServices() ->
                    Log.w(LOGGER_TAG, "GATT refused to start service discovery, $label")

                !latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS) ->
                    Log.w(LOGGER_TAG, "Service discovery did not answer within ${OPERATION_TIMEOUT_MS}ms, $label")

                server.services.isNullOrEmpty() ->
                    Log.w(
                        LOGGER_TAG,
                        "Service discovery finished with status=$servicesStatus " +
                            "(${describeGattStatus(servicesStatus)}) but the adapter exposes no services, $label"
                    )

                else -> return true
            }
        }

        return false
    }

    /**
     * BluetoothGatt.refresh() is not public API, so this is best effort - hidden-API restrictions
     * may well refuse it. It is the only way to drop the platform's cached service database, which
     * is what makes a bonded adapter report zero services forever.
     */
    private fun refreshDeviceCache(server: BluetoothGatt) {
        try {
            val refreshed = server.javaClass.getMethod("refresh").invoke(server) as? Boolean
            Log.i(LOGGER_TAG, "Dropped the cached GATT service database: $refreshed")
        } catch (e: Throwable) {
            Log.i(LOGGER_TAG, "Could not drop the cached GATT service database: ${e.message}")
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

    /**
     * A UUID match is not enough: the characteristics have to actually support what they are used
     * for. Cloned adapters reuse the well-known UUIDs on characteristics that are read-only, or
     * expose FFE0/FFE1 as a second, decorative service - matching those left a link that was up
     * and answered nothing.
     */
    private fun resolveProfile(server: BluetoothGatt): Boolean {
        profiles.forEach { profile ->
            val service = server.getService(profile.service) ?: return@forEach
            val notify = service.getCharacteristic(profile.notifyCharacteristic)
            val write =
                if (profile.writeCharacteristic == profile.notifyCharacteristic) {
                    notify
                } else {
                    service.getCharacteristic(profile.writeCharacteristic)
                }

            when {
                notify == null || write == null -> return@forEach

                !notify.supports(NOTIFY_PROPERTIES) || !write.supports(WRITE_PROPERTIES) ->
                    Log.w(
                        LOGGER_TAG,
                        "Profile ${profile.name} matches by UUID but not by properties: " +
                            "notify=${notify.properties}, write=${write.properties}. Skipping it."
                    )

                else -> {
                    Log.i(LOGGER_TAG, "Matched BLE profile: ${profile.name}")
                    notifyCharacteristic = notify
                    writeCharacteristic = write
                    return true
                }
            }
        }

        return discoverSerialProfile(server)
    }

    /**
     * Last resort: find a serial-bridge-shaped pair among whatever the adapter exposes.
     *
     * There is no standard OBD-over-BLE profile, so [BLE_PROFILES] can only ever list the modules
     * somebody has already seen. An unlisted one used to fail outright even though its GATT layout
     * is the same as all the others - one vendor service with a notifiable characteristic and a
     * writable one. Probing for that shape connects those adapters instead of refusing them, and
     * the UUIDs are logged so the pair can be promoted to a named profile.
     */
    private fun discoverSerialProfile(server: BluetoothGatt): Boolean {
        server.services
            .orEmpty()
            .filter { it.uuid !in BLE_GENERIC_SERVICES }
            .forEach { service ->
                val notify = service.characteristics.firstOrNull { it.supports(NOTIFY_PROPERTIES) } ?: return@forEach

                // A separate write characteristic is the common layout; a single read/write one
                // (HM-10 and friends) is the other.
                val write =
                    service.characteristics.firstOrNull { it !== notify && it.supports(WRITE_PROPERTIES) }
                        ?: notify.takeIf { it.supports(WRITE_PROPERTIES) }
                        ?: return@forEach

                Log.w(
                    LOGGER_TAG,
                    "No known BLE profile matched $deviceAddress. Falling back to the serial-like pair " +
                        "on service ${service.uuid}: notify=${notify.uuid}, write=${write.uuid}"
                )

                notifyCharacteristic = notify
                writeCharacteristic = write
                return true
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

        // Writing the NOTIFY value to a characteristic that only INDICATES subscribes to nothing:
        // the link comes up, the CCCD write reports success, and not one byte is ever delivered.
        val value =
            if (characteristic.supports(BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                Log.i(LOGGER_TAG, "${characteristic.uuid} indicates rather than notifies; subscribing accordingly")
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }

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

    /**
     * Throws away whatever the adapter volunteered on being subscribed to - typically its version
     * banner. No command has been sent yet, so those bytes are the answer to nothing; left in the
     * queue they are read as the reply to the first command and shift every response after it by
     * one, which looks exactly like an adapter that connects and then fails to initialise.
     */
    private fun discardBanner() {
        TimeUnit.MILLISECONDS.sleep(BANNER_SETTLE_MS)

        val dropped = input?.discardPending() ?: 0
        if (dropped > 0) {
            Log.i(LOGGER_TAG, "Discarded $dropped unsolicited byte(s) sent before the first command")
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

            when {
                !submitted -> {
                    Log.e(LOGGER_TAG, "GATT rejected a write of ${chunk.size} bytes")
                    false
                }

                // A write that never completed has to be reported as a failure. writeStatus is
                // pre-set to GATT_SUCCESS, so trusting it alone reported a timed-out write as sent.
                !latch.await(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS) -> {
                    Log.e(LOGGER_TAG, "A write of ${chunk.size} bytes did not complete within ${WRITE_TIMEOUT_MS}ms")
                    false
                }

                else -> writeStatus == BluetoothGatt.GATT_SUCCESS
            }
        } catch (e: SecurityException) {
            Log.e(LOGGER_TAG, "Failed to obtain BT Permissions", e)
            Network.requestBluetoothPermissions()
            false
        }
    }
}
