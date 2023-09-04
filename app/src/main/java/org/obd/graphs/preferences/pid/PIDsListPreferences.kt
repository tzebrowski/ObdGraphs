package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

open class PIDsListPreferences(
    context: Context?,
    private val attrs: AttributeSet?
) : DialogPreference(context, attrs) {

    val source = getAttribute("source")

    private fun getAttribute(attrName: String): String = if (attrs == null) {
        ""
    } else {
        val priority: String? = (0 until attrs.attributeCount)
            .filter { index -> attrs.getAttributeName(index) == attrName }
            .map { index -> attrs.getAttributeValue(index) }.firstOrNull()
        priority ?: ""
    }
}

