package org.openobd2.core.logger

import android.content.Context
import androidx.preference.PreferenceManager
import org.obd.metrics.Metric
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.bl.DataLoggerService
import org.openobd2.core.logger.ui.preferences.GENERIC_MODE
import org.openobd2.core.logger.ui.preferences.Prefs
import org.obd.metrics.pid.PidRegistry


class SelectedPids {
    companion object {
        @JvmStatic
        fun get(context: Context, prefKey: String): Pair<MutableSet<String>, MutableList<Metric<*>>> {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            var selectedPids = pref.getStringSet(prefKey, emptySet())
            var pidRegistry: PidRegistry

            when (Prefs.getMode(context)) {
                GENERIC_MODE -> {
                    pidRegistry = DataLoggerService.dataLogger.mode1.registry
                }
                else -> {
                    pidRegistry = DataLoggerService.dataLogger.mode22.registry
                }
            }

            var data: MutableList<Metric<*>> = arrayListOf()

            selectedPids!!.forEach { s: String? ->
                pidRegistry.findBy(s)?.apply {
                    data.add(Metric<Int>(ObdCommand(this), 0, ""))
                }
            }

            return Pair(selectedPids, data)
        }
    }
}