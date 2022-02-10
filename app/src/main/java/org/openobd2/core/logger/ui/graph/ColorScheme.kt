package org.openobd2.core.logger.ui.graph

import com.github.mikephil.charting.utils.ColorTemplate

fun colorScheme(): IntIterator {

    val colorScheme = mutableListOf<Int>()
    colorScheme.add(android.R.color.holo_green_light)
    colorScheme.add(android.R.color.holo_red_light)
    colorScheme.add(android.R.color.holo_purple)
    colorScheme.add(android.R.color.holo_green_dark)
    colorScheme.add(android.R.color.holo_blue_bright)
    colorScheme.add(android.R.color.holo_orange_light)
    colorScheme.add(android.R.color.holo_red_dark)

    ColorTemplate.MATERIAL_COLORS.forEach {
        colorScheme.add(it)
    }

    return colorScheme.toIntArray().iterator()
}