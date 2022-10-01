package org.obd.graphs.preferences

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.bluetoothAdapter
import org.obd.graphs.requestBluetoothPermissions
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.colorize
import java.util.*

private class Device(val name: String)

class AdaptersListPreferences(
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
            entries.add(it.name)
            entriesValues.add(it.name)
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
            entriesValues.add(it.name)
        }

        entryValues = entriesValues.toTypedArray()
        return super.getEntryValues()
    }

    override fun getEntries(): Array<CharSequence> {

        val entries: MutableList<CharSequence> =
            LinkedList()

        getDeviceList {
            entries.add(it.name)
        }

        setEntries(entries.toTypedArray())
        return super.getEntries()
    }

    private fun getDeviceList(handler: (device: Device) -> Unit) {

        try {
            bluetoothAdapter()?.run {
                bondedDevices
                    .sortedBy { currentDevice -> currentDevice.name }
                    .forEach { currentDevice ->
                        handler(Device(currentDevice.name))
                    }
            }
        } catch (e: SecurityException) {
            requestBluetoothPermissions()
        }
    }
}