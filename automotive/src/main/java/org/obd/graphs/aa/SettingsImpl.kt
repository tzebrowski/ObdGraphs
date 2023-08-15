package org.obd.graphs.aa

import android.graphics.Color
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.preferences.*
import org.obd.graphs.renderer.ColorTheme



private const val PREF_THEME_PROGRESS_BAR_COLOR= "pref.aa.theme.progressColor"
private const val PREF_THEME_DIVIDER_COLOR= "pref.aa.theme.dividerColor"
private const val PREF_THEME_CURR_VALUE_COLOR= "pref.aa.theme.currentValueColor"

private const val PREF_CURRENT_VIRTUAL_SCREEN = "pref.aa.pids.vs.current"
private const val PREF_SELECTED_PIDS = "pref.aa.pids.selected"
private const val PREF_MAX_PIDS_IN_COLUMN = "pref.aa.max_pids_in_column"
private const val PREF_SCREEN_FONT_SIZE = "pref.aa.screen_font_size"
private const val PREF_SURFACE_FRAME_RATE = "pref.aa.surface.fps"
private const val PREF_STATUS_FPS_VISIBLE = "pref.aa.status.fps.enabled"

private const val DEFAULT_ITEMS_IN_COLUMN = "6"
private const val DEFAULT_FONT_SIZE = "34"
private const val DEFAULT_FRAME_RATE = "5"

const val VIRTUAL_SCREEN_1 = "pref.aa.pids.profile_1"
const val VIRTUAL_SCREEN_2 = "pref.aa.pids.profile_2"
const val VIRTUAL_SCREEN_3 = "pref.aa.pids.profile_3"
const val VIRTUAL_SCREEN_4 = "pref.aa.pids.profile_4"

internal class SettingsImpl : ScreenSettings {
    override fun colorTheme(): ColorTheme {
        return ColorTheme(
            progressColor =  Prefs.getInt(PREF_THEME_PROGRESS_BAR_COLOR, Color.RED),
            dividerColor =  Prefs.getInt(PREF_THEME_DIVIDER_COLOR, Color.WHITE),
            currentValueColor =  Prefs.getInt(PREF_THEME_CURR_VALUE_COLOR, Color.WHITE)
        )
    }
    override fun applyVirtualScreen1() = applyVirtualScreen(VIRTUAL_SCREEN_1)
    override fun applyVirtualScreen2() = applyVirtualScreen(VIRTUAL_SCREEN_2)
    override fun applyVirtualScreen3() = applyVirtualScreen(VIRTUAL_SCREEN_3)
    override fun applyVirtualScreen4() = applyVirtualScreen(VIRTUAL_SCREEN_4)

    override fun getSelectedPIDs() =
        Prefs.getStringSet(PREF_SELECTED_PIDS).map { s -> s.toLong() }.toSet()

    override fun getMaxItemsInColumn(): Int {
        return when (getSelectedPIDs().size) {
            1 -> 1
            2 -> 1
            3 -> 1
            4 -> 1
            else -> Prefs.getS(PREF_MAX_PIDS_IN_COLUMN, DEFAULT_ITEMS_IN_COLUMN).toInt()
        }
    }

    override fun isHistoryEnabled(): Boolean {
        return Prefs.getBoolean("pref.aa.pids.history.enabled", true)
    }

    override fun isFpsCounterEnabled(): Boolean {
        return Prefs.getBoolean(PREF_STATUS_FPS_VISIBLE, false)
    }

    override fun getSurfaceFrameRate(): Int = Prefs.getS(PREF_SURFACE_FRAME_RATE, DEFAULT_FRAME_RATE).toInt()
    override fun getMaxFontSize(): Int =
        Prefs.getS(PREF_SCREEN_FONT_SIZE, DEFAULT_FONT_SIZE).toInt()

    override fun getCurrentVirtualScreen(): String = Prefs.getS(PREF_CURRENT_VIRTUAL_SCREEN, "pref.aa.pids.profile_1")

    override fun applyVirtualScreen(key: String) {
        Prefs.updateString(PREF_CURRENT_VIRTUAL_SCREEN, key)
        Prefs.updateStringSet(PREF_SELECTED_PIDS, Prefs.getStringSet(key).toList())
    }
}