/*
 * Copyright 2019-2026, Tomasz Żebrowski
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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.obd.graphs.CustomPidRepository
import org.obd.graphs.MODULES_LIST_CHANGED_EVENT
import org.obd.graphs.ViewPreferencesSerializer
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.VehicleCapabilitiesManager
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.datalogger.isUserCustom
import org.obd.graphs.bl.datalogger.serialize
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.updateStringSet
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition
import java.util.Locale

private const val FILTER_BY_ECU_SUPPORTED_PIDS_PREF = "pref.pids.registry.filter_pids_ecu_supported"
private const val FILTER_BY_STABLE_PIDS_PREF = "pref.pids.registry.filter_pids_stable"
private const val HIGH_PRIO_PID_PREF = "pref.pids.generic.high"
private const val LOW_PRIO_PID_PREF = "pref.pids.generic.low"
private const val LOG_TAG = "PidViewModel"

data class PidDefinitionDetails(
    val source: PidDefinition,
    var checked: Boolean = false,
    var supported: Boolean = true
)

data class PidUiState(
    val items: List<PidDefinitionDetails> = emptyList(),
    val filteredItems: List<PidDefinitionDetails> = emptyList(),
    val searchQuery: String = ""
)

class PidDefinitionViewModel(
    private val key: String,
    val dialogMode: PidDefinitionDialogMode,
    private val customPidRepository: CustomPidRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PidUiState())
    val uiState: StateFlow<PidUiState> = _uiState.asStateFlow()

    private var allMasterItems: MutableList<PidDefinitionDetails> = mutableListOf()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedItems = sourceList()
            allMasterItems = loadedItems
            _uiState.value = _uiState.value.copy(
                items = loadedItems,
                filteredItems = applyFilter(allMasterItems, _uiState.value.searchQuery)
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val filtered = applyFilter(allMasterItems, query)
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                filteredItems = filtered
            )
        }
    }

    fun updatePidAlerts(pidItem: PidDefinitionDetails, lower: Number?, upper: Number?) {
        viewModelScope.launch(Dispatchers.IO) {
            pidItem.source.alert.lowerThreshold = lower
            pidItem.source.alert.upperThreshold = upper
            pidItem.source.serialize()

            allMasterItems = allMasterItems.map { item ->
                if (item.source.id == pidItem.source.id) {
                    item.copy(source = item.source)
                } else {
                    item
                }
            }.toMutableList()

            val filtered = applyFilter(allMasterItems, _uiState.value.searchQuery)

            _uiState.value = PidUiState(
                items = ArrayList(allMasterItems),
                filteredItems = ArrayList(filtered),
                searchQuery = _uiState.value.searchQuery
            )
        }
    }

    fun toggleSelectAll(selectAll: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            allMasterItems.forEach { it.checked = selectAll }
            val updated = applyFilter(allMasterItems, _uiState.value.searchQuery)
            _uiState.value = _uiState.value.copy(items = allMasterItems, filteredItems = updated)
        }
    }

    fun saveCustomPid(newPid: PidDefinition) {
        viewModelScope.launch(Dispatchers.IO) {
            customPidRepository.save(newPid)
            sendBroadcastEvent(MODULES_LIST_CHANGED_EVENT)
        }
    }

    fun deleteCustomPid(pidItem: PidDefinitionDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            customPidRepository.delete(pidItem.source.id)

            allMasterItems = allMasterItems.filterNot { it.source.id == pidItem.source.id }.toMutableList()

            val sorted = sortItems(allMasterItems)
            allMasterItems = sorted
            val filtered = applyFilter(sorted, _uiState.value.searchQuery)

            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _uiState.value = PidUiState(
                    items = ArrayList(allMasterItems),
                    filteredItems = ArrayList(filtered),
                    searchQuery = _uiState.value.searchQuery
                )
            }

            sendBroadcastEvent(MODULES_LIST_CHANGED_EVENT)
        }
    }

    fun persistSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            val newList = allMasterItems.filter { it.checked }.map { it.source.id.toString() }
            if (Prefs.getStringSet(key).toSet() != newList.toSet()) {
                Log.i(LOG_TAG, "Persisting PID list for key=$key, new list=$newList")
                sendBroadcastEvent("$key.event.changed")
                Prefs.updateStringSet(key, newList)
            } else {
                Log.i(LOG_TAG, "Do not persist PID list for key=$key, it did not change")
            }
        }
    }

    fun reorderItems(items: List<PidDefinitionDetails>) {
        viewModelScope.launch(Dispatchers.IO) {
            val serializer = viewPreferencesSerializer()
            serializer.store(items.map { it.source.id })
            sendBroadcastEvent("$key.event.changed")
        }
    }

    private fun applyFilter(input: List<PidDefinitionDetails>, query: String): List<PidDefinitionDetails> {
        if (query.isEmpty()) return input

        var filtered = input.toList()
        var searchText = query

        if (query.contains("m:")) {
            val spaceIndex = query.indexOf(" ")
            val colon = query.indexOf(":") + 1
            val module = if (spaceIndex > 0) {
                searchText = query.substring(spaceIndex).trim()
                query.substring(colon, spaceIndex)
            } else {
                searchText = ""
                query.substring(colon)
            }

            filtered = filtered.filter {
                it.source.resourceFile.lowercase(Locale.getDefault()).contains(module.lowercase(Locale.getDefault()))
            }
        }

        if (searchText.isNotEmpty()) {
            filtered = filtered.filter {
                it.source.description.lowercase(Locale.getDefault()).contains(searchText.lowercase(Locale.getDefault()))
            }
        }

        return filtered
    }

    private fun sourceList(): MutableList<PidDefinitionDetails> {
        val all = DataLoggerRepository.getPidDefinitionRegistry().findAll()
        val individualQuery = dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled
        val pidRegistry = DataLoggerRepository.getPidDefinitionRegistry()

        val sourceList: List<PidDefinitionDetails> = when {
            dialogMode is PidDefinitionDialogMode.TripInfo -> {
                Query.instance(QueryStrategyType.TRIP_INFO_QUERY).getDefaultPIDs()
                    .mapNotNull { pidRegistry.findBy(it) }
                    .map { PidDefinitionDetails(it, checked = false, supported = true) }
            }
            dialogMode is PidDefinitionDialogMode.Performance -> {
                Query.instance(QueryStrategyType.PERFORMANCE_QUERY).getDefaultPIDs()
                    .mapNotNull { pidRegistry.findBy(it) }
                    .map { PidDefinitionDetails(it, checked = false, supported = true) }
            }
            individualQuery -> {
                findPidDefinitionByPriority(pidRegistry.findAll()) { true }
            }
            else -> {
                when (dialogMode) {
                    is PidDefinitionDialogMode.LowPriority -> findPidDefinitionByPriority(all) { it.priority > 0 }
                    is PidDefinitionDialogMode.HighPriority -> findPidDefinitionByPriority(all) { it.priority == 0 }
                    is PidDefinitionDialogMode.Dashboard, is PidDefinitionDialogMode.Graph,
                    is PidDefinitionDialogMode.Gauge, is PidDefinitionDialogMode.Giulia,
                    is PidDefinitionDialogMode.AA -> map(all)
                    else -> findPidDefinitionByPriority(pidRegistry.findAll()) { true }
                }
            }
        }

        val pref = Prefs.getStringSet(key).map { s -> s.toLong() }
        sourceList.forEach { p ->
            if (pref.contains(p.source.id)) {
                p.checked = true
            }
        }

        return sortItems(sourceList)
    }

    private fun sortItems(input: List<PidDefinitionDetails>): MutableList<PidDefinitionDetails> {
        val viewSerializer = viewPreferencesSerializer()
        val sortOrder = viewSerializer.getItemsSortOrder()

        val (userPids, standardPids) = input.partition { it.source.isUserCustom }

        val checkedStandard = standardPids.filter { it.checked }.toMutableList()
        val uncheckedStandard = standardPids.filter { !it.checked }

        if (sortOrder == null || sortOrder.isEmpty()) {
            viewSerializer.store(checkedStandard.map { it.source.id })
            sendBroadcastEvent("$key.event.changed")
        } else {
            try {
                checkedStandard.sortWith { m1, m2 ->
                    if (sortOrder.containsKey(m1.source.id) && sortOrder.containsKey(m2.source.id)) {
                        sortOrder[m1.source.id]!!.compareTo(sortOrder[m2.source.id]!!)
                    } else {
                        m1.source.id.compareTo(m2.source.id)
                    }
                }
            } catch (e: Throwable) {
                Log.e(LOG_TAG, "Failed to sort PIDs", e)
            }
        }

        return (userPids + checkedStandard + uncheckedStandard).toMutableList()
    }

    private fun viewPreferencesSerializer(): ViewPreferencesSerializer = ViewPreferencesSerializer("$key.view.settings")

    private fun map(all: MutableCollection<PidDefinition>): List<PidDefinitionDetails> {
        val source = Prefs.getStringSet(HIGH_PRIO_PID_PREF).map { s -> s.toLong() } +
            Prefs.getStringSet(LOW_PRIO_PID_PREF).map { s -> s.toLong() }
        return findPidDefinitionByPriority(all.filter { source.contains(it.id) }) { true }
    }

    private fun findPidDefinitionByPriority(
        source: Collection<PidDefinition>,
        predicate: (PidDefinition) -> Boolean
    ): List<PidDefinitionDetails> {
        val ecuSupportedPIDs = VehicleCapabilitiesManager.getSupportedPIDs()
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

    private fun isSupported(ecuSupportedPIDs: MutableList<String>, p: PidDefinition): Boolean =
        if (p.mode == "01") ecuSupportedPIDs.contains(p.pid.lowercase()) else true
}

class PidViewModelFactory(
    private val key: String,
    private val dialogMode: PidDefinitionDialogMode,
    private val customPidRepository: CustomPidRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PidDefinitionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PidDefinitionViewModel(key, dialogMode, customPidRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
