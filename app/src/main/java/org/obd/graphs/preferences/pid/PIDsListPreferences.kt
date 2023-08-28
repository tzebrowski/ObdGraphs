package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

class PIDsListPreferences(
    context: Context?,
    private val attrs: AttributeSet?
) : DialogPreference(context, attrs) {

    var prio = getPriority()

    private fun getPriority(): String = if (attrs == null) {
        ""
    } else {
        val priority: String? = (0 until attrs.attributeCount)
            .filter { index -> attrs.getAttributeName(index) == "priority" }
            .map { index -> attrs.getAttributeValue(index) }.firstOrNull()
        priority ?: ""
    }
}

