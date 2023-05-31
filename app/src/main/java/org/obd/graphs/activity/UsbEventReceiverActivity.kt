package org.obd.graphs.activity

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Parcelable


class UsbEventReceiverActivity : Activity() {


    override fun onResume() {
        super.onResume()

        if (intent != null) {
            if (intent.action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                val usbDevice: Parcelable? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                // Create a new intent and put the usb device in as an extra
                val broadcastIntent = Intent(ACTION_USB_DEVICE_ATTACHED)
                broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice)

                // Broadcast this event so we can receive it
                sendBroadcast(broadcastIntent)
            }
        }

        // Close the activity
        finish()
    }

    companion object {
        const val ACTION_USB_DEVICE_ATTACHED = "com.example.ACTION_USB_DEVICE_ATTACHED"
    }
}