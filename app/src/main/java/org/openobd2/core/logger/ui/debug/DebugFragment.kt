package org.openobd2.core.logger.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R

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
        return root
    }
}