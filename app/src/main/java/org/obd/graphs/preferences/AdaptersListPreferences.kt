package org.obd.graphs.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.REQUEST_PERMISSIONS_BT
import org.obd.graphs.bluetoothAdapter
import org.obd.graphs.sendBroadcastEvent
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

        try {
            bluetoothAdapter().run {
                bondedDevices.forEach { currentDevice ->
                    entries.add(currentDevice.name)
                    entriesValues.add(currentDevice.name)
                }
            }
        }catch (e: SecurityException){
            sendBroadcastEvent(REQUEST_PERMISSIONS_BT)
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}