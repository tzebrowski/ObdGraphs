 /**
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
package org.obd.graphs.profile

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updatePreference
import java.io.File
import java.util.Properties

internal class BackupRestoreTest : TestSetup() {
    @Before
    override fun setup() {
        super.setup()

        mockkObject(Prefs)
        every { Prefs.edit() } returns editor
        every { Prefs.all } returns emptyMap()
        every { Prefs.getString(any(), any()) } answers { secondArg() as String }
        every { Prefs.getBoolean(any(), any()) } returns false // Default to false for installation check
        every { Prefs.registerOnSharedPreferenceChangeListener(any()) } just Runs

        // Mock Editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putStringSet(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.commit() } returns true
        every { editor.apply() } just Runs
    }

    @Test
    fun `restoreBackup clears preferences and reloads from file`() {
        val tempFile = File.createTempFile("test_backup", ".properties")
        val backupProfile = "profile_4"
        val backupProps =
            Properties().apply {
                setProperty("pref.restored.key.string", "\"restored_value\"") // Strings are quoted in backup
                setProperty("pref.restored.key.int", "999")
                setProperty("pref.restored.key.bool", "true")
                setProperty("pref.restored.key.array", "[22, 01, 02]")
                setProperty("pref.profile.id", backupProfile)
            }

        java.io.FileOutputStream(tempFile).use { backupProps.store(it, null) }
        mockPropertiesFiles(listOf("alfa_2_0_gme.properties", "default.properties"))
        val versionCode = 86

        profileService.init(versionCode, "profile_1", "20251218.1055")
        profileService.setupProfiles(forceOverrideRecommendation = true)

        // profiles setup profile_1
        verify { editor.updatePreference("profile_1.pref.adapter.power.switch_network_on_off", true) }
        verify { editor.updatePreference("profile_1.pref.dash.swipe.to.delete", true) }
        verify { editor.updatePreference("profile_1.pref.adapter.init.protocol", "AUTO") }
        verify { editor.updatePreference("profile_1.pref.pids.registry.list", setOf("mode01_2.json", "extra.json", "mode01.json")) }

        //
        // profiles setup profile_3
        verify { editor.updatePreference("profile_3.pref.adapter.power.switch_network_on_off", false) }
        verify { editor.updatePreference("profile_3.pref.gauge.fps", "4") }
        verify { editor.updatePreference("profile_3.pref.adapter.init.protocol", "CAN_29") }
        verify {
            editor.updatePreference(
                "profile_3.pref.pids.generic.high",
                setOf("22", "7002", "13", "15", "7003", "7006", "6", "7005", "7018", "7029", "7007"),
            )
        }
        verify { editor.updatePreference("profile_3.pref.giulia.pids.selected", setOf("7002", "7003", "6", "7005", "7016", "7018")) }
        verify {
            editor.updatePreference(
                "profile_3.pref.pids.registry.list",
                setOf(
                    "mode01_2.json",
                    "mode01.json",
                    "giulia_2.0_gme.json",
                    "rfhub_module.json",
                    "abs_module.json",
                    "dtcm_module.json",
                    "2.0_gme_ext.json",
                ),
            )
        }

        // profile load
        verify { editor.updatePreference("pref.adapter.power.switch_network_on_off", "true") }
        verify { editor.updatePreference("pref.dash.swipe.to.delete", "true") }
        verify { editor.updatePreference("pref.adapter.init.protocol", "AUTO") }

        profileService.restoreBackup(tempFile)

        verify {
            editor.clear()
            editor.putBoolean("pref.restored.key.bool", true)
            editor.putString("pref.restored.key.string", "restored_value")
            editor.putInt("pref.restored.key.int", 999)
            editor.putString("pref.profile.id", backupProfile)
            editor.putBoolean("prefs.installed.profiles.$versionCode", true)
            editor.putStringSet("pref.restored.key.array", setOf("22", "01", "02"))

            editor.apply()
        }

        verify { org.obd.graphs.sendBroadcastEvent("data.logger.profile.changed.event") }
    }
}
