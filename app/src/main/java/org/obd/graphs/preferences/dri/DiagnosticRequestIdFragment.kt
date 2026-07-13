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
package org.obd.graphs.preferences.dri

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.DiagnosticMappingItem
import org.obd.graphs.DiagnosticRequestIDManager
import org.obd.graphs.R

class DiagnosticRequestIdFragment : Fragment() {

    private lateinit var adapter: DiagnosticRequestIdAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dri_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAdd)

        adapter = DiagnosticRequestIdAdapter(
            items = DiagnosticRequestIDManager.getMappings().toMutableList(),
            onEditClicked = { item -> showEditDialog(item) },
            onDeleteClicked = { item -> deleteMapping(item) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            showEditDialog(null)
        }
    }

    private fun showEditDialog(item: DiagnosticMappingItem?) {
        val dialog = EditDiagnosticRequestIdBottomSheet(item) { requestKey, headerValue ->
            if (item != null) {
                val updatedItem = item.copy(requestKey = requestKey, headerValue = headerValue)
                DiagnosticRequestIDManager.saveMapping(updatedItem)
            } else {
                DiagnosticRequestIDManager.addMapping(requestKey, headerValue)
            }
            refreshList()
        }
        dialog.show(childFragmentManager, "EditDiagnosticRequestIdBottomSheet")
    }

    private fun deleteMapping(item: DiagnosticMappingItem) {
        DiagnosticRequestIDManager.deleteMapping(item)
        refreshList()
    }

    private fun refreshList() {
        adapter.updateData(DiagnosticRequestIDManager.getMappings())
    }
}
