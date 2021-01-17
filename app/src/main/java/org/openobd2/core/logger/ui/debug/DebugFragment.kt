package org.openobd2.core.logger.ui.debug

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.ModelChangePublisher

class DebugFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_debug, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)

        ModelChangePublisher.debugData.observe(viewLifecycleOwner, Observer {
            textView.append(it.toString() + "\n")
        })

        return root
    }
}