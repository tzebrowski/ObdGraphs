package org.obd.graphs.aa.screen.nav

const val GIULIA_SCREEN_ID = 0
const val DRAG_RACING_SCREEN_ID = 1
const val ROUTINES_SCREEN_ID = 2

internal class ScreenNavigator {

    private var screenId = GIULIA_SCREEN_ID

    fun setNewScreen(newScreen: Int){
        screenId = newScreen
    }

    fun getCurrentScreenId(): Int = screenId

    fun isVirtualScreensEnabled(): Boolean = getCurrentScreenId() == GIULIA_SCREEN_ID
}