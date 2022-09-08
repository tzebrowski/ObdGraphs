package org.obd.graphs.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.bluetoothAdapter
import org.obd.graphs.requestBluetoothPermissions
import java.util.*

private class Device(val name: String)

class AdaptersListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {
        setOnPreferenceClickListener {
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