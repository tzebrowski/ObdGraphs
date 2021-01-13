package org.openobd2.core.logger.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.openobd2.core.logger.R

class ConfigurationFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}