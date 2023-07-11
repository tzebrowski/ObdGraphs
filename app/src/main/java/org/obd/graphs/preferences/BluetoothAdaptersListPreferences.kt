package org.obd.graphs.preferences

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.network
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.colorize
import java.util.*

private class Device(val address: String, val label: Spanned)

class BluetoothAdaptersListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {
    init {
        setOnPreferenceChangeListener{ _,_ ->
            navigateToPreferencesScreen("pref.adapter.connection")
            true
        }

        val entriesValues: MutableList<CharSequence> =
            LinkedList()
        val entries: MutableList<CharSequence> =
            LinkedList()

        getDeviceList {
            entries.add(it.label)
            entriesValues.add(it.address)
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }

    override fun getSummary(): CharSequence {
        return super.getSummary().toString().colorize(COLOR_PHILIPPINE_GREEN, Typeface.BOLD, 1.0f)
    }

    override fun getEntryValues(): Array<CharSequence> {
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        getDeviceList {
            entriesValues.add(it.address)
        }

        entryValues = entriesValues.toTypedArray()
        return super.getEntryValues()
    }

    override fun getEntries(): Array<CharSequence> {

        val entries: MutableList<CharSequence> =
            LinkedList()

        getDeviceList {
            entries.add(it.label)
        }

        setEntries(entries.toTypedArray())
        return super.getEntries()
    }

    private fun getDeviceList(handler: (device: Device) -> Unit) {

        try {
            network.bluetoothAdapter()?.run {
                bondedDevices
                    .sortedBy { currentDevice -> currentDevice.name }
                    .forEach { currentDevice ->

                        handler(Device(address = currentDevice.address, label = format("${currentDevice.name} (${currentDevice.address})")))
                    }
            }
        } catch (e: SecurityException) {
            network.requestBluetoothPermissions()
        }
    }
    private fun format(text: String): Spanned {
        return SpannableString(text).apply {
            val endIndexOf = text.indexOf(")") + 1
            val startIndexOf = text.indexOf("(")
            setSpan(
                RelativeSizeSpan(0.5f), startIndexOf, endIndexOf,
                0
            )

            setSpan(
                ForegroundColorSpan(COLOR_PHILIPPINE_GREEN),
                startIndexOf,
                endIndexOf,
                0
            )
        }
    }
}