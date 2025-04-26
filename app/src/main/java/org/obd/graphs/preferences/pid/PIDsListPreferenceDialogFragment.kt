 /**
 * Copyright 2019-2025, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.preferences.pid

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.TableLayout
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ViewPreferencesSerializer
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.bl.datalogger.vehicleCapabilitiesManager
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.preferences.CoreDialogFragment
import org.obd.graphs.preferences.PREFERENCE_SCREEN_SOURCE_PERFORMANCE
import org.obd.graphs.preferences.PREFERENCE_SCREEN_SOURCE_TRIP_INFO
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.updateStringSet
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.DragManageAdapter
import org.obd.graphs.ui.common.SwappableAdapter
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition
import java.util.*


private const val FILTER_BY_ECU_SUPPORTED_PIDS_PREF = "pref.pids.registry.filter_pids_ecu_supported"
private const val FILTER_BY_STABLE_PIDS_PREF = "pref.pids.registry.filter_pids_stable"
private const val HIGH_PRIO_PID_PREF = "pref.pids.generic.high"
private const val LOW_PRIO_PID_PREF = "pref.pids.generic.low"
private const val LOG_TAG = "PIDsDialog"

data class PidDefinitionDetails(val source: PidDefinition, var checked: Boolean = false, var supported: Boolean = true)

open class PIDsListPreferenceDialogFragment(
    private val key: String,
    private val source: String,
    private val onDialogCloseListener: (() -> Unit) = {}
) : CoreDialogFragment() {

    private lateinit var root: View
    private lateinit var listOfItems: MutableList<PidDefinitionDetails>
    private val detailsViewEnabled: Boolean = (source == "low" || source == "high")

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        requestWindowFeatures()

        root = inflater.inflate(R.layout.dialog_pids, container, false)

        listOfItems = sourceList()

        val adapter = PIDsViewAdapter(root, context, listOfItems, detailsViewEnabled)
        val recyclerView: RecyclerView = getRecyclerView(root)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        attachSearchView()
        attachDragManager(recyclerView)
        attachActionButtons()
        adjustItemsVisibility()

        adjustRecyclerViewHeight(recyclerView)

        return root
    }

    private fun adjustRecyclerViewHeight(recyclerView: RecyclerView) {
        val orientation = resources.configuration.orientation
        recyclerView.layoutParams.height = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350f, resources.displayMetrics).toInt()
        } else {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 450f, resources.displayMetrics).toInt()
        }
    }

    private fun adjustItemsVisibility() {
        root.findViewById<TableLayout>(R.id.details_view).apply {
            visibility = if (detailsViewEnabled) View.VISIBLE else View.GONE
        }
    }

    private fun attachSearchView() {

        val toolbar = root.findViewById<Toolbar>(R.id.custom_dialog_layout_toolbar)
        toolbar.inflateMenu(R.menu.pids_dialog_menu)
        val searchView = toolbar.menu.findItem(R.id.menu_searchview).actionView as SearchView
        searchView.setIconifiedByDefault(true)
        searchView.isIconified = false

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                    Log.d(LOG_TAG, "OnQueryTextSubmit newText=$query")
                }

                filterListOfItems(query)
                return false
            }

            override fun onQueryTextChange(newValue: String): Boolean {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                    Log.d(LOG_TAG, "OnQueryTextChange newValue=$newValue")
                }

                filterListOfItems(newValue)
                return false
            }
        })
    }

    private fun attachActionButtons() {

        root.findViewById<Button>(R.id.action_close_window).apply {
            setOnClickListener {
                dialog?.dismiss()
                onDialogCloseListener.invoke()
            }
        }

        root.findViewById<Button>(R.id.pid_list_save).apply {
            setOnClickListener {
                persistSelection(getAdapter().data)
                dialog?.dismiss()
                onDialogCloseListener.invoke()
            }
        }

        root.findViewById<Button>(R.id.pid_list_select_all).apply {
            setOnClickListener {
                val adapter: PIDsViewAdapter = getAdapter()

                adapter.data.forEach {
                    it.checked = true
                }
                adapter.notifyDataSetChanged()
            }
        }

        root.findViewById<Button>(R.id.pid_list_deselect_all).apply {
            setOnClickListener {
                val adapter: PIDsViewAdapter = getAdapter()

                adapter.data.forEach {
                    it.checked = false
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun attachDragManager(recyclerView: RecyclerView) {
        val viewSerializer = viewPreferencesSerializer()
        val swappableAdapter: SwappableAdapter = object : SwappableAdapter {
            override fun swapItems(fromPosition: Int, toPosition: Int) {
                if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                    Log.v(LOG_TAG, "swappableAdapter fromPosition=$fromPosition toPosition=$toPosition")
                }
                getAdapter().swapItems(fromPosition, toPosition)
            }

            override fun storePreferences(context: Context) {
                if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                    Log.v(LOG_TAG, "storePreferences for $key")
                }

                viewSerializer.store(getAdapter().data.map { it.source.id })
                notifyListChanged()
            }
        }

        val callback = DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.ACTION_STATE_DRAG, swappableAdapter
        )


        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }


    private fun persistSelection(list: List<PidDefinitionDetails>) {
        val newList = list.filter { it.checked }
            .map { it.source.id.toString() }.toList()

        Log.i(LOG_TAG, "Key=$key, selected PIDs=$newList")

        if (Prefs.getStringSet(key).toSet() != newList.toSet()) {
            notifyListChanged()
        }

        Prefs.updateStringSet(key, newList)
    }

    private fun notifyListChanged() {
        sendBroadcastEvent("${key}.event.changed")
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterListOfItems(newText: String) {
        try {
            val adapter = getAdapter()

            adapter.data.forEach { pp ->
                listOfItems.find { it.source.id == pp.source.id }?.let {
                    it.checked = pp.checked
                }
            }
            var filtered: MutableList<PidDefinitionDetails> = mutableListOf<PidDefinitionDetails>().apply {
                addAll(listOfItems)
            }

            var text = newText
            if (newText.contains("m:")) {
                val spaceIndex = newText.indexOf(" ")
                val module: String
                val colon = newText.indexOf(":") + 1

                if (spaceIndex > 0) {
                    text = newText.substring(spaceIndex, newText.length)
                    module = newText.substring(colon, spaceIndex)
                } else {
                    text = ""
                    module = newText.substring(colon, newText.length)
                }

                filtered = filtered.filter { it.source.resourceFile.lowercase(Locale.getDefault()).contains(module) }.toMutableList()
                Log.e(LOG_TAG, "Filtered module=$module and query=$text")
            }

            if (text.isNotEmpty()) {
                filtered = filtered.filter { it.source.description.lowercase(Locale.getDefault()).contains(text) }.toMutableList()
            }

            val sorted = sortItems(filtered)
            adapter.updateData(sorted)
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to filter user input", e)
        }
    }

    private fun getAdapter() = (getRecyclerView(root).adapter as PIDsViewAdapter)

    private fun sourceList(): MutableList<PidDefinitionDetails> {
        val all = dataLogger.getPidDefinitionRegistry().findAll()
        val individualQuery = dataLoggerPreferences.instance.individualQueryStrategyEnabled

        val sourceList: List<PidDefinitionDetails> =
            if (source == PREFERENCE_SCREEN_SOURCE_TRIP_INFO){
                val pidRegistry = dataLogger.getPidDefinitionRegistry()
                val list = Query.instance(QueryStrategyType.TRIP_INFO_QUERY).getDefaults()
                    .mapNotNull { pidRegistry.findBy(it) }
                    .toMutableList()
                list.map { PidDefinitionDetails(it, checked = false, supported = true) }

            } else if (source == PREFERENCE_SCREEN_SOURCE_PERFORMANCE) {
                val pidRegistry = dataLogger.getPidDefinitionRegistry()
                val list = Query.instance(QueryStrategyType.PERFORMANCE_QUERY).getDefaults()
                    .mapNotNull { pidRegistry.findBy(it) }
                    .toMutableList()
                list.map { PidDefinitionDetails(it, checked = false, supported = true) }
            } else if (individualQuery) {
                findPidDefinitionByPriority(dataLogger.getPidDefinitionRegistry().findAll()) { true }
            } else {
                when (source) {
                    "low" -> findPidDefinitionByPriority(all) { pidDefinition -> pidDefinition.priority > 0 }
                    "high" -> findPidDefinitionByPriority(all) { pidDefinition -> pidDefinition.priority == 0 }

                    "dashboard" -> {
                        map(all)
                    }

                    "graph" -> {
                        map(all)
                    }

                    "gauge" -> {
                        map(all)
                    }

                    "giulia" -> {
                        map(all)
                    }

                    "aa" -> {
                        map(all)
                    }

                    else -> findPidDefinitionByPriority(dataLogger.getPidDefinitionRegistry().findAll()) { true }
                }
            }

        Log.e(LOG_TAG,"source=${source}, size=${sourceList.size}")

        val pref = Prefs.getStringSet(key).map { s -> s.toLong() }
        sourceList.let {
            it.forEach { p ->
                if (pref.contains(p.source.id)) {
                    p.checked = true
                }
            }
        }

        return sortItems(sourceList)
    }

    private fun sortItems(
        input: List<PidDefinitionDetails>
    ): MutableList<PidDefinitionDetails> {

        val viewSerializer = viewPreferencesSerializer()
        val checked = input.filter { it.checked }.toMutableList()
        val sortOrder = viewSerializer.getItemsSortOrder()

        if (sortOrder == null) {
            viewSerializer.store(checked.map { it.source.id })
            notifyListChanged()
        } else {
            sortOrder.let { order ->
                if (order.isEmpty()) {
                    viewSerializer.store(checked.map { it.source.id })
                    notifyListChanged()
                } else {
                    try {
                        checked.sortWith { m1: PidDefinitionDetails, m2: PidDefinitionDetails ->
                            if (order.containsKey(m1.source.id) && order.containsKey(
                                    m2.source.id
                                )
                            ) {
                                order[m1.source.id]!!
                                    .compareTo(order[m2.source.id]!!)
                            } else {
                                m1.source.id.compareTo(m2.source.id)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e(LOG_TAG, "Failed to sort PIDs", e)
                    }
                }
            }

        }

        return (checked + input.filter { !it.checked }).toMutableList()
    }

    private fun viewPreferencesSerializer(): ViewPreferencesSerializer = ViewPreferencesSerializer("$key.view.settings")

    private fun map(all: MutableCollection<PidDefinition>): List<PidDefinitionDetails> {
        val source =
            Prefs.getStringSet(HIGH_PRIO_PID_PREF).map { s -> s.toLong() } + Prefs.getStringSet(LOW_PRIO_PID_PREF).map { s -> s.toLong() }
        return findPidDefinitionByPriority(all.filter { source.contains(it.id) }) { true }
    }

    private fun getRecyclerView(root: View): RecyclerView = root.findViewById(R.id.recycler_view)

    private fun findPidDefinitionByPriority(
        source: Collection<PidDefinition>,
        predicate: (PidDefinition) -> Boolean
    ): List<PidDefinitionDetails> {

        val ecuSupportedPIDs = vehicleCapabilitiesManager.getCapabilities()
        val ecuSupportedPIDsEnabled = Prefs.getBoolean(FILTER_BY_ECU_SUPPORTED_PIDS_PREF, false)
        val stablePIDsEnabled = Prefs.getBoolean(FILTER_BY_STABLE_PIDS_PREF, false)

        return source
            .asSequence()
            .filter { p -> p.group == PIDsGroup.LIVEDATA }
            .filter { p -> if (!stablePIDsEnabled) p.stable!! else true }
            .filter { p -> predicate.invoke(p) }
            .map { p -> PidDefinitionDetails(source = p, supported = isSupported(ecuSupportedPIDs, p)) }
            .filter { p -> if (ecuSupportedPIDsEnabled) true else p.supported }
            .toList()
    }

    private fun isSupported(
        ecuSupportedPIDs: MutableList<String>, p: PidDefinition
    ): Boolean = if (p.mode == "01") {
        ecuSupportedPIDs.contains(p.pid.lowercase())
    } else true
}
