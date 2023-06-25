package org.obd.graphs.aa

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.R
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator


class CarService : CarAppService() {
    override fun onCreateSession(): Session  = CarSession()
    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(R.array.hosts_allowlist_sample)
                .build()
        }
    }
}
