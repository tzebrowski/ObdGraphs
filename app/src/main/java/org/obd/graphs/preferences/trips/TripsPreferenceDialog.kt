package org.obd.graphs.preferences.trips

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.*
import org.obd.graphs.activity.navigateToScreen
import org.obd.graphs.bl.trip.tripManager

class TripsPreferenceDialog : DialogFragment() {
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
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
        val root = inflater.inflate(R.layout.dialog_trips, container, false)
        val adapter = TripsViewAdapter(context, tripManager.findAllTripsBy())
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter

        root.findViewById<Button>(R.id.pid_list_close_window).apply {
            setOnClickListener {
                navigateToScreen(R.id.navigation_graph)
                dialog?.dismiss()
            }
        }

        root.findViewById<Button>(R.id.trip_delete_all).apply {
            setOnClickListener {
                val builder = AlertDialog.Builder(context)
                val title = context.getString(R.string.trip_delete_dialog_ask_question)
                val yes = context.getString(R.string.trip_delete_dialog_ask_question_yes)
                val no = context.getString(R.string.trip_delete_dialog_ask_question_no)

                builder.setMessage(title)
                    .setCancelable(false)
                    .setPositiveButton(yes) { _, _ ->
                        try {

                            sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT)
                            adapter.data.forEach {
                                tripManager.deleteTrip(it)
                            }
                            adapter.data.clear()
                            adapter.notifyDataSetChanged()

                        } finally {
                            sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
                        }
                    }
                    .setNegativeButton(no) { dialog, _ ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
            }
        }
        return root
    }
}