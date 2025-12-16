package org.obd.graphs.profile

import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.obd.graphs.diagnosticRequestIDMapper
import org.obd.graphs.modules
import org.obd.graphs.preferences.Prefs
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class SetupProfilesTest: TestSetup(){


    @Before
    override fun setup() {
        super.setup()

        mockkObject(Prefs)
        every { Prefs.edit() } returns editor
        every { editor.putString(any(),any())} returns editor
        every { Prefs.getString(any(), any()) } answers { secondArg() }
        every { Prefs.getBoolean(any(), any()) } returns false // Default to false for installation check

        mockkObject(diagnosticRequestIDMapper)
        every { diagnosticRequestIDMapper.updateSettings(any()) } just Runs
        every { diagnosticRequestIDMapper.getValuePreferenceName() } returns "pref.adapter.id"

        mockkObject(modules)
        every { modules.updateSettings(any()) } just Runs

        // Initialize the class
        profileService = ProfilePreferencesBackend()
        profileService.init(1, "profile_1",  SimpleDateFormat("yyyyMMdd.HHmm",
            Locale.getDefault()).format(Date()))
    }



    @Test
    fun `setupProfiles should load assets and update preferences when fresh install`() {
        // GIVEN
        val testProfileFile = "profile_1.properties"
        val profileContent = """
            pref.engine.type=petrol
            pref.adapter.logging.enabled=true
            pref.custom.list=[1, 2, 3]
            pref.adapter.baud=38400
        """.trimIndent()

        // Mock Assets to return our test file
        every { assets.list("") } returns arrayOf(testProfileFile)
        every { assets.open(testProfileFile) } returns ByteArrayInputStream(profileContent.toByteArray())

        // Mock Prefs to appear empty (fresh install)
        every { Prefs.all } returns emptyMap()
        // Mock getBoolean for installation check (returns false)
        every { Prefs.getBoolean(any(), false) } returns false

        // WHEN
        profileService.setupProfiles(forceOverrideRecommendation = false)

        // THEN
        // 1. Verify editor was cleared (logic: if keys empty, forceOverride becomes true)
        verify { editor.clear() }

        // 2. Verify String parsing
        verify { editor.putString("pref.engine.type", "petrol") }

        // 3. Verify Boolean parsing (StringExt.kt logic)
        verify { editor.putBoolean("pref.adapter.logging.enabled", true) }

        // 4. Verify Array/Set parsing
        verify {
            editor.putStringSet("pref.custom.list", match {
                it.contains("1") && it.contains("2") && it.contains("3")
            })
        }

        // 5. Verify Numeric parsing
        verify { editor.putInt("pref.adapter.baud", 38400) }

        // 6. Verify default profile was set
        verify { editor.putString("pref.profile.id", "profile_1") }

        // 7. Verify installation key was set to true
        verify { editor.putBoolean(match { it.startsWith("prefs.installed.profiles") }, true) }

        // 8. Verify apply was called
        verify(atLeast = 1) { editor.apply() }
    }


    @Test
    fun `setupProfiles should force reload if forceOverride is true`() {
        // GIVEN
        val testProfileFile = "profile_1.properties"
        val profileContent = "pref.some.key=value"
        val installationKey = "prefs.installed.profiles.1"

        // Even if installed...
        every { Prefs.all } returns mapOf(installationKey to true)
        every { Prefs.getBoolean(installationKey, false) } returns false

        // Mock Assets
        every { assets.list("") } returns arrayOf(testProfileFile)
        every { assets.open(testProfileFile) } returns ByteArrayInputStream(profileContent.toByteArray())

        // WHEN
        profileService.setupProfiles(forceOverrideRecommendation = true)

        // THEN
        // It SHOULD clear and load
        verify { editor.clear() }
        verify { assets.open(testProfileFile) }
        verify { editor.putString("pref.some.key", "value") }
    }
}