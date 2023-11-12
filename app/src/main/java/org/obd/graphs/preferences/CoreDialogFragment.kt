package org.obd.graphs.preferences

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.fragment.app.DialogFragment
import org.obd.graphs.R

abstract class CoreDialogFragment : DialogFragment() {
    protected fun requestWindowFeatures() {
        dialog?.let {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
    }


    protected fun attachCloseButton(root: View, func: () -> Unit = {}) {
        root.findViewById<Button>(R.id.action_close_window).apply {
            setOnClickListener {
                func()
                dialog?.dismiss()
            }
        }
    }
}