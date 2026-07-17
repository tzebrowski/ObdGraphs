/*
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.wizard

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.obd.graphs.R
import org.obd.graphs.language.LanguageManager
import pub.devrel.easypermissions.EasyPermissions

private data class WizardStep(
    val titleResId: Int,
    val fragmentFactory: () -> Fragment
)

class SetupWizardActivity :
    AppCompatActivity(),
    EasyPermissions.PermissionCallbacks {
    private val steps =
        listOf(
            WizardStep(R.string.wizard_step_welcome_title) { WelcomeStepFragment() },
            WizardStep(R.string.wizard_step_language_title) { LanguageStepFragment() },
            WizardStep(R.string.wizard_step_permissions_title) { PermissionsStepFragment() },
            WizardStep(R.string.wizard_step_profile_title) { ProfileStepFragment() },
            WizardStep(R.string.wizard_step_adapter_title) { AdapterStepFragment() },
            WizardStep(R.string.wizard_step_test_connection_title) { TestConnectionStepFragment() },
            WizardStep(R.string.wizard_step_gdrive_title) { GoogleDriveStepFragment() },
            WizardStep(R.string.wizard_step_views_title) { ViewsStepFragment() },
            WizardStep(R.string.wizard_step_summary_title) { SummaryStepFragment() }
        )

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.getLocalizedContext(newBase))
    }

    private var currentStep = 0

    private lateinit var progress: LinearProgressIndicator
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: MaterialButton
    private lateinit var btnSkip: MaterialButton
    private lateinit var btnNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_setup_wizard)
        supportActionBar?.hide()

        progress = findViewById(R.id.wizardProgress)
        tvTitle = findViewById(R.id.tvWizardStepTitle)
        btnBack = findViewById(R.id.btnWizardBack)
        btnSkip = findViewById(R.id.btnWizardSkip)
        btnNext = findViewById(R.id.btnWizardNext)

        applyWindowInsets()

        btnBack.setOnClickListener { goToStep(currentStep - 1) }
        btnSkip.setOnClickListener { advance() }
        btnNext.setOnClickListener { advance() }

        goToStep(0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(
        requestCode: Int,
        perms: MutableList<String>
    ) = refreshPermissionsStep()

    override fun onPermissionsDenied(
        requestCode: Int,
        perms: MutableList<String>
    ) = refreshPermissionsStep()

    private fun refreshPermissionsStep() {
        (supportFragmentManager.findFragmentById(R.id.wizardStepContainer) as? PermissionsStepFragment)?.refreshStatus()
    }

    private fun applyWindowInsets() {
        val buttonBar = findViewById<View>(R.id.wizardButtonBar)
        val buttonBarBasePadding = buttonBar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.wizardRoot)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            progress.updatePadding(top = systemBars.top)
            buttonBar.updatePadding(bottom = buttonBarBasePadding + systemBars.bottom)
            insets
        }
    }

    private fun advance() {
        if (currentStep == steps.lastIndex) {
            SetupWizardManager.markCompleted(this)
            finish()
        } else {
            goToStep(currentStep + 1)
        }
    }

    private fun goToStep(step: Int) {
        currentStep = step
        val wizardStep = steps[step]

        tvTitle.text = getString(wizardStep.titleResId)
        progress.progress = (step + 1) * 100 / steps.size

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.wizardStepContainer, wizardStep.fragmentFactory())
            .commit()

        btnBack.isEnabled = step > 0

        val isLastStep = step == steps.lastIndex
        btnNext.setText(if (isLastStep) R.string.wizard_action_finish else R.string.wizard_action_next)
        btnSkip.visibility = if (isLastStep) View.GONE else View.VISIBLE
    }
}
