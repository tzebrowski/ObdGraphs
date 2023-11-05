/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.preferences.pid

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TableLayout
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ViewPreferencesSerializer
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.vehicleCapabilitiesManager
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
private const val LOG_KEY = "PIDsDialog"

data class PidDefinitionDetails(val source: PidDefinition, var checked: Boolean = false, var supported: Boolean = true)

class PIDsListPreferenceDialog(private val key: String, private val detailsViewEnabled: Boolean = false,
                               private val source: String, private val onDialogCloseListener: (() -> Unit) = {}) :
    DialogFragment() {

    private lateinit var root: View
    private lateinit var listOfItems: MutableList<PidDefinitionDetails>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.let {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        root = inflater.inflate(R.layout.dialog_pids, container, false)
        val toolbar = root.findViewById<Toolbar>(R.id.custom_dialog_layout_toolbar)
        toolbar.inflateMenu(R.menu.pids_dialog_menu)

        val searchView = toolbar.menu.findItem(R.id.menu_searchview).actionView as SearchView
        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                if (Log.isLoggable(LOG_KEY,Log.DEBUG)) {
                    Log.d(LOG_KEY, "OnQueryTextSubmit newText=$query")
                }
                filterListOfItems(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (Log.isLoggable(LOG_KEY,Log.DEBUG)) {
                    Log.d(LOG_KEY, "OnQueryTextChange newText=$newText")
                }
                filterListOfItems(newText)
                return false
            }
        })

        val viewSerializer = ViewPreferencesSerializer("$key.view.settings")
        listOfItems = buildInitialList(viewSerializer)

        val adapter = PIDsViewAdapter(root, context, listOfItems, detailsViewEnabled)
        val recyclerView: RecyclerView = getRecyclerView(root)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        attachDragManager(viewSerializer, recyclerView)

        root.findViewById<TableLayout>(R.id.details_view).apply {
            visibility = if (detailsViewEnabled) View.VISIBLE else View.GONE
        }

        root.findViewById<Button>(R.id.pid_list_close_window).apply {
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
                adapter.data.forEach {
                    it.checked = true
                }
                adapter.notifyDataSetChanged()
            }
        }

        root.findViewById<Button>(R.id.pid_list_deselect_all).apply {
            setOnClickListener {
                adapter.data.forEach {
                    it.checked = false
                }
                adapter.notifyDataSetChanged()
            }
        }
        return root
    }

    private fun attachDragManager(
        viewSerializer: ViewPreferencesSerializer,
        recyclerView: RecyclerView
    ) {
        val swappableAdapter: SwappableAdapter = object : SwappableAdapter {
            override fun swapItems(fromPosition: Int, toPosition: Int) {
                if (Log.isLoggable(LOG_KEY,Log.VERBOSE)) {
                    Log.v(LOG_KEY, "swappableAdapter fromPosition=$fromPosition toPosition=$toPosition")
                }
                getAdapter().swapItems(fromPosition, toPosition)
            }

            override fun storePreferences(context: Context) {
                if (Log.isLoggable(LOG_KEY,Log.VERBOSE)) {
                    Log.v(LOG_KEY, "storePreferences for $key")
                }

                viewSerializer.store(getAdapter().data.map {it.source.id})
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

        Log.e(LOG_KEY, "Key=$key, selected PIDs=$newList")

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
        val adapter = getAdapter()

        adapter.data.forEach { pp ->
            listOfItems.find { it.source.id == pp.source.id }?.let {
                it.checked = pp.checked
            }
        }

        val filtered = listOfItems.filter { it.source.description.lowercase(Locale.getDefault()).contains(newText) }
        adapter.updateData(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun getAdapter() = (getRecyclerView(root).adapter as PIDsViewAdapter)

    private fun buildInitialList(viewSerializer: ViewPreferencesSerializer): MutableList<PidDefinitionDetails> {
        val all = dataLogger.getPidDefinitionRegistry().findAll()

        val list = when (source) {
            "low" -> findPidDefinitionByPriority(all) { pidDefinition -> pidDefinition.priority > 0 }
            "high" -> findPidDefinitionByPriority(all) { pidDefinition -> pidDefinition.priority == 0 }

            "dashboard" -> {
                buildListFromSource(all)
            }

            "graph" -> {
                buildListFromSource(all)
            }

            "gauge" -> {
                buildListFromSource(all)
            }

            "giulia" -> {
                buildListFromSource(all)
            }

            "aa" -> {
                buildListFromSource(all)
            }
            else -> findPidDefinitionByPriority(dataLogger.getPidDefinitionRegistry().findAll()) { true }
        }

        val pref = Prefs.getStringSet(key).map { s -> s.toLong() }
        list.let {
            it.forEach { p ->
                if (pref.contains(p.source.id)) {
                    p.checked = true
                }
            }
        }

        var toSort = list.toMutableList()
        viewSerializer.getItemsSortOrder()?.let { order ->
            if (order.isEmpty()){
                toSort = toSort.sortedBy { !it.checked }.toMutableList()
                viewSerializer.store(toSort.map {it.source.id})
                notifyListChanged()
            }else {
                try {
                    toSort.sortWith { m1: PidDefinitionDetails, m2: PidDefinitionDetails ->
                        if (order.containsKey(m1.source.id) && order.containsKey(
                                m2.source.id
                            )
                        ) {
                            order[m1.source.id]!!
                                .compareTo(order[m2.source.id]!!)
                        } else {
                            -1
                        }
                    }
                }catch (e: java.lang.IllegalArgumentException){
                    Log.e(LOG_KEY, "Failed to sort PIDs",e)
                }
            }
        }
        return toSort
    }

    private fun buildListFromSource(all: MutableCollection<PidDefinition>): List<PidDefinitionDetails> {
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