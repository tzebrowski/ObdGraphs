package org.openobd2.core.logger.ui.preferences

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.pid.PidRegistry
import java.util.*

class AdaptersListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {
    init {
        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        for (currentDevice in mBluetoothAdapter.bondedDevices) {
            entries.add(currentDevice.name)
            entriesValues.add(currentDevice.name)
        }
        setEntries(entries.toTypedArray())
        setEntryValues(entriesValues.toTypedArray())
    }
}