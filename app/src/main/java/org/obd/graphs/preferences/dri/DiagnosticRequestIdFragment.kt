package org.obd.graphs.preferences.dri

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.R

class DiagnosticRequestIdFragment : Fragment() {

    private lateinit var adapter: DiagnosticRequestIdAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diagnostic_request_id_manager, container, false)
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
                // Update existing
                val updatedItem = item.copy(requestKey = requestKey, headerValue = headerValue)
                DiagnosticRequestIDManager.saveMapping(updatedItem)
            } else {
                // Create new
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
