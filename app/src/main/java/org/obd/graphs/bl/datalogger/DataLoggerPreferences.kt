package org.obd.graphs.bl.datalogger

import android.content.SharedPreferences
import android.util.Log
import org.obd.graphs.ui.preferences.PREFERENCE_CONNECTION_TYPE
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getStringSet
import org.obd.graphs.ui.preferences.isEnabled

const val GENERIC_MODE = "Generic mode"

data class DataLoggerPreferences(
    var pids: MutableSet<Long>,
    var connectionType: String,
    var tcpHost: String,
    var tcpPort: Int,
    var batchEnabled: Boolean,
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
    var fetchSupportedPids: Boolean
) {
    companion object {
        private lateinit var strongReference: SharedPreferenceChangeListener
        val instance: DataLoggerPreferences by lazy {
            val dataLoggerPreferences = getDataLoggerPreferences()
            strongReference = SharedPreferenceChangeListener(dataLoggerPreferences)
            Prefs.registerOnSharedPreferenceChangeListener(strongReference)
            dataLoggerPreferences
        }
    }
}

const val LOGGER_KEY = "PREFS"

private class SharedPreferenceChangeListener(val dataLoggerPreferences: DataLoggerPreferences) :
    SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(LOGGER_KEY, "Key to update $key")

        when (key) {
            "pref.pids.generic.low" -> dataLoggerPreferences.pids =
                getPidsToQuery()
            "pref.pids.generic.high" -> dataLoggerPreferences.pids =
                getPidsToQuery()
            "pref.mode" -> dataLoggerPreferences.mode =
                Prefs.getString(key, GENERIC_MODE)!!
            "pref.debug.generator.enabled" -> dataLoggerPreferences.generatorEnabled =
                Prefs.isEnabled(key)
            "pref.adapter.adaptive.enabled" -> dataLoggerPreferences.adaptiveConnectionEnabled =
                Prefs.isEnabled(key)
            PREFERENCE_CONNECTION_TYPE -> dataLoggerPreferences.connectionType =
                Prefs.getString(key, "wifi")!!
            "pref.adapter.connection.tcp.host" -> dataLoggerPreferences.tcpHost =
                Prefs.getString(key,"192.168.0.10")!!
            "pref.adapter.connection.tcp.port" -> dataLoggerPreferences.tcpPort =
                Prefs.getString(key,"35000")!!.toInt()
            "pref.adapter.batch.enabled" -> dataLoggerPreferences.batchEnabled =
                Prefs.getBoolean(key, true)

            "pref.adapter.cache.result.enabled" -> dataLoggerPreferences.resultsCacheEnabled =
                Prefs.getBoolean(key, true)

            "pref.adapter.reconnect.hard_reset" -> dataLoggerPreferences.hardReset =
                Prefs.getBoolean(key, false)
            "pref.adapter.reconnect" -> dataLoggerPreferences.reconnectWhenError =
                Prefs.getBoolean(key, true)

            "pref.adapter.reconnect.max_retry" -> dataLoggerPreferences.maxReconnectRetry =
                Prefs.getString(key, "0")!!.toInt()

            "pref.adapter.id" -> dataLoggerPreferences.adapterId =
                Prefs.getString(key, "OBDII")!!
            "pref.adapter.command.freq" -> dataLoggerPreferences.commandFrequency =
                Prefs.getString(key, "6").toString().toLong()

            "pref.adapter.init.delay" -> dataLoggerPreferences.initDelay =
                Prefs.getString(key, "500").toString().toLong()

            "pref.adapter.init.protocol" -> dataLoggerPreferences.initProtocol =
                Prefs.getString(key, "AUTO").toString()

            "pref.adapter.init.fetchDeviceProperties" -> dataLoggerPreferences.fetchDeviceProperties =
                Prefs.getBoolean(key, true)

            "pref.adapter.init.fetchSupportedPids" -> dataLoggerPreferences.fetchSupportedPids =
                Prefs.getBoolean(key, true)

            "pref.pids.registry.list" -> dataLoggerPreferences.resources =
                resources()
        }
        Log.d(LOGGER_KEY, "Update data logger preferences $dataLoggerPreferences")
    }
}

private fun getDataLoggerPreferences(): DataLoggerPreferences {
    val connectionType = Prefs.getString(PREFERENCE_CONNECTION_TYPE, "bluetooth")!!
    val tcpHost = Prefs.getString("pref.adapter.connection.tcp.host", "192.168.0.10")!!
    val tcpPort = Prefs.getString("pref.adapter.connection.tcp.port", "35000")!!.toInt()
    val batchEnabled = Prefs.getBoolean("pref.adapter.batch.enabled", true)
    val reconnectWhenError = Prefs.getBoolean("pref.adapter.reconnect", true)
    val adapterId = Prefs.getString("pref.adapter.id", "OBDII")!!
    val commandFrequency = Prefs.getString("pref.adapter.command.freq", "6").toString().toLong()
    val initDelay = Prefs.getString("pref.adapter.init.delay", "500").toString().toLong()

    val mode = Prefs.getString("pref.mode", GENERIC_MODE)!!
    val generatorEnabled = Prefs.isEnabled("pref.debug.generator.enabled")
    val adaptiveConnectionEnabled = Prefs.isEnabled("pref.adapter.adaptive.enabled")
    val resultsCacheEnabled = Prefs.isEnabled("pref.adapter.cache.result.enabled")

    val initProtocol = Prefs.getString("pref.adapter.init.protocol", "AUTO")!!

    val hardReset =
        Prefs.getBoolean("pref.adapter.reconnect.hard_reset", false)

    val maxReconnectRetry =
        Prefs.getString("pref.adapter.reconnect.max_retry", "0")!!.toInt()

    val fetchDeviceProperties = Prefs.getBoolean("pref.adapter.init.fetchDeviceProperties", true)

    val fetchSupportedPids = Prefs.getBoolean("pref.adapter.init.fetchSupportedPids" , true)

    val dataLoggerPreferences = DataLoggerPreferences(
        pids = getPidsToQuery(),
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
        fetchSupportedPids = fetchSupportedPids
    )

    Log.i(LOGGER_KEY, "Loaded data logger preferences: $dataLoggerPreferences")
    return dataLoggerPreferences
}

private fun resources(): MutableSet<String> =
    Prefs.getStringSet("pref.pids.registry.list", defaultPidFiles.keys)!!

fun getPidsToQuery() =
    (Prefs.getStringSet("pref.pids.generic.high").map { s -> s.toLong() }
            + Prefs.getStringSet("pref.pids.generic.low").map { s -> s.toLong() }).toMutableSet()
