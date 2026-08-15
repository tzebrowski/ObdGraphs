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
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
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
    private val orderPrefKey: String? = null,
    private val dragReorderEnabled: Boolean = true,
    private val onDialogCloseListener: (() -> Unit) = {}
) : CoreDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var root: View
    private lateinit var categoryIndexBar: LinearLayout

    private val dialogMode: PidDefinitionDialogMode = PidDefinitionDialogMode.fromString(source)
    private val viewModel: PidDefinitionViewModel by viewModels {
        PidViewModelFactory(key, dialogMode, CustomPidRepository(requireContext().applicationContext), orderPrefKey)
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
            onDeleteClicked = { clickedPid -> viewModel.delete(clickedPid) },
            onCategoryToggled = { category, checked -> viewModel.setCategoryChecked(category, checked) }
        )

        recyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        categoryIndexBar = root.findViewById(R.id.category_index_bar)

        attachSearchView()
        if (dragReorderEnabled) {
            attachDragManager(recyclerView)
        }
        attachActionButtons()

        val fabAddPid = root.findViewById<View>(R.id.fab_add_pid)
        fabAddPid.visibility = if (dialogMode.isEdit) View.VISIBLE else View.GONE
        fabAddPid.setOnClickListener { showAddBottomSheet() }

        adjustRecyclerViewHeight(recyclerView, resources.configuration.orientation)
        observeViewModel(adapter)

        return root
    }

    private fun observeViewModel(adapter: PidDefinitionViewAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.updateData(state.filteredItems)
                    updateCategoryIndexBar(state.filteredItems)
                }
            }
        }
    }

    // A jump-to-group index along the left edge, so a long PID list doesn't require scrolling to
    // find a specific category (or the Selected group) -- only shown once there's more than one
    // group to jump between, and rebuilt whenever the visible group set changes (e.g. search).
    private fun updateCategoryIndexBar(items: List<PidDefinitionDetails>) {
        val anchors = LinkedHashMap<PidSection, Int>()
        items.forEachIndexed { index, item ->
            val section = sectionFor(item) ?: return@forEachIndexed
            anchors.putIfAbsent(section, index)
        }

        categoryIndexBar.removeAllViews()

        if (anchors.size < 2) {
            categoryIndexBar.visibility = View.GONE
            setRecyclerViewIndexBarInset(reserved = false)
            return
        }

        categoryIndexBar.visibility = View.VISIBLE
        setRecyclerViewIndexBarInset(reserved = true)
        anchors.forEach { (section, position) ->
            val label = when (section) {
                is PidSection.Selected -> getString(R.string.pref_pid_manage_dialog_selected_pids_short)
                is PidSection.Category -> getString(section.category.shortStringRes)
            }
            categoryIndexBar.addView(createIndexMarker(label, position))
        }
    }

    // The list itself gets pushed to the right of the index bar rather than having the bar float
    // on top of it, so the bar never covers the PID checkboxes/text underneath.
    private fun setRecyclerViewIndexBarInset(reserved: Boolean) {
        val marginPx = if (reserved) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics).toInt()
        } else {
            0
        }
        (recyclerView.layoutParams as? RelativeLayout.LayoutParams)?.let {
            if (it.marginStart != marginPx) {
                it.marginStart = marginPx
                recyclerView.layoutParams = it
            }
        }
    }

    // Markers are square (matching the bar's own width) so the -90 rotation needed for vertical
    // text doesn't make the drawn text spill outside its own bounds, and the box is tall enough
    // to be a comfortable tap target -- the previous single-line horizontal abbreviation was too
    // short vertically to hit reliably.
    private fun createIndexMarker(label: String, targetPosition: Int): TextView {
        val sizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics).toInt()
        return TextView(requireContext()).apply {
            text = label
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            rotation = -90f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, sizePx).apply {
                val marginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
                bottomMargin = marginPx
            }
            setOnClickListener {
                (recyclerView.layoutManager as? GridLayoutManager)?.scrollToPositionWithOffset(targetPosition, 0)
            }
        }
    }

    private fun showAddBottomSheet() {
        val bottomSheet = EditPidBottomSheet(null, dialogMode) { formData ->
            if (dialogMode.isEdit) {
                val parsedType = parseValueType(formData.valueType)
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
                    parsedType,
                    PidDefinition.Overrides(formData.canHeader, formData.batch, formData.canNetwork)
                ).apply {
                    group = org.obd.metrics.pid.PIDsGroup.LIVEDATA
                    longDescription = formData.longDescription
                    stable = formData.stable
                    alert.lowerThreshold = formData.lowerThreshold
                    alert.upperThreshold = formData.upperThreshold
                }
                viewModel.save(newPid)
            }
        }
        bottomSheet.show(childFragmentManager, "AddPidBottomSheet")
    }

    private fun showEditBottomSheet(pidItem: PidDefinitionDetails) {
        val bottomSheet = EditPidBottomSheet(pidItem, dialogMode) { formData ->
            if (dialogMode.isEdit) {
                val parsedType = parseValueType(formData.valueType)
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
                    overrides = PidDefinition.Overrides(formData.canHeader, formData.batch, formData.canNetwork)
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
        }
        bottomSheet.show(childFragmentManager, "EditPidBottomSheet")
    }

    private fun parseValueType(valueType: String): org.obd.metrics.pid.ValueType {
        return try {
            org.obd.metrics.pid.ValueType.valueOf(valueType)
        } catch (e: Exception) {
            org.obd.metrics.pid.ValueType.DOUBLE
        }
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

        val textColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dialog_text_primary)
        val hintColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dialog_text_secondary)

        val searchEditText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.let {
            it.setTextColor(textColor)
            it.setHintTextColor(hintColor)
        }

        val searchIcon = searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon?.setColorFilter(textColor)

        val closeIcon = searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon?.setColorFilter(textColor)

        toolbar.navigationIcon?.setTint(textColor)

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
        val btnSave = root.findViewById<Button>(R.id.pid_list_save)
        btnSave.visibility = if (dialogMode.isInteractive) View.GONE else View.VISIBLE
        btnSave.setOnClickListener {
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
