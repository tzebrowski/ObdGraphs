package org.openobd2.core.logger

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import org.obd.metrics.Metric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidRegistry
import org.openobd2.core.logger.bl.DataLoggerService

class SelectedPids {
    companion object {
        @JvmStatic
        fun get(
            context: Context,
            prefKey: String
        ): Pair<MutableSet<String>, MutableList<Metric<*>>> {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            var selectedPids = pref.getStringSet(prefKey, emptySet())
            Log.d("S_PID", "$prefKey   ->  $selectedPids")

            var pidRegistry: PidRegistry =
                DataLoggerService.dataLogger.pids()
            var data: MutableList<Metric<*>> = arrayListOf()
            selectedPids!!.forEach { s: String? ->
                pidRegistry.findBy(s)?.apply {
                    data.add(Metric.builder<Int>().command(ObdCommand(this)).build())
                }
            }
            return Pair(selectedPids, data)
        }
    }
}