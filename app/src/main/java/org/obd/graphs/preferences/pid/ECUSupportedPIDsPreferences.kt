package org.obd.graphs.preferences.pid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.bl.datalogger.WORKFLOW_RELOAD_EVENT
import org.obd.graphs.preferences.getECUSupportedPIDs
import org.obd.graphs.ui.common.COLOR_CARDINAL
import java.util.*

class ECUSupportedPIDsPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action === WORKFLOW_RELOAD_EVENT) {
                initialize()
            }
        }
    }

    init {
        initialize()
    }

    override fun onDetached() {
        super.onDetached()
        context?.unregisterReceiver(broadcastReceiver)
    }

    override fun onAttached() {
        super.onAttached()
        registerReceiver(context)
    }

    private fun registerReceiver(context: Context?) {
        context?.registerReceiver(
            broadcastReceiver,
            IntentFilter().apply {
                addAction(WORKFLOW_RELOAD_EVENT)
            }
        )
    }

    private fun initialize() {
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
            val text = notSupportedByApp(p)
            entries.add(text)
            entriesValues.add(text)
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }

    private fun notSupportedByApp(p: String): SpannableString  =
        SpannableString("PID: ${p.uppercase()} (not supported by application)").apply {

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
}

