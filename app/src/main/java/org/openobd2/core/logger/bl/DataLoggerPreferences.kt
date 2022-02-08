package org.openobd2.core.logger.bl

import android.content.SharedPreferences
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getString
import org.openobd2.core.logger.ui.preferences.getStringSet
import org.openobd2.core.logger.ui.preferences.isEnabled

data class DataLoggerPreferences(
    val connectionType: String,
    val tcpHost: String,
    val tcpPort: Int,
    val batchEnabled: Boolean,
    val reconnectWhenError: Boolean,
    val adapterId: String,
    val commandFrequency: Long,
    val initDelay: Long,
    val mode: String,
    val generatorEnabled: Boolean,
    val adaptiveConnectionEnabled: Boolean,
    val mode01Pids: MutableSet<Long>,
    val mode02Pids: MutableSet<Long>)


fun getDataLoggerPreferences(): DataLoggerPreferences {

    val connectionType = Prefs.getString("selected.connection.type","wifi")!!
    val tcpHost = Prefs.getString("pref.adapter.connection.tcp.host")!!
    val tcpPort = Prefs.getString("pref.adapter.connection.tcp.port")!!.toInt()
    val batchEnabled =  Prefs.getBoolean("pref.adapter.batch.enabled", true)
    val reconnectWhenError = Prefs.getBoolean("pref.adapter.reconnect", true)
    val adapterId = Prefs.getString("pref.adapter.id", "OBDII")!!
    val commandFrequency =  Prefs.getString("pref.adapter.command.freq", "6").toString().toLong()
    val initDelay: Long = Prefs.getString("pref.adapter.init_delay", "500").toString().toLong()
    val mode = Prefs.getString("pref.mode", "Generic mode")!!

    val generatorEnabled = Prefs.isEnabled("pref.debug.generator.enabled")

    val adaptiveConnectionEnabled = Prefs.isEnabled("pref.adapter.adaptive.enabled")
    val mode01Pids = Prefs.getStringSet("pref.pids.generic").map { s -> s.toLong() }.toMutableSet()
    val mode02Pids = Prefs.getStringSet("pref.pids.mode22").map { s -> s.toLong() }.toMutableSet()

    return DataLoggerPreferences(connectionType,
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
}