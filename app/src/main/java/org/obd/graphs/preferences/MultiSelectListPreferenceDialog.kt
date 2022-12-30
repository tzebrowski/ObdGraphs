package org.obd.graphs.preferences

import android.os.Bundle
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat

private const val DIALOG_FRAGMENT_TAG  = "androidx.preference.PreferenceFragment.DIALOG"
class MultiSelectListPreferenceDialog(key: String, private val onDialogCloseListener: (() -> Unit)):
    MultiSelectListPreferenceDialogFragmentCompat() {

    init {
        val bundle = Bundle(1)
        bundle.putString(ARG_KEY, key)
        arguments = bundle
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        onDialogCloseListener.invoke()
    }
}

fun showMultiSelectListDialog(preferenceKey: String, targetFragment: PreferenceFragmentCompat,
                              onDialogCloseListener: (() -> Unit)) {

    MultiSelectListPreferenceDialog(key = preferenceKey, onDialogCloseListener = onDialogCloseListener).also {
        it.setTargetFragment(targetFragment, 0)
        it.show(
            targetFragment.parentFragmentManager,
            DIALOG_FRAGMENT_TAG
        )
    }
}