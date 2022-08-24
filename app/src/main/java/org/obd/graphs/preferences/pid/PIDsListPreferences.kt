package org.obd.graphs.preferences.pid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.obd.metrics.pid.PidDefinition
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.bl.datalogger.WORKFLOW_RELOAD_EVENT
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getECUSupportedPIDs
import org.obd.metrics.command.group.DefaultCommandGroup
import java.util.*

private val supportedPIDsIds = DefaultCommandGroup.SUPPORTED_PIDS.commands.map { c -> c.pid.id }.toSet()

private const val FILTER_BY_ECU_SUPPORTED_PIDS_PREF = "pref.pids.registry.filter_pids_ecu_supported"
private const val FILTER_BY_STABLE_PIDS_PREF = "pref.pids.registry.filter_pids_stable"

class PIDsListPreferences(
    context: Context?,
    attrs: AttributeSet?
): MultiSelectListPreference(context, attrs) {

    private val priority = getPriority(attrs)
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action === WORKFLOW_RELOAD_EVENT) {
                initialize()
            }
        }
    }

    init {
        initialize()
    }

    override fun onDetached() {
        super.onDetached()
        context?.unregisterReceiver(broadcastReceiver)
    }

    override fun onAttached() {
        super.onAttached()
        registerReceiver(context)
    }

    private fun registerReceiver(context: Context?) {
        context?.registerReceiver(
            broadcastReceiver,
            IntentFilter().apply {
                addAction(WORKFLOW_RELOAD_EVENT)
            }
        )
    }

    private fun initialize() {
        setDefaultValue(hashSetOf<String>())
        when (priority) {
            "low" -> findPidDefinitionByPriority { pidDefinition -> pidDefinition.priority > 0 }
            "high" -> findPidDefinitionByPriority { pidDefinition -> pidDefinition.priority == 0 }
            else -> Pair(mutableListOf(), mutableListOf())
        }.let {
            entries = it.first.toTypedArray()
            entryValues = it.second.toTypedArray()
        }
    }

    private fun findPidDefinitionByPriority(predicate: (PidDefinition) -> Boolean): Pair<MutableList<CharSequence>, MutableList<CharSequence>> {
        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        val ecuSupportedPIDs = getECUSupportedPIDs()
        val ecuSupportedPIDsEnabled =  Prefs.getBoolean(FILTER_BY_ECU_SUPPORTED_PIDS_PREF,false)
        val stablePIDsEnabled =  Prefs.getBoolean(FILTER_BY_STABLE_PIDS_PREF,true)

        getPidList()
            .asSequence()
            .filter { p -> !supportedPIDsIds.contains(p.id)}
            .filter { p -> if (stablePIDsEnabled)  p.stable!! else true }
            .filter { p -> predicate.invoke(p) }
            .filter { p-> if (ecuSupportedPIDsEnabled && p.mode == "01")
                ecuSupportedPIDs.contains(p.pid.lowercase()) else true }
            .sortedBy { p -> p.displayString() .toString()}
            .toList()
            .forEach { p ->
                entries.add(p.displayString())
                entriesValues.add(p.id.toString())
            }

        return Pair(entries, entriesValues)
    }

    private fun getPidList() = DataLogger.instance.pidDefinitionRegistry().findAll()

    private fun getPriority(attrs: AttributeSet?): String = if (attrs == null) {
        ""
    } else {
        val priority: String? = (0 until attrs.attributeCount)
            .filter { index -> attrs.getAttributeName(index) == "priority" }
            .map { index -> attrs.getAttributeValue(index) }.firstOrNull()
        priority ?: ""
    }
}