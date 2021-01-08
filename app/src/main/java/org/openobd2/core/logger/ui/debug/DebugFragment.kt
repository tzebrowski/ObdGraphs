package org.openobd2.core.logger.ui.debug

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLoggerService

class DebugFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_debug, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)

        Model.text.observe(viewLifecycleOwner, Observer {
            textView.append(it + "\n")
        })

        val btnStop: FloatingActionButton = root.findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Stop data logging ")
            DataLoggerService.stopAction(this.requireContext())
        });

        val btnStart: FloatingActionButton =  root.findViewById(R.id.btn_start);
        btnStart.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Start data logging")
            DataLoggerService.startAction(this.requireContext())
        });
        return root
    }
}