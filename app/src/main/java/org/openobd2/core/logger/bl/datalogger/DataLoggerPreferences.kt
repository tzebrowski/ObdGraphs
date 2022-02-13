package org.openobd2.core.logger.bl.datalogger

import android.content.SharedPreferences
import android.util.Log
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getString
import org.openobd2.core.logger.ui.preferences.getStringSet
import org.openobd2.core.logger.ui.preferences.isEnabled

const val GENERIC_MODE = "Generic mode"


data class DataLoggerPreferences(
    var mode01Pids: MutableSet<Long>,
    var mode02Pids: MutableSet<Long>,
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
    var adaptiveConnectionEnabled: Boolean){

    fun isGenericModeSelected() : Boolean {
        return mode == GENERIC_MODE
    }

    companion object  {
        private lateinit var strongReference: SharedPreferenceChangeListener
        val instance: DataLoggerPreferences by lazy {
            val dataLoggerPreferences  = getDataLoggerPreferences()
            strongReference = SharedPreferenceChangeListener(dataLoggerPreferences)
            Prefs.registerOnSharedPreferenceChangeListener(strongReference)
            dataLoggerPreferences
        }
    }
}

private val LOGGER_KEY = "PREFS"

private class SharedPreferenceChangeListener(val dataLoggerPreferences: DataLoggerPreferences) : SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(LOGGER_KEY,"Key to update ${key}")

        when (key) {
            "pref.pids.generic.low" -> dataLoggerPreferences.mode01Pids =
                genericPids()
            "pref.pids.generic.high" -> dataLoggerPreferences.mode01Pids =
                genericPids()

            "pref.pids.mode22" -> dataLoggerPreferences.mode01Pids =
                Prefs.getStringSet(key).map { s -> s.toLong() }.toMutableSet()
            "pref.mode" -> dataLoggerPreferences.mode =
                Prefs.getString(key, GENERIC_MODE)!!
            "pref.debug.generator.enabled" -> dataLoggerPreferences.generatorEnabled =
                Prefs.isEnabled(key)
            "pref.adapter.adaptive.enabled" -> dataLoggerPreferences.adaptiveConnectionEnabled =
                Prefs.isEnabled(key)
            "selected.connection.type" ->  dataLoggerPreferences.connectionType =
                Prefs.getString(key, "wifi")!!
            "pref.adapter.connection.tcp.host" -> dataLoggerPreferences.tcpHost =
                Prefs.getString(key)!!
            "pref.adapter.connection.tcp.port" -> dataLoggerPreferences.tcpPort =
                Prefs.getString(key)!!.toInt()
            "pref.adapter.batch.enabled" -> dataLoggerPreferences.batchEnabled =
                Prefs.getBoolean(key, true)
            "pref.adapter.reconnect" -> dataLoggerPreferences.reconnectWhenError =
                Prefs.getBoolean(key, true)
            "pref.adapter.id" -> dataLoggerPreferences.adapterId =
                Prefs.getString(key, "OBDII")!!
            "pref.adapter.command.freq" -> dataLoggerPreferences.commandFrequency =
                Prefs.getString(key, "6").toString().toLong()
            "pref.adapter.init_delay" -> dataLoggerPreferences.initDelay =
                Prefs.getString(key, "500").toString().toLong()
        }
        Log.i(LOGGER_KEY,"Update data logger preferences ${dataLoggerPreferences}")
    }
}

private fun getDataLoggerPreferences(): DataLoggerPreferences {

    val connectionType = Prefs.getString("selected.connection.type","wifi")!!
    val tcpHost = Prefs.getString("pref.adapter.connection.tcp.host","192.168.0.10")!!
    val tcpPort = Prefs.getString("pref.adapter.connection.tcp.port","35000")!!.toInt()
    val batchEnabled =  Prefs.getBoolean("pref.adapter.batch.enabled", true)
    val reconnectWhenError = Prefs.getBoolean("pref.adapter.reconnect", true)
    val adapterId = Prefs.getString("pref.adapter.id", "OBDII")!!
    val commandFrequency =  Prefs.getString("pref.adapter.command.freq", "6").toString().toLong()
    val initDelay = Prefs.getString("pref.adapter.init_delay", "500").toString().toLong()

    val mode = Prefs.getString("pref.mode", GENERIC_MODE)!!
    val generatorEnabled = Prefs.isEnabled("pref.debug.generator.enabled")
    val adaptiveConnectionEnabled = Prefs.isEnabled("pref.adapter.adaptive.enabled")


    val mode02Pids = Prefs.getStringSet("pref.pids.mode22").map { s -> s.toLong() }.toMutableSet()

    val dataLoggerPreferences =  DataLoggerPreferences(
        genericPids(),
        mode02Pids,
        connectionType,
        tcpHost,
        tcpPort,
        batchEnabled,
        reconnectWhenError,
        adapterId,
        commandFrequency,
        initDelay,
        mode,
        generatorEnabled,
        adaptiveConnectionEnabled)

    Log.i(LOGGER_KEY,"Loaded data logger preferences: $dataLoggerPreferences")
    return dataLoggerPreferences
}

private fun genericPids(): MutableSet<Long> {
    val high = Prefs.getStringSet("pref.pids.generic.high").map { s -> s.toLong() }
    val low = Prefs.getStringSet("pref.pids.generic.low").map { s -> s.toLong() }
    return  (high + low).toMutableSet()
}