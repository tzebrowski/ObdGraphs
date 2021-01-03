package org.openobd2.core.logger.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.bl.DataLoggerService
import org.openobd2.core.logger.R

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        Model.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        val btnStop: Button = root.findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Stop data logging ")
            DataLoggerService.stopAction(this.requireContext())
        });

        val btnStart: Button = root.findViewById(R.id.btn_start);
        btnStart.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Start data logging")
            DataLoggerService.startAction(this.requireContext(), "OBDII")
        });
        return root
    }
}