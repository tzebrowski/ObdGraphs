package org.obd.graphs.activity

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Parcelable

const val USB_DEVICE_ATTACHED_EVENT = "org.obd.graphs.activity.ACTION_USB_DEVICE_ATTACHED"

class UsbEventReceiverActivity : Activity() {
    override fun onResume() {
        super.onResume()
        if (intent != null) {
            when (intent.action){
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val usbDevice: Parcelable? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    val broadcastIntent = Intent(USB_DEVICE_ATTACHED_EVENT)
                    broadcastIntent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice)
                    sendBroadcast(broadcastIntent)
                }
            }
        }
        finish()
    }
}