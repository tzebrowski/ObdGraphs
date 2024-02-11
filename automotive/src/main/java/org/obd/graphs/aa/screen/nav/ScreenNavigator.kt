package org.obd.graphs.aa.screen.nav

import android.graphics.Color
import org.obd.graphs.aa.CarSettings

const val GIULIA_SCREEN_ID = 0
const val DRAG_RACING_SCREEN_ID = 1
const val ROUTINES_SCREEN_ID = 2



class ScreenNavigator(private val settings: CarSettings) {

    private var screenId = GIULIA_SCREEN_ID

    fun nextScreenId(): Int {
        if (screenId == getNumberOfScreensEnabled()) {
            screenId = GIULIA_SCREEN_ID
        } else {
            screenId++
        }
        return screenId
    }

    fun getCurrentScreenBtnColor(): Int = when (getCurrentScreenId()){
        GIULIA_SCREEN_ID -> Color.RED
        DRAG_RACING_SCREEN_ID -> Color.WHITE
        ROUTINES_SCREEN_ID -> Color.BLUE
        else -> Color.YELLOW
    }


    fun getCurrentScreenId(): Int = screenId

    fun isVirtualScreensEnabled(): Boolean = getCurrentScreenId() == GIULIA_SCREEN_ID

    private fun getNumberOfScreensEnabled() = if (settings.isRoutinesEnabled()) ROUTINES_SCREEN_ID else DRAG_RACING_SCREEN_ID
}