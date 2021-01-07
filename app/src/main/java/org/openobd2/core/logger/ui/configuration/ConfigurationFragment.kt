package org.openobd2.core.logger.ui.configuration

import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceFragmentCompat
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R

class ConfigurationFragment : PreferenceFragmentCompat () {

    private lateinit var configurationViewModel: ConfigurationViewModel
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}