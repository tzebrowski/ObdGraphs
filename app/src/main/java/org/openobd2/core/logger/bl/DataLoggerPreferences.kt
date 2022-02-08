package org.openobd2.core.logger.bl

import android.content.SharedPreferences
import android.util.Log
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getString
import org.openobd2.core.logger.ui.preferences.getStringSet
import org.openobd2.core.logger.ui.preferences.isEnabled

data class DataLoggerPreferences(
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
    var mode01Pids: MutableSet<Long>,
    var mode02Pids: MutableSet<Long>){

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

        when (key) {
            "pref.pids.generic" -> dataLoggerPreferences.mode01Pids =
                Prefs.getStringSet(key).map { s -> s.toLong() }.toMutableSet()
            "pref.pids.mode22" -> dataLoggerPreferences.mode01Pids =
                Prefs.getStringSet(key).map { s -> s.toLong() }.toMutableSet()
            "pref.mode" -> dataLoggerPreferences.mode =
                Prefs.getString(key, "Generic mode")!!
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
    val tcpHost = Prefs.getString("pref.adapter.connection.tcp.host")!!
    val tcpPort = Prefs.getString("pref.adapter.connection.tcp.port")!!.toInt()
    val batchEnabled =  Prefs.getBoolean("pref.adapter.batch.enabled", true)
    val reconnectWhenError = Prefs.getBoolean("pref.adapter.reconnect", true)
    val adapterId = Prefs.getString("pref.adapter.id", "OBDII")!!
    val commandFrequency =  Prefs.getString("pref.adapter.command.freq", "6").toString().toLong()
    val initDelay = Prefs.getString("pref.adapter.init_delay", "500").toString().toLong()

    val mode = Prefs.getString("pref.mode", "Generic mode")!!
    val generatorEnabled = Prefs.isEnabled("pref.debug.generator.enabled")
    val adaptiveConnectionEnabled = Prefs.isEnabled("pref.adapter.adaptive.enabled")
    val mode01Pids = Prefs.getStringSet("pref.pids.generic").map { s -> s.toLong() }.toMutableSet()
    val mode02Pids = Prefs.getStringSet("pref.pids.mode22").map { s -> s.toLong() }.toMutableSet()

    val dataLoggerPreferences =  DataLoggerPreferences(connectionType,
                    tcpHost,
                    tcpPort,
                    batchEnabled,
                    reconnectWhenError,
                    adapterId,
                    commandFrequency,
                    initDelay,
                    mode,
                    generatorEnabled,
                    adaptiveConnectionEnabled,
                    mode01Pids,
                    mode02Pids)

    Log.i(LOGGER_KEY,"Loaded data logger preferences: $dataLoggerPreferences")

    return dataLoggerPreferences
}