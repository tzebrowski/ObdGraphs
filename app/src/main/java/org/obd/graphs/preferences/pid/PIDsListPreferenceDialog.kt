package org.obd.graphs.preferences.pid

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.vehicleCapabilitiesManager
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.updateStringSet
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition

private const val FILTER_BY_ECU_SUPPORTED_PIDS_PREF = "pref.pids.registry.filter_pids_ecu_supported"
private const val FILTER_BY_STABLE_PIDS_PREF = "pref.pids.registry.filter_pids_stable"


data class PidDefinitionWrapper(val source: PidDefinition, var checked: Boolean = false, var supported: Boolean =  true)


class PIDsListPreferenceDialog(private val key: String, private val priority: String) : DialogFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.let {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
        super.onViewCreated(view, savedInstanceState)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.dialog_pids, container, false)

        when (priority) {
            "low" -> findPidDefinitionByPriority { pidDefinition -> pidDefinition.priority > 0 }
            "high" -> findPidDefinitionByPriority { pidDefinition -> pidDefinition.priority == 0 }
            else -> mutableListOf()
        }.let {
            val pref = Prefs.getStringSet(key).map { s -> s.toLong() }
            it.forEach { p ->
                if (pref.contains(p.source.id)) {
                    p.checked = true
                }
            }

            val adapter = PIDsViewAdapter(context, it)
            val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
            recyclerView.layoutManager = GridLayoutManager(context, 1)
            recyclerView.adapter = adapter
        }


        root.findViewById<Button>(R.id.pid_list_close_window).apply {
            setOnClickListener {
                dialog?.dismiss()
            }
        }

        root.findViewById<Button>(R.id.pid_list_save).apply {
            setOnClickListener {
                val pidList = getSelectedPIDs(root)
                Log.i("PIDsListPreferenceDialog", "Key=$key, selected PIDs=$pidList")
                Prefs.updateStringSet(key, pidList)
                dialog?.dismiss()
            }
        }

        return root
    }

    private fun getSelectedPIDs(root: View): List<String> =
        (root.findViewById<RecyclerView>(R.id.recycler_view).adapter as PIDsViewAdapter).data
            .filter { it.checked }
            .map { it.source.id.toString() }.toList()


    private fun findPidDefinitionByPriority(predicate: (PidDefinition) -> Boolean): List<PidDefinitionWrapper> {

        val ecuSupportedPIDs = vehicleCapabilitiesManager.getCapabilities()
        val ecuSupportedPIDsEnabled = Prefs.getBoolean(FILTER_BY_ECU_SUPPORTED_PIDS_PREF, false)
        val stablePIDsEnabled = Prefs.getBoolean(FILTER_BY_STABLE_PIDS_PREF, false)

        return getPidList()
            .asSequence()
            .filter { p -> p.group == PIDsGroup.LIVEDATA }
            .filter { p -> if (!stablePIDsEnabled) p.stable!! else true }
            .filter { p -> predicate.invoke(p) }
            .map { p -> PidDefinitionWrapper(source=p, supported=isSupported(ecuSupportedPIDs, p))}
            .filter { p-> if (ecuSupportedPIDsEnabled) true else p.supported }
            .toList()
    }

    private fun isSupported(
        ecuSupportedPIDs: MutableList<String>, p: PidDefinition) : Boolean  =  if (p.mode == "01"){
             ecuSupportedPIDs.contains(p.pid.lowercase())
        } else true

    private fun getPidList() = dataLogger.getPidDefinitionRegistry().findAll()
}