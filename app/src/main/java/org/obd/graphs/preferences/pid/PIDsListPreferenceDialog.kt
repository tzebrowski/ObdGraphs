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
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
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
import java.util.*


private const val FILTER_BY_ECU_SUPPORTED_PIDS_PREF = "pref.pids.registry.filter_pids_ecu_supported"
private const val FILTER_BY_STABLE_PIDS_PREF = "pref.pids.registry.filter_pids_stable"


data class PidDefinitionDetails(val source: PidDefinition, var checked: Boolean = false, var supported: Boolean =  true)

class PIDsListPreferenceDialog(private val key: String, private val priority: String) : DialogFragment() {

    private lateinit var root: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.let {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
        super.onViewCreated(view, savedInstanceState)
    }
    private lateinit var listOfItems: List<PidDefinitionDetails>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        root = inflater.inflate(R.layout.dialog_pids, container, false)
        val toolbar = root.findViewById<Toolbar>(R.id.custom_dialog_layout_toolbar)
        toolbar.inflateMenu(R.menu.pids_dialog_menu)

        val searchView = toolbar.menu.findItem(R.id.menu_searchview).actionView as SearchView
        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                Log.i("PIDsListPreferenceDialog", "OnQueryTextSubmit newText=$query")
                filterListOfItems(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                Log.i("PIDsListPreferenceDialog", "OnQueryTextChange newText=$newText")
                filterListOfItems(newText)
                return false
            }
        })

        listOfItems = findPIDs()

        val adapter = PIDsViewAdapter(context, listOfItems)
        val recyclerView: RecyclerView = getRecyclerView(root)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        root.findViewById<Button>(R.id.pid_list_close_window).apply {
            setOnClickListener {
                dialog?.dismiss()
            }
        }

        root.findViewById<Button>(R.id.pid_list_save).apply {
            setOnClickListener {
                persistSelection()
                dialog?.dismiss()
            }
        }

        return root
    }

    private fun persistSelection() {
        val pidList = getAdapter().data
            .filter { it.checked }
            .map { it.source.id.toString() }.toList()

        Log.i("PIDsListPreferenceDialog", "Key=$key, selected PIDs=$pidList")
        Prefs.updateStringSet(key, pidList)
    }

    private fun filterListOfItems(newText: String) {
        val adapter = getAdapter()

        adapter.data.forEach { pp ->
            listOfItems.find { it.source.id == pp.source.id }?.let {
                it.checked = pp.checked
            }
        }

        val ccc = listOfItems.filter { it.source.description.lowercase(Locale.getDefault()).contains(newText) }
        adapter.updateData(ccc)
        adapter.notifyDataSetChanged()
    }

    private fun getAdapter() = (getRecyclerView(root).adapter as PIDsViewAdapter)

    private fun findPIDs(): List<PidDefinitionDetails> {
        val pref = Prefs.getStringSet(key).map { s -> s.toLong() }

        val list = when (priority) {
            "low" -> findPidDefinitionByPriority { pidDefinition -> pidDefinition.priority > 0 }
            "high" -> findPidDefinitionByPriority { pidDefinition -> pidDefinition.priority == 0 }
            else -> mutableListOf()
        }

        list.let {
            it.forEach { p ->
                if (pref.contains(p.source.id)) {
                    p.checked = true
                }
            }
        }
        return list
    }

    private fun getRecyclerView(root: View): RecyclerView = root.findViewById(R.id.recycler_view)


    private fun findPidDefinitionByPriority(predicate: (PidDefinition) -> Boolean): List<PidDefinitionDetails> {

        val ecuSupportedPIDs = vehicleCapabilitiesManager.getCapabilities()
        val ecuSupportedPIDsEnabled = Prefs.getBoolean(FILTER_BY_ECU_SUPPORTED_PIDS_PREF, false)
        val stablePIDsEnabled = Prefs.getBoolean(FILTER_BY_STABLE_PIDS_PREF, false)

        return getPidList()
            .asSequence()
            .filter { p -> p.group == PIDsGroup.LIVEDATA }
            .filter { p -> if (!stablePIDsEnabled) p.stable!! else true }
            .filter { p -> predicate.invoke(p) }
            .map { p -> PidDefinitionDetails(source=p, supported=isSupported(ecuSupportedPIDs, p))}
            .filter { p-> if (ecuSupportedPIDsEnabled) true else p.supported }
            .toList()
    }

    private fun isSupported(
        ecuSupportedPIDs: MutableList<String>, p: PidDefinition) : Boolean  =  if (p.mode == "01"){
             ecuSupportedPIDs.contains(p.pid.lowercase())
        } else true

    private fun getPidList() = dataLogger.getPidDefinitionRegistry().findAll()
}