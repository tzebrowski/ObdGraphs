package org.openobd2.core.logger.ui.preferences

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
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
        entryValues = entriesValues.toTypedArray()
    }
}