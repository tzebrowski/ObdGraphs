package org.openobd2.core.logger

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent


class AdminReceiver : DeviceAdminReceiver() {



    companion object {
        const val ACTION_DISABLED = "device_admin_action_disabled"
        const val ACTION_ENABLED = "device_admin_action_enabled"
    }
}