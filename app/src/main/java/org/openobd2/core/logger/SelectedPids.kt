package org.openobd2.core.logger

import android.content.Context
import androidx.preference.PreferenceManager
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.bl.Pids
import org.openobd2.core.logger.ui.preferences.GENERIC_MODE
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.pid.PidRegistry


class SelectedPids {
    companion object {
        @JvmStatic
        fun get(context: Context, prefKey: String): Pair<MutableSet<String>, MutableList<CommandReply<*>>> {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            var selectedPids = pref.getStringSet(prefKey, emptySet())
            var pidRegistry: PidRegistry
            val mode: String

            when (Prefs.getMode(context)) {
                GENERIC_MODE -> {
                    mode = "01"
                    pidRegistry = Pids.instance.generic
                }
                else -> {
                    mode = "22"
                    pidRegistry = Pids.instance.custom
                }
            }

            var data: MutableList<CommandReply<*>> = arrayListOf()

            selectedPids!!.forEach { s: String? ->
                pidRegistry.findBy(mode, s)?.apply {
                    data.add(CommandReply<Int>(ObdCommand(this), 0, ""))
                }
            }

            data.sortBy { commandReply -> commandReply.command.label }
            return Pair(selectedPids, data)
        }
    }
}