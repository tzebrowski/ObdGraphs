package org.obd.graphs.ui.preferences.pid

import android.content.Context
import android.text.Html
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.ui.common.COLOR_CARDINAL
import java.util.*

class ECUSupportedPIDsPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()
        val pidList = DataLogger.instance.pidDefinitionRegistry().findAll()

        val groupBy =
            getECUSupportedPIDs().groupBy { p -> if (pidList.firstOrNull { it.pid == p.uppercase() } == null) "not supported" else "supported" }

        groupBy["supported"]?.forEach { p ->
            val pid = pidList.first { it.pid == p.uppercase() }
            val text = pid.displayString()
            entries.add(text)
            entriesValues.add(text)
        }

        groupBy["not supported"]?.forEach { p ->
            val text = SpannableString("PID: ${p.uppercase()} (not supported by application)").apply {

                val endIndexOf = indexOf(")") + 1
                val startIndexOf = indexOf("(")
                setSpan(
                    RelativeSizeSpan(0.5f), startIndexOf, endIndexOf,
                    0
                )

                setSpan(
                    ForegroundColorSpan(COLOR_CARDINAL),
                    startIndexOf,
                    endIndexOf,
                    0
                )
            }
            entries.add(text)
            entriesValues.add(text)
        }


        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
        summaryProvider = SupportedPIDsPreferencesSummaryProvider.instance
    }

    class SupportedPIDsPreferencesSummaryProvider private constructor() :
        SummaryProvider<ECUSupportedPIDsPreferences> {
        override fun provideSummary(preference: ECUSupportedPIDsPreferences): CharSequence {
            var summary = ""
            val pidList = DataLogger.instance.pidDefinitionRegistry().findAll()

            getECUSupportedPIDs().forEach { p ->
                val pid = pidList.firstOrNull { it.pid == p }
                summary += if (pid == null) {
                    "$p <br>"
                } else {
                    "${pid.pid} - ${pid.description}<br>"
                }
            }

            return Html.fromHtml(summary)
        }

        companion object {
            private var sSimpleSummaryProvider: SupportedPIDsPreferencesSummaryProvider? = null

            val instance: SupportedPIDsPreferencesSummaryProvider?
                get() {
                    if (sSimpleSummaryProvider == null) {
                        sSimpleSummaryProvider = SupportedPIDsPreferencesSummaryProvider()
                    }
                    return sSimpleSummaryProvider
                }
        }
    }
}