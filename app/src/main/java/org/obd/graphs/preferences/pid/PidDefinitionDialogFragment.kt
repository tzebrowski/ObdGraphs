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

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.obd.graphs.CustomPidRepository
import org.obd.graphs.R
import org.obd.graphs.preferences.CoreDialogFragment
import org.obd.graphs.ui.common.DragManageAdapter
import org.obd.graphs.ui.common.SwappableAdapter
import org.obd.graphs.ui.common.getScreenHeight
import org.obd.metrics.pid.PidDefinition

open class PidDefinitionDialogFragment(
    private val key: String,
    source: String,
    private val onDialogCloseListener: (() -> Unit) = {}
) : CoreDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var root: View

    private val dialogMode: PidDefinitionDialogMode = PidDefinitionDialogMode.fromString(source)
    private val viewModel: PidDefinitionViewModel by viewModels {
        PidViewModelFactory(key, dialogMode, CustomPidRepository(requireContext().applicationContext))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustRecyclerViewHeight(recyclerView, newConfig.orientation)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requestWindowFeatures()
        root = inflater.inflate(R.layout.dialog_pid, container, false)

        val adapter = PidDefinitionViewAdapter(
            context = context,
            data = emptyList(),
            editModeEnabled = dialogMode.isInteractive,
            onEditClicked = { clickedPid -> showEditBottomSheet(clickedPid) },
            onDeleteClicked = { clickedPid -> viewModel.delete(clickedPid) }
        )

        recyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        attachSearchView()
        attachDragManager(recyclerView)
        attachActionButtons()

        val fabAddPid = root.findViewById<View>(R.id.fab_add_pid)
        fabAddPid.visibility = if (dialogMode.isEdit) View.VISIBLE else View.GONE
        fabAddPid.setOnClickListener { showEditBottomSheet(null) }

        adjustRecyclerViewHeight(recyclerView, resources.configuration.orientation)
        observeViewModel(adapter)

        return root
    }

    private fun observeViewModel(adapter: PidDefinitionViewAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.updateData(state.filteredItems)
                }
            }
        }
    }

    private fun showEditBottomSheet(pidItem: PidDefinitionDetails?) {
        val bottomSheet = EditPidBottomSheet(pidItem, dialogMode) { formData ->
            if (pidItem != null) {
                if (dialogMode.isEdit) {
                    val parsedType = try {
                        org.obd.metrics.pid.ValueType.valueOf(formData.valueType)
                    } catch (e: Exception) {
                        org.obd.metrics.pid.ValueType.DOUBLE
                    }
                    pidItem.source.apply {
                        length = formData.length
                        formula = formData.formula ?: ""
                        mode = formData.mode ?: ""
                        pid = formData.pidCode ?: ""
                        units = formData.units ?: ""
                        description = formData.description ?: ""
                        min = formData.min
                        max = formData.max
                        type = parsedType
                        overrides = PidDefinition.Overrides(formData.canHeader, formData.batch, null)
                        longDescription = formData.longDescription
                        stable = formData.stable
                        alert.lowerThreshold = formData.lowerThreshold
                        alert.upperThreshold = formData.upperThreshold
                    }
                } else {
                    pidItem.source.apply {
                        alert.lowerThreshold = formData.lowerThreshold
                        alert.upperThreshold = formData.upperThreshold
                    }
                }

                viewModel.update(pidItem)

                recyclerView.post {
                    getAdapter().notifyDataSetChanged()
                }
            } else if (dialogMode.isEdit) {
                val type = try {
                    org.obd.metrics.pid.ValueType.valueOf(formData.valueType)
                } catch (_: Exception) {
                    org.obd.metrics.pid.ValueType.DOUBLE
                }

                val newPid = PidDefinition(
                    System.currentTimeMillis(),
                    formData.length,
                    formData.formula ?: "",
                    formData.mode ?: "",
                    formData.pidCode ?: "",
                    formData.units ?: "",
                    formData.description ?: "",
                    formData.min,
                    formData.max,
                    type,
                    PidDefinition.Overrides(formData.canHeader, formData.batch, null)
                ).apply {
                    longDescription = formData.longDescription
                    stable = formData.stable
                    alert.lowerThreshold = formData.lowerThreshold
                    alert.upperThreshold = formData.upperThreshold
                }
                viewModel.save(newPid)
            }
        }
        bottomSheet.show(childFragmentManager, "EditPidBottomSheet")
    }

    private fun adjustRecyclerViewHeight(recyclerView: RecyclerView, orientation: Int) {
        recyclerView.layoutParams.height = if (dialogMode.isInteractive) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getScreenHeight() * 0.2f, resources.displayMetrics).toInt()
            } else {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getScreenHeight() * 0.6f, resources.displayMetrics).toInt()
            }
        } else {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getScreenHeight() * 0.65f, resources.displayMetrics).toInt()
            } else {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getScreenHeight() * 0.85f, resources.displayMetrics).toInt()
            }
        }
    }

    private fun attachSearchView() {
        val toolbar = root.findViewById<Toolbar>(R.id.custom_dialog_layout_toolbar)
        toolbar.inflateMenu(R.menu.pids_dialog_menu)
        val searchView = toolbar.menu.findItem(R.id.menu_searchview).actionView as SearchView

        searchView.setIconifiedByDefault(true)
        searchView.isIconified = false
        searchView.queryHint = getString(R.string.pref_pids_search_hint)

        val searchEditText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.let {
            it.setTextColor(android.graphics.Color.BLACK)
            it.setHintTextColor(android.graphics.Color.DKGRAY)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.onSearchQueryChanged(query)
                return false
            }

            override fun onQueryTextChange(newValue: String): Boolean {
                viewModel.onSearchQueryChanged(newValue)
                return false
            }
        })
    }

    private fun attachActionButtons() {
        root.findViewById<Button>(R.id.pid_list_save).setOnClickListener {
            viewModel.persistSelection()
        }

        root.findViewById<Button>(R.id.action_close_window).setOnClickListener {
            dialog?.dismiss()
            onDialogCloseListener.invoke()
        }

        val btnSelectAll = root.findViewById<Button>(R.id.pid_list_select_all)
        btnSelectAll.visibility = if (dialogMode.isInteractive) View.GONE else View.VISIBLE
        btnSelectAll.setOnClickListener {
            viewModel.toggleSelectAll(true)
        }

        val btnDeselectAll = root.findViewById<Button>(R.id.pid_list_deselect_all)
        btnDeselectAll.visibility = if (dialogMode.isInteractive) View.GONE else View.VISIBLE
        btnDeselectAll.setOnClickListener {
            viewModel.toggleSelectAll(false)
        }
    }

    private fun attachDragManager(recyclerView: RecyclerView) {
        val swappableAdapter = object : SwappableAdapter {
            override fun swapItems(fromPosition: Int, toPosition: Int) {
                getAdapter().swapItems(fromPosition, toPosition)
            }

            override fun storePreferences(context: Context) {
                viewModel.reorderItems(getAdapter().data)
            }
        }

        val callback = DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.ACTION_STATE_DRAG,
            swappableAdapter
        )

        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun getAdapter() = (recyclerView.adapter as PidDefinitionViewAdapter)
}
