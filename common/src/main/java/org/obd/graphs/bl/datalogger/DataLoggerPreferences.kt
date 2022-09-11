package org.obd.graphs.bl.datalogger

import android.content.SharedPreferences
import android.util.Log
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.isEnabled


const val GENERIC_MODE = "Generic mode"
const val PREFERENCE_CONNECTION_TYPE = "pref.adapter.connection.type"
private const val LOGGER_KEY = "PREFS"

data class DataLoggerPreferences(
    var pids: MutableSet<Long>,
    var connectionType: String,
    var tcpHost: String,
    var tcpPort: Int,
    var batchEnabled: Boolean,
    var reconnectSilent: Boolean,
    var reconnectWhenError: Boolean,
    var adapterId: String,
    var commandFrequency: Long,
    var initDelay: Long,
    var mode: String,
    var generatorEnabled: Boolean,
    var adaptiveConnectionEnabled: Boolean,
    var resultsCacheEnabled: Boolean,
    var initProtocol: String,
    var hardReset: Boolean,
    var maxReconnectRetry: Int,
    var resources: Set<String>,
    var fetchDeviceProperties: Boolean,
    var fetchSupportedPids: Boolean,
    var responseLengthEnabled: Boolean,
    var gracefulStop: Boolean
    )

class DataLoggerPreferencesManager {

    private inner class SharedPreferenceChangeListener :
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            Log.d(LOGGER_KEY, "Key to update $key")
            instance = loadPreferences()
            Log.d(LOGGER_KEY, "Update data logger preferences $dataLoggerPreferences")
        }
    }

    private var strongReference: SharedPreferenceChangeListener = SharedPreferenceChangeListener()
    var instance: DataLoggerPreferences

    init {
        Prefs.registerOnSharedPreferenceChangeListener(strongReference)
        instance =  loadPreferences()
    }

    private fun loadPreferences(): DataLoggerPreferences {
        val connectionType = Prefs.getS(PREFERENCE_CONNECTION_TYPE, "bluetooth")
        val tcpHost = Prefs.getS("pref.adapter.connection.tcp.host", "192.168.0.10")
        val tcpPort = Prefs.getS("pref.adapter.connection.tcp.port", "35000").toInt()
        val batchEnabled = Prefs.getBoolean("pref.adapter.batch.enabled", true)
        val reconnectWhenError = Prefs.getBoolean("pref.adapter.reconnect", true)
        val adapterId = Prefs.getS("pref.adapter.id", "OBDII")
        val commandFrequency = Prefs.getS("pref.adapter.command.freq", "6").toLong()
        val initDelay = Prefs.getS("pref.adapter.init.delay", "500").toLong()

        val mode = Prefs.getS("pref.mode", GENERIC_MODE)
        val generatorEnabled = Prefs.isEnabled("pref.debug.generator.enabled")
        val adaptiveConnectionEnabled = Prefs.isEnabled("pref.adapter.adaptive.enabled")
        val resultsCacheEnabled = Prefs.isEnabled("pref.adapter.cache.result.enabled")

        val initProtocol = Prefs.getS("pref.adapter.init.protocol", "AUTO")

        val hardReset =
            Prefs.getBoolean("pref.adapter.reconnect.hard_reset", false)

        val maxReconnectRetry =
            Prefs.getS("pref.adapter.reconnect.max_retry", "0").toInt()

        val fetchDeviceProperties =
            Prefs.getBoolean("pref.adapter.init.fetchDeviceProperties", true)

        val fetchSupportedPids = Prefs.getBoolean("pref.adapter.init.fetchSupportedPids", true)

        val responseLength = Prefs.getBoolean("pref.adapter.responseLength.enabled", false)

        val gracefulStop = Prefs.getBoolean("pref.adapter.graceful_stop.enabled", true)

        val reconnectSilent = Prefs.getBoolean("pref.adapter.reconnect.silent", true)

        val dataLoggerPreferences = DataLoggerPreferences(
            pids = getPIDsToQuery(),
            connectionType = connectionType,
            tcpHost = tcpHost,
            tcpPort = tcpPort,
            batchEnabled = batchEnabled,
            reconnectWhenError = reconnectWhenError,
            adapterId = adapterId,
            commandFrequency = commandFrequency,
            initDelay = initDelay,
            mode = mode,
            generatorEnabled = generatorEnabled,
            adaptiveConnectionEnabled = adaptiveConnectionEnabled,
            resultsCacheEnabled = resultsCacheEnabled,
            initProtocol = initProtocol,
            hardReset = hardReset,
            maxReconnectRetry = maxReconnectRetry,
            resources = resources(),
            fetchDeviceProperties = fetchDeviceProperties,
            fetchSupportedPids = fetchSupportedPids,
            responseLengthEnabled = responseLength,
            gracefulStop = gracefulStop,
            reconnectSilent = reconnectSilent
        )

        Log.i(LOGGER_KEY, "Loaded data logger preferences: $dataLoggerPreferences")
        return dataLoggerPreferences
    }

    fun getPIDsToQuery() =
        (fastPIDs()
                + slowPIDs()).toMutableSet()

    private fun fastPIDs() = Prefs.getStringSet("pref.pids.generic.high").map { s -> s.toLong() }
    private fun slowPIDs() = Prefs.getStringSet("pref.pids.generic.low").map { s -> s.toLong() }

    private fun resources(): MutableSet<String> =
        Prefs.getStringSet("pref.pids.registry.list", defaultPidFiles.keys)!!
}

val dataLoggerPreferences  by lazy {  DataLoggerPreferencesManager() }