package org.obd.graphs.ui.preferences

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import java.util.*

@SuppressLint("MissingPermission")
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
        mBluetoothAdapter?.run {
            for (currentDevice in bondedDevices) {
                entries.add(currentDevice.name)
                entriesValues.add(currentDevice.name)
            }
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}